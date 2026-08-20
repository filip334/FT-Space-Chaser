package io.github.filip334.spacechaser;

import com.badlogic.gdx.Game;
import io.github.filip334.spacechaser.screen.GameScreen;
import io.github.filip334.spacechaser.screen.MainMenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class SpaceChaserGame extends Game {
   @Override
    public void create() {
        setScreen(new MainMenuScreen(this));
    }
}
