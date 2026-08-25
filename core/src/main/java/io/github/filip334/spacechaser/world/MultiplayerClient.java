package io.github.filip334.spacechaser.world;

import io.github.filip334.spacechaser.server.GameStateSnapshot;
import io.github.filip334.spacechaser.server.PlayerInputMessage;
import io.github.filip334.spacechaser.server.WelcomeMessage;

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

    private Consumer<Integer> onConnected;
    private Runnable onDisconnected;

    public MultiplayerClient() {
    }

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
                    if (onConnected != null) onConnected.accept(localPlayerId);
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

    public void sendInput(PlayerInputMessage input) {
        if (!running) return;
        try {
            input.playerId = localPlayerId;
            out.writeObject(input);
            out.flush();
            out.reset();
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public int getLocalPlayerId() {
        return localPlayerId;
    }

    public boolean isConnected() {
        return running;
    }

    public void setOnConnected(Consumer<Integer> callback) { this.onConnected = callback; }
    public void setOnDisconnected(Runnable callback) { this.onDisconnected = callback; }
}