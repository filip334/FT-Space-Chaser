package io.github.filip334.spacechaser;

import com.badlogic.gdx.Game;
import io.github.filip334.spacechaser.screen.GameScreen;
import io.github.filip334.spacechaser.screen.MainMenuScreen;
import io.github.filip334.spacechaser.world.GameSettings;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class SpaceChaserGame extends Game {
    private final GameSettings settings = new GameSettings();

    public GameSettings getSettings() {
        return settings;
    }

   @Override
    public void create() {
        setScreen(new MainMenuScreen(this));
    }
}
