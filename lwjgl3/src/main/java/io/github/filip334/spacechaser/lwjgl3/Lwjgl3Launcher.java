package io.github.filip334.spacechaser.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.world.GameLayout;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    // Meniji (Main Menu, Multiplayer, Host/Join lobby, Pause...) crtaju dugmad
    // na fiksnim piksel koordinatama, ne kroz viewport koji bi ih sam skalirao
    // kao sto to radi sama igra (GameScreen koristi FitViewport). Zato prozor
    // ne sme da se smanji ispod velicine na kojoj ta dugmad jos uvek staju bez
    // preklapanja - umesto da menjamo raspored u svakom ekranu, jednostavnije
    // je da ogranicimo koliko mali sam prozor sme da bude.
    private static final int MIN_WINDOW_WIDTH = 960;
    private static final int MIN_WINDOW_HEIGHT = 600;

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new SpaceChaserGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("FTSpaceChaser2.0");
        //// Vsync limits the frames per second to what your hardware can display, and helps eliminate
        //// screen tearing. This setting doesn't always work on Linux, so the line after is a safeguard.
        configuration.useVsync(true);
        //// Limits FPS to the refresh rate of the currently active monitor, plus 1 to try to match fractional
        //// refresh rates. The Vsync setting above should limit the actual FPS to match the monitor.
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.

        int availableWidth = Lwjgl3ApplicationConfiguration.getDisplayMode().width - 80;
        int availableHeight = Lwjgl3ApplicationConfiguration.getDisplayMode().height - 120;
        float scale = Math.min(1f, Math.min(
                availableWidth / (float) GameLayout.WINDOW_WIDTH,
                availableHeight / (float) GameLayout.WINDOW_HEIGHT));
        configuration.setWindowedMode(
                Math.max(MIN_WINDOW_WIDTH, Math.round(GameLayout.WINDOW_WIDTH * scale)),
                Math.max(MIN_WINDOW_HEIGHT, Math.round(GameLayout.WINDOW_HEIGHT * scale)));
        configuration.setResizable(true);
        // Korisnik ne moze rucno da smanji prozor ispod ovoga (max sirina/visina
        // -1 = bez gornjeg ogranicenja).
        configuration.setWindowSizeLimits(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT, -1, -1);
        //// You can change these files; they are in lwjgl3/src/main/resources/ .
        //// They can also be loaded from the root of assets/ .
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        //// This could improve compatibility with Windows machines with buggy OpenGL drivers, Macs
        //// with Apple Silicon that have to emulate compatibility with OpenGL anyway, and more.
        //// This uses the dependency `com.badlogicgames.gdx:gdx-lwjgl3-angle` to function.
        //// You would need to add this line to lwjgl3/build.gradle , below the dependency on `gdx-backend-lwjgl3`:
        ////     implementation "com.badlogicgames.gdx:gdx-lwjgl3-angle:$gdxVersion"
        //// You can choose to add the following line and the mentioned dependency if you want; they
        //// are not intended for games that use GL30 (which is compatibility with OpenGL ES 3.0).
        //// Know that it might not work well in some cases.
//        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 0, 0);

        return configuration;
    }
}
