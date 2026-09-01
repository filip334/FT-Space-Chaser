package io.github.filip334.spacechaser.input;

import com.badlogic.gdx.Gdx;
import io.github.filip334.spacechaser.world.GameSettings;

public class InputManager{
    private final GameSettings settings;

    public boolean forward,backward,left,right,shoot,boost;
    
    
    
    
    public InputManager(GameSettings settings) {
        this.settings = settings;
    }
    
    public boolean moveForward() {
        return Gdx.input.isKeyPressed(settings.getMoveUp()) || forward;
    }

    public boolean moveBackward() {
        return Gdx.input.isKeyPressed(settings.getMoveDown()) || backward;
    }

    public boolean moveLeft() {
        return Gdx.input.isKeyPressed(settings.getMoveLeft()) || left;
    }

    public boolean moveRight() {
        return Gdx.input.isKeyPressed(settings.getMoveRight()) || right;
    }

    public boolean shoot() {
        return Gdx.input.isKeyPressed(settings.getShoot()) || shoot;
    }
    
    public boolean boosting(){
        return Gdx.input.isKeyPressed(settings.getIsBoosting()) | boost;
    }
}
