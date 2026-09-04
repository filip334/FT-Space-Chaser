package io.github.filip334.spacechaser.server;

import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Wall;
import io.github.filip334.spacechaser.entity.Coin;
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

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public GameServer(int port) {
        this.port = port;
    }

    /**
     * Bez-arg verzija za pokretanje kroz Thread(server::start, ...) iz MultiplayerScreen.
     * Ne baca checked IOException napolje jer se pokrece u posebnoj niti.
     */
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

    /**
     * Stvarni port na kome server slusa - kad je konstruktor pozvan sa 0,
     * OS bira slobodan port, pa se pravi broj saznaje tek posle start().
     */
    public int getPort() {
        if (serverSocket != null && serverSocket.isBound()) {
            return serverSocket.getLocalPort();
        }
        return port;
    }

    public int getConnectedPlayerCount() {
        return clients.size();
    }

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

    private void gameLoop() {
        long lastTime = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1_000_000_000f;
            lastTime = now;

            tick(deltaTime);

            try {
                Thread.sleep(16); // ~60 tick/s
            } catch (InterruptedException ignored) {}
        }
    }

    private void tick(float deltaTime) {
        // Dok je lobby otvoren server samo odrzava veze; simulacija pocinje
        // tek kada host eksplicitno pritisne Start Game.
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

        world.update(deltaTime);
        broadcastSnapshot();
    }

    private void broadcastSnapshot() {
        GameStateSnapshot snapshot = buildSnapshot();
        for (ClientHandler client : clients.values()) {
            client.send(snapshot);
        }
    }

    private GameStateSnapshot buildSnapshot() {
        GameStateSnapshot s = new GameStateSnapshot();

        for (Map.Entry<Integer, Player> entry : world.getPlayersMap().entrySet()) {
            s.players.add(toPlayerState(entry.getKey(), entry.getValue()));
        }

        // TODO: zameniti sekvencijalne ID-jeve stabilnim ID-jem sa same Enemy/Bullet klase
        // kad budemo dodavali punu sinhronizaciju neprijatelja i metaka.
        int enemyId = 100_000;
        for (Enemy e : world.getEnemies()) {
            s.enemies.add(toSimpleState(enemyId++, e.getX(), e.getY(), e.getRotation()));
        }

        int bulletId = 200_000;
        for (Bullet b : world.getBullets()) {
            s.bullets.add(toSimpleState(bulletId++, b.getX(), b.getY(), b.getRotation()));
        }

        int wallId = 300_000;
        for (Wall w : world.getWalls()) {
            EntityState state = new EntityState();
            state.id = wallId++;
            state.x = w.getX();
            state.y = w.getY();
            state.width = w.getWidth();
            state.height = w.getHeight();
            s.walls.add(state);
        }

        for (Coin coin : world.getCoins()) {
            s.coins.add(toSimpleState(coin.getId(), coin.getX(), coin.getY(), 0f));
        }

        s.score = world.getScore();
        s.gameTime = world.getGameTime();
        s.matchOver = world.isMatchOver();

        return s;
    }

    private EntityState toPlayerState(int id, Player p) {
        EntityState s = new EntityState();
        s.id = id;
        s.x = p.getX();
        s.y = p.getY();
        s.rotation = p.getRotation();
        s.health = p.getHealth();
        s.maxHealth = p.getMaxHealth();
        s.fuel = p.getFuel();
        s.maxFuel = p.getMaxFuel();
        s.thrusting = p.isThrusting();
        s.boosting = p.isBoosting();
        return s;
    }

    private EntityState toSimpleState(int id, float x, float y, float rotation) {
        EntityState s = new EntityState();
        s.id = id;
        s.x = x;
        s.y = y;
        s.rotation = rotation;
        return s;
    }

    // pozivi iz ClientHandler-a

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

    public void startGame() {
        if (clients.size() >= 2) {
            gameStarted = true;
            broadcastLobbyStatus();
        }
    }

    private void broadcastLobbyStatus() {
        LobbyStatusMessage status = new LobbyStatusMessage();
        status.hostName = playerNames.getOrDefault(1, "Host");
        status.guestName = getGuestName();
        status.gameMode = GAME_MODE;
        status.gameStarted = gameStarted;
        for (ClientHandler client : clients.values()) client.send(status);
    }

    private String sanitizeName(String name) {
        if (name == null) return "Player";
        String result = name.trim();
        return result.isEmpty() ? "Player" : result.substring(0, Math.min(16, result.length()));
    }

    public void onPlayerInput(PlayerInputMessage input) {
        latestInputs.put(input.playerId, input);
    }

    public void onPlayerDisconnected(int playerId) {
        clients.remove(playerId);
        latestInputs.remove(playerId);
        playerNames.remove(playerId);
        world.removePlayer(playerId);
        broadcastLobbyStatus();
    }
}
