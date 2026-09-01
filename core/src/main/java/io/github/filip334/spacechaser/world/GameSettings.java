package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.Input;

public class GameSettings {
    private int moveUp = Input.Keys.W;
    private int moveDown = Input.Keys.S;
    private int moveLeft = Input.Keys.A;
    private int moveRight = Input.Keys.D;
    private int shoot = Input.Keys.SPACE;
    private int boost = Input.Keys.SHIFT_LEFT;

    public GameSettings() {
    }

    public int getMoveUp() {
        return moveUp;
    }

    public int getMoveDown() {
        return moveDown;
    }

    public int getMoveLeft() {
        return moveLeft;
    }

    public int getMoveRight() {
        return moveRight;
    }

    public int getShoot() {
        return shoot;
    }
    
    public int getIsBoosting(){
        return boost;
    }
}
