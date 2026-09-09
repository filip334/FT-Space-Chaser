package io.github.filip334.spacechaser.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class HighScoreManager {
    private static final String PREFS_NAME = "spacechaser-highscore";
    private static final String KEY_HIGH_SCORE_LEGACY = "highScore";
    private static final String KEY_HIGH_SCORE_SINGLEPLAYER = "highScoreSingleplayer";
    private static final String KEY_HIGH_SCORE_MULTIPLAYER = "highScoreMultiplayer";

    private final Preferences prefs;
    private int highScoreSingleplayer;
    private int highScoreMultiplayer;

    // ---------------- KONSTRUKTOR ----------------

    public HighScoreManager() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        highScoreSingleplayer = prefs.getInteger(KEY_HIGH_SCORE_SINGLEPLAYER, 0);
        highScoreMultiplayer = prefs.getInteger(KEY_HIGH_SCORE_MULTIPLAYER, 0);
        migrateLegacyScore();
    }

    // ---------------- LEGACY MIGRATION ----------------

    private void migrateLegacyScore() {
        if (!prefs.contains(KEY_HIGH_SCORE_LEGACY)) return;

        int legacyScore = prefs.getInteger(KEY_HIGH_SCORE_LEGACY, 0);
        if (legacyScore > highScoreSingleplayer) {
            highScoreSingleplayer = legacyScore;
            prefs.putInteger(KEY_HIGH_SCORE_SINGLEPLAYER, highScoreSingleplayer);
        }
        prefs.remove(KEY_HIGH_SCORE_LEGACY);
        prefs.flush();
    }

    // ---------------- GET / SET ----------------

    public int getSingleplayerHighScore() {
        return highScoreSingleplayer;
    }

    public int getMultiplayerHighScore() {
        return highScoreMultiplayer;
    }

    // ---------------- REPORT SCORE ----------------

    public boolean reportSingleplayerScore(int score) {
        if (score <= highScoreSingleplayer) {
            return false;
        }
        highScoreSingleplayer = score;
        prefs.putInteger(KEY_HIGH_SCORE_SINGLEPLAYER, highScoreSingleplayer);
        prefs.flush();
        return true;
    }

    public boolean reportMultiplayerScore(int score) {
        if (score <= highScoreMultiplayer) {
            return false;
        }
        highScoreMultiplayer = score;
        prefs.putInteger(KEY_HIGH_SCORE_MULTIPLAYER, highScoreMultiplayer);
        prefs.flush();
        return true;
    }
}
