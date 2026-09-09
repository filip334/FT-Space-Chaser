package io.github.filip334.spacechaser.server;

import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.network.message.GameStateSnapshot;
import io.github.filip334.spacechaser.network.message.LobbyStatusMessage;
import io.github.filip334.spacechaser.network.message.PauseStatusMessage;
import io.github.filip334.spacechaser.network.message.PlayerInputMessage;
import io.github.filip334.spacechaser.world.GameWorld;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {

    private final int port;

    private final GameWorld world = new GameWorld();
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<Integer, PlayerInputMessage> latestInputs = new ConcurrentHashMap<>();
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);
    private final Map<Integer, String> playerNames = new ConcurrentHashMap<>();
    private volatile boolean gameStarted;
    private static final String GAME_MODE = "2 Players vs AI";

    private volatile boolean paused = false;
    private volatile int pausingPlayerId = -1;

    private volatile boolean countingDown = false;
    private volatile float countdownRemaining = 0f;
    private static final float COUNTDOWN_SECONDS = 3f;

    private final SnapshotBuilder snapshotBuilder = new SnapshotBuilder();

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    // ---------------- KONSTRUKTOR ----------------

    public GameServer(int port) {
        this.port = port;
    }

    // ---------------- START ----------------

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        running = true;

        Thread acceptThread = new Thread(this::acceptLoop, "GameServer-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();

        Thread gameLoopThread = new Thread(this::gameLoop, "GameServer-Loop");
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();
    }

    // ---------------- GET / SET ----------------

    public int getPort() {
        if (serverSocket != null && serverSocket.isBound()) {
            return serverSocket.getLocalPort();
        }
        return port;
    }

    public int getConnectedPlayerCount() {
        return clients.size();
    }

    // ---------------- DISPOSE ----------------

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}

        for (ClientHandler client : clients.values()) {
            client.disconnect();
        }
        clients.clear();
    }

    // ---------------- CONNECTION HANDLING ----------------

    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                int playerId = nextPlayerId.getAndIncrement();
                ClientHandler handler = new ClientHandler(clientSocket, this, playerId);
                clients.put(playerId, handler);

                Thread clientThread = new Thread(handler, "ClientHandler-" + playerId);
                clientThread.setDaemon(true);
                clientThread.start();

            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }
    }

    // ---------------- GAME LOOP ----------------

    private void gameLoop() {
        long lastTime = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1_000_000_000f;
            lastTime = now;

            tick(deltaTime);

            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {}
        }
    }

    private void tick(float deltaTime) {
        if (countingDown) {
            countdownRemaining -= deltaTime;
            if (countdownRemaining <= 0f) {
                countingDown = false;
                gameStarted = true;
            }
            broadcastLobbyStatus();
        }

        if (!gameStarted) {
            return;
        }

        for (PlayerInputMessage input : latestInputs.values()) {
            Player p = world.getPlayerById(input.playerId);
            if (p != null) {
                p.applyInput(input.left, input.right, input.forward,
                        input.backward, input.boost, input.shoot);
            }
        }

        if (!paused) {
            world.update(deltaTime);
        }
        broadcastSnapshot();
    }

    // ---------------- SNAPSHOT BUILDING ----------------

    private void broadcastSnapshot() {
        GameStateSnapshot snapshot = snapshotBuilder.build(world);
        for (ClientHandler client : clients.values()) {
            client.send(snapshot);
        }
    }

    // ---------------- LOBBY / CONNECTION ----------------

    public void onPlayerConnected(int playerId) {
        world.spawnPlayer(playerId);
        broadcastLobbyStatus();
    }

    public void onPlayerNamed(int playerId, String playerName) {
        playerNames.put(playerId, sanitizeName(playerName));
        broadcastLobbyStatus();
    }

    public String getGuestName() {
        for (Map.Entry<Integer, String> entry : playerNames.entrySet()) {
            if (entry.getKey() != 1) return entry.getValue();
        }
        return "Waiting for player...";
    }

    public void requestStartCountdown() {
        if (clients.size() >= 2 && !countingDown && !gameStarted) {
            countingDown = true;
            countdownRemaining = COUNTDOWN_SECONDS;
            broadcastLobbyStatus();
        }
    }

    public boolean isCountingDown() {
        return countingDown;
    }

    public float getCountdownRemaining() {
        return countdownRemaining;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    private void broadcastLobbyStatus() {
        LobbyStatusMessage status = new LobbyStatusMessage();
        status.hostName = playerNames.getOrDefault(1, "Host");
        status.guestName = getGuestName();
        status.gameMode = GAME_MODE;
        status.gameStarted = gameStarted;
        status.countingDown = countingDown;
        status.countdownRemaining = countdownRemaining;
        for (ClientHandler client : clients.values()) client.send(status);
    }

    private String sanitizeName(String name) {
        if (name == null) return "Player";
        String result = name.trim();
        return result.isEmpty() ? "Player" : result.substring(0, Math.min(16, result.length()));
    }

    // ---------------- PLAYER INPUT & DISCONNECT ----------------

    public void onPlayerInput(PlayerInputMessage input) {
        latestInputs.put(input.playerId, input);
    }

    public void onPlayerDisconnected(int playerId) {
        clients.remove(playerId);
        latestInputs.remove(playerId);
        playerNames.remove(playerId);
        world.removePlayer(playerId);

        if (countingDown && clients.size() < 2) {
            countingDown = false;
            countdownRemaining = 0f;
        }

        broadcastLobbyStatus();

        if (paused && pausingPlayerId == playerId) {
            paused = false;
            pausingPlayerId = -1;
            broadcastPauseStatus();
        }
    }

    // ---------------- PAUSE HANDLING ----------------

    public void onPlayerPauseRequest(int playerId, boolean wantsPause) {
        if (wantsPause) {
            if (paused) return;
            paused = true;
            pausingPlayerId = playerId;
        } else {
            if (!paused || pausingPlayerId != playerId) return;
            paused = false;
            pausingPlayerId = -1;
        }
        broadcastPauseStatus();
    }

    private void broadcastPauseStatus() {
        PauseStatusMessage status = new PauseStatusMessage();
        status.paused = paused;
        status.pausedByPlayerId = pausingPlayerId;
        for (ClientHandler client : clients.values()) client.send(status);
    }
}
