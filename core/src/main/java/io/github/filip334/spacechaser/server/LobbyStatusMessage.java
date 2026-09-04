package io.github.filip334.spacechaser.server;

public class LobbyStatusMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;
    public String hostName;
    public String guestName;
    public String gameMode;
    public boolean gameStarted;
}
