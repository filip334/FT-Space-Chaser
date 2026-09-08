package io.github.filip334.spacechaser.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Cuva najbolji skor trajno (LibGDX Preferences - fajl/registry u zavisnosti
 * od platforme). Singleplayer i multiplayer imaju odvojene rekorde - drugaciji
 * su nacini igre (multiplayer ima drugog igraca/wave sistem koji utice na
 * bodovanje), pa ih ne treba mesati u jedan zajednicki rekord.
 */
public class HighScoreManager {
    private static final String PREFS_NAME = "spacechaser-highscore";
    private static final String KEY_HIGH_SCORE_LEGACY = "highScore"; // stari, zajednicki kljuc pre razdvajanja
    private static final String KEY_HIGH_SCORE_SINGLEPLAYER = "highScoreSingleplayer";
    private static final String KEY_HIGH_SCORE_MULTIPLAYER = "highScoreMultiplayer";

    private final Preferences prefs;
    private int highScoreSingleplayer;
    private int highScoreMultiplayer;

    public HighScoreManager() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        highScoreSingleplayer = prefs.getInteger(KEY_HIGH_SCORE_SINGLEPLAYER, 0);
        highScoreMultiplayer = prefs.getInteger(KEY_HIGH_SCORE_MULTIPLAYER, 0);
        migrateLegacyScore();
    }

    /**
     * Pre razdvajanja je postojao samo jedan zajednicki rekord - preselimo ga
     * jednom u singleplayer rekord (tu se najcesce i postizao) da igrac ne
     * izgubi postojeci "Best score" posle ove izmene.
     */
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

    public int getSingleplayerHighScore() {
        return highScoreSingleplayer;
    }

    public int getMultiplayerHighScore() {
        return highScoreMultiplayer;
    }

    /**
     * Prijavljuje trenutni skor singleplayer partije - ako je novi rekord,
     * upisuje se odmah na disk. Bezbedno se poziva svaki frejm (upis se desi
     * samo kad se rekord stvarno popravi).
     * @return true ako je ovo bio novi rekord.
     */
    public boolean reportSingleplayerScore(int score) {
        if (score <= highScoreSingleplayer) {
            return false;
        }
        highScoreSingleplayer = score;
        prefs.putInteger(KEY_HIGH_SCORE_SINGLEPLAYER, highScoreSingleplayer);
        prefs.flush();
        return true;
    }

    /** Isto kao reportSingleplayerScore, samo za multiplayer rekord. */
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
