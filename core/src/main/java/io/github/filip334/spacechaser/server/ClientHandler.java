package io.github.filip334.spacechaser.server;

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

    public ClientHandler(Socket socket, GameServer server, int playerId) {
        this.socket = socket;
        this.server = server;
        this.playerId = playerId;
    }

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

    private void listenLoop() {
        try {
            while (running) {
                Object received = in.readObject();

                if (received instanceof PlayerInputMessage) {
                    PlayerInputMessage input = (PlayerInputMessage) received;
                    input.playerId = playerId; // ne veruj klijentu, sam upisi ID
                    server.onPlayerInput(input);
                }
            }
        } catch (EOFException | SocketException e) {
            // klijent se diskonektovao - normalno
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

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

    public void disconnect() {
        if (!running) return;
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {}
        server.onPlayerDisconnected(playerId);
    }

    public int getPlayerId() {
        return playerId;
    }
}