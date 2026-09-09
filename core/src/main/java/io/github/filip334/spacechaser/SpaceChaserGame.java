package io.github.filip334.spacechaser;

import com.badlogic.gdx.Game;
import io.github.filip334.spacechaser.screen.GameScreen;
import io.github.filip334.spacechaser.screen.MainMenuScreen;
import io.github.filip334.spacechaser.settings.GameSettings;
import io.github.filip334.spacechaser.settings.HighScoreManager;

public class SpaceChaserGame extends Game {
    private final GameSettings settings = new GameSettings();
    private HighScoreManager highScoreManager;

    // ---------------- GET / SET ----------------

    public GameSettings getSettings() {
        return settings;
    }

    public HighScoreManager getHighScoreManager() {
        return highScoreManager;
    }

    // ---------------- KONSTRUKTOR / CREATE ----------------

   @Override
    public void create() {
        highScoreManager = new HighScoreManager();
        setScreen(new MainMenuScreen(this));
    }
}
