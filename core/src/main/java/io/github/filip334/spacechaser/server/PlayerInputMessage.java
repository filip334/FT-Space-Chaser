package io.github.filip334.spacechaser.server;

public class PlayerInputMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;

    public int playerId;
    public boolean left;
    public boolean right;
    public boolean forward;
    public boolean backward;
    public boolean boost;
    public boolean shoot;
}