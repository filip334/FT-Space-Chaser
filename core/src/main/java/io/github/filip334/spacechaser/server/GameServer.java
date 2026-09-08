package io.github.filip334.spacechaser.server;

import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Wall;
import io.github.filip334.spacechaser.entity.Coin;
import io.github.filip334.spacechaser.network.message.EntityState;
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

    // Zidovi/novcici se ne racunaju svaki tick preko celog snapshot-a - vidi
    // buildSnapshot(). Ovi fildovi se citaju/pisu samo iz GameServer-Loop niti.
    //
    // VAZNO: prvi tick posle starta NIJE dovoljan trenutak da se puna lista
    // posalje samo jednom - host/gost tek na SVOM sledecem render frejmu
    // prebacuju ekran i tek tada MultiplayerClient dobija svoj
    // MultiplayerGameWorld (setWorld()). Do tada listenLoop() ima world==null
    // i tiho baca sve sto stigne. Zato puna lista ide kroz kratak "startup"
    // prozor (ne samo 1 tick) da sigurno stigne posle te tranzicije.
    private boolean wallsBroadcast = false;
    private int lastCoinCount = -1;
    private int ticksSinceStart = 0;
    private static final int STARTUP_FULL_SYNC_TICKS = 90; // ~1.5s @ 60Hz

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
        if (countingDown) {
            countdownRemaining -= deltaTime;
            if (countdownRemaining <= 0f) {
                countingDown = false;
                gameStarted = true;
            }
            broadcastLobbyStatus();
        }

        // Dok je lobby otvoren server samo odrzava veze; simulacija pocinje
        // tek kada host eksplicitno pritisne Start Game.
        if (!gameStarted) {
            return;
        }

        ticksSinceStart++;

        for (PlayerInputMessage input : latestInputs.values()) {
            Player p = world.getPlayerById(input.playerId);
            if (p != null) {
                p.applyInput(input.left, input.right, input.forward,
                        input.backward, input.boost, input.shoot);
            }
        }

        // Dok je igra pauzirana, simulacija stoji na mestu - i dalje saljemo
        // snapshot da klijenti ostanu sinhronizovani (samo se nista ne menja).
        if (!paused) {
            world.update(deltaTime);
        }
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

        boolean startupWindow = ticksSinceStart <= STARTUP_FULL_SYNC_TICKS;

        // Zidovi su staticni za ceo mec (nikad se ne pomeraju/dodaju) - saljemo
        // ih ponovljeno samo tokom kratkog "startup" prozora (ne 60x/sec ceo
        // mec), da sigurno stignu i ako klijent zakasni da zakaci svoj svet.
        if (startupWindow || !wallsBroadcast) {
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
            s.wallsIncluded = true;
            wallsBroadcast = true;
        }

        // Novcici se samo skupljaju (nikad ne dodaju tokom meca) - van startup
        // prozora saljemo punu listu samo kad se broj promeni od prethodnog tick-a.
        int coinCount = world.getCoins().size;
        if (startupWindow || coinCount != lastCoinCount) {
            for (Coin coin : world.getCoins()) {
                s.coins.add(toSimpleState(coin.getId(), coin.getX(), coin.getY(), 0f));
            }
            s.coinsChanged = true;
            lastCoinCount = coinCount;
        }

        s.score = world.getScore();
        s.gameTime = world.getGameTime();
        s.matchOver = world.isMatchOver();
        s.wave = world.getWaveNumber();
        s.waveCountdown = world.isWaveCountingDown() ? world.getWaveCountdownSeconds() : 0f;

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
        s.score = p.getScore();
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

    /** Host pokrece odbrojavanje - i sam host i gost ga vide, igra pocinje kad istekne. */
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

    public void onPlayerInput(PlayerInputMessage input) {
        latestInputs.put(input.playerId, input);
    }

    public void onPlayerDisconnected(int playerId) {
        clients.remove(playerId);
        latestInputs.remove(playerId);
        playerNames.remove(playerId);
        world.removePlayer(playerId);

        // Ako neko izadje dok se odbrojava, ne sme da pocne partija sa 1 igracem.
        if (countingDown && clients.size() < 2) {
            countingDown = false;
            countdownRemaining = 0f;
        }

        broadcastLobbyStatus();

        // Ako je diskonektovani igrac bio taj koji je pauzirao, ne sme da
        // ostane trajno zaglavljeno pauzirano - niko drugi ne bi mogao da nastavi.
        if (paused && pausingPlayerId == playerId) {
            paused = false;
            pausingPlayerId = -1;
            broadcastPauseStatus();
        }
    }

    /**
     * Samo igrac koji je pauzirao moze da nastavi igru - dok god je "pause"
     * true od nekog drugog igraca, zahtevi za pauzu se ignorisu (vec je
     * pauzirano), a zahtevi za nastavak od nekog drugog se ignorisu (nije
     * njegova pauza).
     */
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
