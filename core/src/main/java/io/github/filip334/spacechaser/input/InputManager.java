package io.github.filip334.spacechaser.input;

import com.badlogic.gdx.Gdx;
import io.github.filip334.spacechaser.world.GameSettings;

public class InputManager{
    private final GameSettings settings;
    private final boolean readsKeyboard;

    private boolean forward, backward, left, right, shoot, boost;
    
    
    
    
    public InputManager(GameSettings settings) {
        this(settings, true);
    }

    /**
     * Server-side igraci moraju imati false: server prima komande preko mreze
     * i nikad ne sme da cita tastaturu host aplikacije.
     */
    public InputManager(GameSettings settings, boolean readsKeyboard) {
        this.settings = settings;
        this.readsKeyboard = readsKeyboard;
    }
    
    public boolean moveForward() {
        return (readsKeyboard && Gdx.input.isKeyPressed(settings.getMoveUp())) || forward;
    }

    public boolean moveBackward() {
        return (readsKeyboard && Gdx.input.isKeyPressed(settings.getMoveDown())) || backward;
    }

    public boolean moveLeft() {
        return (readsKeyboard && Gdx.input.isKeyPressed(settings.getMoveLeft())) || left;
    }

    public boolean moveRight() {
        return (readsKeyboard && Gdx.input.isKeyPressed(settings.getMoveRight())) || right;
    }

    public boolean shoot() {
        return (readsKeyboard && Gdx.input.isKeyPressed(settings.getShoot())) || shoot;
    }
    
    public boolean boosting(){
        return (readsKeyboard && Gdx.input.isKeyPressed(settings.getIsBoosting())) || boost;
    }

    public void setState(boolean left, boolean right, boolean forward, boolean backward,
                         boolean boost, boolean shoot) {
        this.left = left;
        this.right = right;
        this.forward = forward;
        this.backward = backward;
        this.boost = boost;
        this.shoot = shoot;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public void setBoost(boolean boost) {
        this.boost = boost;
    }
}
