package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.Input;

public class GameSettings {
    private static final int DEFAULT_MOVE_UP = Input.Keys.W;
    private static final int DEFAULT_MOVE_DOWN = Input.Keys.S;
    private static final int DEFAULT_MOVE_LEFT = Input.Keys.A;
    private static final int DEFAULT_MOVE_RIGHT = Input.Keys.D;
    private static final int DEFAULT_SHOOT = Input.Keys.SPACE;
    private static final int DEFAULT_BOOST = Input.Keys.SHIFT_LEFT;

    private String playerName = "Player";
    private int moveUp = DEFAULT_MOVE_UP;
    private int moveDown = DEFAULT_MOVE_DOWN;
    private int moveLeft = DEFAULT_MOVE_LEFT;
    private int moveRight = DEFAULT_MOVE_RIGHT;
    private int shoot = DEFAULT_SHOOT;
    private int boost = DEFAULT_BOOST;

    public GameSettings() {
    }

    /** Vraca samo kontrole na podrazumevane (W/S/A/D, Space, Left Shift) - ime igraca ostaje nepromenjeno. */
    public void resetControlsToDefault() {
        moveUp = DEFAULT_MOVE_UP;
        moveDown = DEFAULT_MOVE_DOWN;
        moveLeft = DEFAULT_MOVE_LEFT;
        moveRight = DEFAULT_MOVE_RIGHT;
        shoot = DEFAULT_SHOOT;
        boost = DEFAULT_BOOST;
    }

    public int getMoveUp() {
        return moveUp;
    }

    public void setMoveUp(int moveUp) {
        this.moveUp = moveUp;
    }

    public int getMoveDown() {
        return moveDown;
    }

    public void setMoveDown(int moveDown) {
        this.moveDown = moveDown;
    }

    public int getMoveLeft() {
        return moveLeft;
    }

    public void setMoveLeft(int moveLeft) {
        this.moveLeft = moveLeft;
    }

    public int getMoveRight() {
        return moveRight;
    }

    public void setMoveRight(int moveRight) {
        this.moveRight = moveRight;
    }

    public int getShoot() {
        return shoot;
    }

    public void setShoot(int shoot) {
        this.shoot = shoot;
    }
    
    public int getIsBoosting(){
        return boost;
    }

    public void setBoost(int boost) {
        this.boost = boost;
    }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) {
        this.playerName = playerName == null || playerName.trim().isEmpty() ? "Player" : playerName.trim();
    }
}
