package io.github.filip334.spacechaser;

import com.badlogic.gdx.Game;
import io.github.filip334.spacechaser.screen.GameScreen;
import io.github.filip334.spacechaser.screen.MainMenuScreen;
import io.github.filip334.spacechaser.settings.GameSettings;
import io.github.filip334.spacechaser.settings.HighScoreManager;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class SpaceChaserGame extends Game {
    private final GameSettings settings = new GameSettings();
    // Preferences (unutar HighScoreManager) zahteva Gdx.app, koji jos ne
    // postoji dok se ovaj objekat konstruise (backend ga postavlja tek posle) -
    // zato se pravi u create(), ne kao eager polje.
    private HighScoreManager highScoreManager;

    public GameSettings getSettings() {
        return settings;
    }

    public HighScoreManager getHighScoreManager() {
        return highScoreManager;
    }

   @Override
    public void create() {
        highScoreManager = new HighScoreManager();
        setScreen(new MainMenuScreen(this));
    }
}
