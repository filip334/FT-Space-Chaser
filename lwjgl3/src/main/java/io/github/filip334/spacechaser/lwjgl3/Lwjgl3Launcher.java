package io.github.filip334.spacechaser.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.world.GameLayout;

public class Lwjgl3Launcher {
    // ---------------- ENTRY POINT ----------------

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        createApplication();
    }

    private static final int MIN_WINDOW_WIDTH = 960;
    private static final int MIN_WINDOW_HEIGHT = 600;

    // ---------------- APPLICATION SETUP ----------------

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new SpaceChaserGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("FTSpaceChaser2.0");
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);

        int availableWidth = Lwjgl3ApplicationConfiguration.getDisplayMode().width - 80;
        int availableHeight = Lwjgl3ApplicationConfiguration.getDisplayMode().height - 120;
        float scale = Math.min(1f, Math.min(
                availableWidth / (float) GameLayout.WINDOW_WIDTH,
                availableHeight / (float) GameLayout.WINDOW_HEIGHT));
        configuration.setWindowedMode(
                Math.max(MIN_WINDOW_WIDTH, Math.round(GameLayout.WINDOW_WIDTH * scale)),
                Math.max(MIN_WINDOW_HEIGHT, Math.round(GameLayout.WINDOW_HEIGHT * scale)));
        configuration.setResizable(true);
        configuration.setWindowSizeLimits(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT, -1, -1);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        return configuration;
    }
}
