package io.github.filip334.spacechaser.input;

public class PlayerInput {
    
    //
    public boolean left;
    public boolean right;
    public boolean forward;
    public boolean backward;
    public boolean boost;
    public boolean shoot;

    // ---------------- RESET ----------------
    public void reset() {
        left = right = forward = backward = boost = shoot = false;
    }
}
