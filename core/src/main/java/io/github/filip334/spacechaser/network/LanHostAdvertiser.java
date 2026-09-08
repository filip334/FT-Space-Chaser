package io.github.filip334.spacechaser.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

/**
 * Server-side UDP "oglasivac" - slusa broadcast discovery upite i
 * odgovara imenom hosta i TCP portom na kome GameServer prima konekcije.
 * Zivi paralelno sa GameServer-om dok host ceka igrace.
 */
public class LanHostAdvertiser {

    public static final int DISCOVERY_PORT = 54555;
    private static final String DISCOVER_REQUEST = "SPACECHASER_DISCOVER";
    private static final String RESPONSE_PREFIX = "SPACECHASER_HOST";

    private final int gamePort;
    private final String hostName;
    private final String gameMode;

    private DatagramSocket socket;
    private volatile boolean running = false;
    private Thread listenThread;

    public LanHostAdvertiser(int gamePort, String hostName, String gameMode) {
        this.gamePort = gamePort;
        this.hostName = hostName;
        this.gameMode = gameMode;
    }

    public void start() {
        try {
            socket = new DatagramSocket(DISCOVERY_PORT);
            socket.setBroadcast(true);
        } catch (IOException e) {
            System.out.println("LanHostAdvertiser: ne moze da otvori UDP port "
                    + DISCOVERY_PORT + " (" + e.getMessage() + ")");
            return;
        }

        running = true;
        listenThread = new Thread(this::listenLoop, "LanHostAdvertiser");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private void listenLoop() {
        byte[] buffer = new byte[512];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if (!DISCOVER_REQUEST.equals(message)) continue;

                String response = RESPONSE_PREFIX + "|" + hostName + "|" + gameMode + "|" + gamePort;
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes, responseBytes.length,
                        packet.getAddress(), packet.getPort()
                );
                socket.send(responsePacket);

            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
    }
}
