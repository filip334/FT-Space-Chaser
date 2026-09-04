package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.Input;

public class GameSettings {
    private String playerName = "Player";
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
