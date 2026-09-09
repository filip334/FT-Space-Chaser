package io.github.filip334.spacechaser.server;

import io.github.filip334.spacechaser.network.message.JoinLobbyMessage;
import io.github.filip334.spacechaser.network.message.NetworkMessage;
import io.github.filip334.spacechaser.network.message.PauseMessage;
import io.github.filip334.spacechaser.network.message.PlayerInputMessage;
import io.github.filip334.spacechaser.network.message.WelcomeMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameServer server;
    private final int playerId;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean running = false;

    // ---------------- KONSTRUKTOR ----------------

    public ClientHandler(Socket socket, GameServer server, int playerId) {
        this.socket = socket;
        this.server = server;
        this.playerId = playerId;
    }

    // ---------------- RUN ----------------

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            running = true;

            WelcomeMessage welcome = new WelcomeMessage();
            welcome.assignedPlayerId = playerId;
            send(welcome);

            server.onPlayerConnected(playerId);

            listenLoop();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    // ---------------- LISTEN LOOP ----------------

    private void listenLoop() {
        try {
            while (running) {
                Object received = in.readObject();

                if (received instanceof PlayerInputMessage) {
                    PlayerInputMessage input = (PlayerInputMessage) received;
                    input.playerId = playerId;
                    server.onPlayerInput(input);
                } else if (received instanceof JoinLobbyMessage) {
                    server.onPlayerNamed(playerId, ((JoinLobbyMessage) received).playerName);
                } else if (received instanceof PauseMessage) {
                    server.onPlayerPauseRequest(playerId, ((PauseMessage) received).pause);
                }
            }
        } catch (EOFException | SocketException e) {
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // ---------------- SEND ----------------

    public synchronized void send(NetworkMessage message) {
        if (!running) return;
        try {
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
        if (!running) return;
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {}
        server.onPlayerDisconnected(playerId);
    }

    // ---------------- GET / SET ----------------

    public int getPlayerId() {
        return playerId;
    }
}
