package io.github.filip334.spacechaser.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side LAN discovery: salje UDP broadcast upit i skuplja odgovore
 * svih pokrenutih LanHostAdvertiser instanci na mrezi u odredjenom prozoru vremena.
 * Poziva se sa pozadinske niti - scan() blokira do isteka timeout-a.
 */
public class LanGameDiscovery {

    private static final String DISCOVER_REQUEST = "SPACECHASER_DISCOVER";
    private static final String RESPONSE_PREFIX = "SPACECHASER_HOST";

    public static class DiscoveredHost {
        public final String address;
        public final int port;
        public final String hostName;
        public final String gameMode;

        public DiscoveredHost(String address, int port, String hostName, String gameMode) {
            this.address = address;
            this.port = port;
            this.hostName = hostName;
            this.gameMode = gameMode;
        }
    }

    public List<DiscoveredHost> scan(int timeoutMillis) {
        List<DiscoveredHost> results = new ArrayList<>();

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(200);

            byte[] requestBytes = DISCOVER_REQUEST.getBytes(StandardCharsets.UTF_8);
            DatagramPacket requestPacket = new DatagramPacket(
                    requestBytes, requestBytes.length,
                    InetAddress.getByName("255.255.255.255"),
                    LanHostAdvertiser.DISCOVERY_PORT
            );
            socket.send(requestPacket);

            long deadline = System.currentTimeMillis() + timeoutMillis;
            byte[] buffer = new byte[512];

            while (System.currentTimeMillis() < deadline) {
                try {
                    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(responsePacket);

                    String message = new String(responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8);
                    String[] parts = message.split("\\|");
                    if (parts.length != 4 || !RESPONSE_PREFIX.equals(parts[0])) continue;

                    String hostName = parts[1];
                    String gameMode = parts[2];
                    int port = Integer.parseInt(parts[3]);
                    String address = responsePacket.getAddress().getHostAddress();

                    boolean alreadyKnown = false;
                    for (DiscoveredHost h : results) {
                        if (h.address.equals(address) && h.port == port) {
                            alreadyKnown = true;
                            break;
                        }
                    }
                    if (!alreadyKnown) {
                        results.add(new DiscoveredHost(address, port, hostName, gameMode));
                    }

                } catch (SocketTimeoutException ignored) {
                    // normalno - samo nastavi da ceka do deadline-a
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return results;
    }
}
