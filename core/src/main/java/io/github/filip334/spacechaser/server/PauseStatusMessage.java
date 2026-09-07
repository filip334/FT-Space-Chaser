package io.github.filip334.spacechaser.server;

/** Server -> svi klijenti: trenutno stanje pauze i koji igrac je pauzirao. */
public class PauseStatusMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;

    public boolean paused;
    public int pausedByPlayerId = -1;
}
