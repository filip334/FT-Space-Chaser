package io.github.filip334.spacechaser.network;

import io.github.filip334.spacechaser.network.message.GameStateSnapshot;
import io.github.filip334.spacechaser.network.message.PauseMessage;
import io.github.filip334.spacechaser.network.message.PauseStatusMessage;
import io.github.filip334.spacechaser.network.message.PlayerInputMessage;
import io.github.filip334.spacechaser.network.message.WelcomeMessage;
import io.github.filip334.spacechaser.network.message.JoinLobbyMessage;
import io.github.filip334.spacechaser.network.message.LobbyStatusMessage;
import io.github.filip334.spacechaser.network.message.NetworkMessage;
import io.github.filip334.spacechaser.world.MultiplayerGameWorld;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;

public class MultiplayerClient {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listenerThread;
    private volatile boolean running = false;

    private MultiplayerGameWorld world;
    private volatile int localPlayerId = -1;

    private Runnable onDisconnected;
    private Consumer<LobbyStatusMessage> onLobbyStatus;
    private volatile LobbyStatusMessage latestLobbyStatus;
    private Consumer<PauseStatusMessage> onPauseStatus;
    private volatile PauseStatusMessage latestPauseStatus;
    private final String playerName;

    // ---------------- KONSTRUKTOR ----------------

    public MultiplayerClient(String playerName) {
        this.playerName = playerName;
    }

    // ---------------- WORLD ----------------

    /**
     * Postavlja MultiplayerGameWorld u koji ce se upisivati primljeni snapshotovi.
     * Ako je WelcomeMessage vec stigao pre nego sto je world postavljen,
     * odmah mu prosledi vec poznati localPlayerId.
     */
    public void setWorld(MultiplayerGameWorld world) {
        this.world = world;
        if (localPlayerId != -1) {
            world.setLocalPlayerId(localPlayerId);
        }
    }

    // ---------------- CONNECT ----------------

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            // output stream MORA da se napravi pre input stream-a na oba kraja
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            running = true;
            listenerThread = new Thread(this::listenLoop, "MultiplayerClient-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---------------- LISTEN LOOP ----------------

    private void listenLoop() {
        try {
            while (running) {
                Object received = in.readObject();

                if (received instanceof GameStateSnapshot) {
                    GameStateSnapshot snapshot = (GameStateSnapshot) received;
                    if (world != null) {
                        world.applySnapshot(snapshot);
                    }

                } else if (received instanceof WelcomeMessage) {
                    WelcomeMessage welcome = (WelcomeMessage) received;
                    localPlayerId = welcome.assignedPlayerId;
                    if (world != null) {
                        world.setLocalPlayerId(localPlayerId);
                    }
                    JoinLobbyMessage join = new JoinLobbyMessage();
                    join.playerName = playerName;
                    send(join);
                } else if (received instanceof LobbyStatusMessage) {
                    latestLobbyStatus = (LobbyStatusMessage) received;
                    if (onLobbyStatus != null) onLobbyStatus.accept(latestLobbyStatus);
                } else if (received instanceof PauseStatusMessage) {
                    latestPauseStatus = (PauseStatusMessage) received;
                    if (onPauseStatus != null) onPauseStatus.accept(latestPauseStatus);
                }
            }
        } catch (EOFException | SocketException e) {
            // konekcija zatvorena - normalno pri disconnect-u
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            running = false;
            if (onDisconnected != null) onDisconnected.run();
        }
    }

    // ---------------- SEND ----------------

    public void sendInput(PlayerInputMessage input) {
        send(input);
    }

    public void sendPause(boolean pause) {
        PauseMessage message = new PauseMessage();
        message.pause = pause;
        send(message);
    }

    private void send(NetworkMessage message) {
        if (!running) return;
        try {
            if (message instanceof PlayerInputMessage) ((PlayerInputMessage) message).playerId = localPlayerId;
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    // ---------------- DISPOSE ----------------

    public void disconnect() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    // ---------------- GET / SET ----------------

    public int getLocalPlayerId() {
        return localPlayerId;
    }

    public void setOnDisconnected(Runnable callback) { this.onDisconnected = callback; }
    public void setOnLobbyStatus(Consumer<LobbyStatusMessage> callback) {
        this.onLobbyStatus = callback;
        if (latestLobbyStatus != null) callback.accept(latestLobbyStatus);
    }
    public void setOnPauseStatus(Consumer<PauseStatusMessage> callback) {
        this.onPauseStatus = callback;
        if (latestPauseStatus != null) callback.accept(latestPauseStatus);
    }
}
