package io.github.filip334.spacechaser.network.message;

public class PauseStatusMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;

    public boolean paused;
    public int pausedByPlayerId = -1;
}
