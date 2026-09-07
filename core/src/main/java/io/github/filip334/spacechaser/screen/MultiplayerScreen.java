package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.ui.Buttons;
import io.github.filip334.spacechaser.ui.ChaseArt;
import io.github.filip334.spacechaser.ui.Fonts;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.world.HighScoreManager;

public class MultiplayerScreen extends BaseScreen {

    private final BitmapFont titleFont;
    private final BitmapFont menuFont;
    private final BitmapFont smallFont;

    private Texture shipTexture;
    private Texture enemyTexture;
    private Texture engineFireTexture;

    private final String[] menuItems = {
            "HOST GAME",
            "JOIN GAME",
            "BACK"
    };

    private static final float BUTTON_WIDTH = 320f;
    private static final float BUTTON_HEIGHT = 58f;
    private static final float BUTTON_SPACING = 30f;
    private static final float BACK_EXTRA_GAP = 55f;
    private static final float CORNER_MARGIN = 30f;

    private final Rectangle[] menuBounds;
    private final Rectangle pilotBox = new Rectangle();

    public MultiplayerScreen(Game game) {
        super(game);

        titleFont = Fonts.generate(46, Theme.WHITE);
        menuFont = Fonts.generate(24, Theme.WHITE);
        smallFont = Fonts.generate(18, Theme.WHITE);

        shipTexture = new Texture(Gdx.files.internal("MenuAssets/shipBackground1.png"));
        enemyTexture = new Texture(Gdx.files.internal("MenuAssets/enemy.png"));
        engineFireTexture = new Texture(Gdx.files.internal("MenuAssets/engineFireMainMenu.png"));

        menuBounds = new Rectangle[menuItems.length];
        for (int i = 0; i < menuBounds.length; i++) {
            menuBounds[i] = new Rectangle();
        }
    }

    @Override
    public void render(float delta) {
        beginFrame();

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        drawBackground(width, height);
        drawTitle(width, height);
        drawChaseScene(width, height);
        drawMenu(width, height);
        drawPilot(width, height);

        handleInput(width, height);
    }

    // =========================================================
    // TITLE
    // =========================================================

    private void drawTitle(float width, float height) {
        String title = "MULTIPLAYER";
        GlyphLayout layout = new GlyphLayout(titleFont, title);

        float x = width / 2f - layout.width / 2f;
        float y = height * 0.94f; // ista visina kao naslov na Main Menu-u - ne "skace" pri prelazu ekrana

        batch.begin();
        titleFont.setColor(Theme.CYAN.r, Theme.CYAN.g, Theme.CYAN.b, 0.5f);
        titleFont.draw(batch, title, x, y + 2f);
        titleFont.setColor(Theme.WHITE);
        titleFont.draw(batch, title, x, y);
        batch.end();

        float underlineY = y - layout.height - 10f;
        drawGlowLine(x, underlineY, layout.width, 3f);
    }

    // =========================================================
    // CHASE SCENE (2 igraca, 3 raketice - ista vizuela kao Main Menu)
    // =========================================================

    /**
     * Brod A (blizi/veci) je NAMERNO identican po velicini i poziciji broda
     * na Main Menu-u (centriran, 0.62 * visina) - da prelaz izmedju ta dva
     * ekrana deluje kontinuirano, bez "skoka". Brod B i raketice su
     * rasporedjeni oko njega tako da ostanu van donjeg levog (meni) i donjeg
     * desnog (pilot box) ugla, cak i na najmanjoj dozvoljenoj velicini
     * prozora (960x600, vidi Lwjgl3Launcher).
     */
    private void drawChaseScene(float width, float height) {
        float dirRad = (float) Math.toRadians(ChaseArt.TRAIL_ANGLE_DEG);
        float dirX = (float) Math.cos(dirRad);
        float dirY = (float) Math.sin(dirRad);
        float perpRad = (float) Math.toRadians(ChaseArt.TRAIL_ANGLE_DEG - 90f);
        float perpX = (float) Math.cos(perpRad);
        float perpY = (float) Math.sin(perpRad);

        // Isto kao MainMenuScreen.drawShip().
        float shipAX = width * 0.5f;
        float shipAY = height * 0.5f;
        float shipAHeight = height * 0.62f;

        float shipBX = width * 0.82f;
        float shipBY = height * 0.40f;
        float shipBHeight = height * 0.30f;

        // Dve raketice jure brod A (jedna pravo iza, jedna postrance/dalje), jedna juri brod B.
        float enemy1X = shipAX + dirX * height * 0.34f;
        float enemy1Y = shipAY + dirY * height * 0.34f;

        float enemy2X = shipAX + dirX * height * 0.46f + perpX * height * 0.16f;
        float enemy2Y = shipAY + dirY * height * 0.46f + perpY * height * 0.16f;

        float enemy3X = shipBX + dirX * height * 0.22f;
        float enemy3Y = shipBY + dirY * height * 0.22f;

        // Neprijatelji se crtaju pre brodova - vizuelno su "iza" njih.
        ChaseArt.drawEnemy(batch, enemyTexture, engineFireTexture, enemy1X, enemy1Y, height * 0.11f, 0.85f);
        ChaseArt.drawEnemy(batch, enemyTexture, engineFireTexture, enemy2X, enemy2Y, height * 0.09f, 0.75f);
        ChaseArt.drawEnemy(batch, enemyTexture, engineFireTexture, enemy3X, enemy3Y, height * 0.10f, 0.85f);

        ChaseArt.drawShip(batch, shipTexture, engineFireTexture, shipAX, shipAY, shipAHeight, 1f);
        ChaseArt.drawShip(batch, shipTexture, engineFireTexture, shipBX, shipBY, shipBHeight, 1f);
    }

    // =========================================================
    // MENU
    // =========================================================

    private void drawMenu(float width, float height) {
        float startX = CORNER_MARGIN;
        // Donji levi ugao - BACK (poslednja stavka) je najnize, sa vecim
        // razmakom od ostalih iznad njega.
        float y = CORNER_MARGIN;

        for (int i = menuItems.length - 1; i >= 0; i--) {
            menuBounds[i].set(startX, y, BUTTON_WIDTH, BUTTON_HEIGHT);
            boolean hovered = isMouseOver(menuBounds[i]);
            Buttons.draw(batch, shapeRenderer, menuFont, menuBounds[i], menuItems[i], hovered);

            float gap = (i == menuItems.length - 1) ? BACK_EXTRA_GAP : BUTTON_SPACING;
            y += BUTTON_HEIGHT + gap;
        }
    }

    // =========================================================
    // PILOT / BEST SCORE
    // =========================================================

    /** Isti prikaz kao pilot box na Main Menu-u, samo bez editovanja imena. */
    private void drawPilot(float width, float height) {
        SpaceChaserGame spaceChaserGame = (SpaceChaserGame) game;
        HighScoreManager highScores = spaceChaserGame.getHighScoreManager();
        String pilotName = spaceChaserGame.getSettings().getPlayerName();
        drawPilotBox(pilotBox, smallFont, pilotName, false,
                highScores.getSingleplayerHighScore(), highScores.getMultiplayerHighScore());
    }

    // =========================================================
    // INPUT
    // =========================================================

    private void handleInput(float width, float height) {
        if (!Gdx.input.justTouched()) {
            return;
        }

        float mouseX = Gdx.input.getX();
        float mouseY = height - Gdx.input.getY();

        for (int i = 0; i < menuBounds.length; i++) {
            if (!menuBounds[i].contains(mouseX, mouseY)) continue;

            switch (i) {
                case 0:
                    navigateTo(new HostGameScreen(game));
                    break;
                case 1:
                    navigateTo(new JoinGameScreen(game));
                    break;
                case 2:
                    navigateTo(new MainMenuScreen(game));
                    break;
            }
        }
    }

    // =========================================================
    // SCREEN
    // =========================================================

    @Override
    public void dispose() {
        super.dispose();
        titleFont.dispose();
        menuFont.dispose();
        smallFont.dispose();

        if (shipTexture != null) {
            shipTexture.dispose();
        }
        if (enemyTexture != null) {
            enemyTexture.dispose();
        }
        if (engineFireTexture != null) {
            engineFireTexture.dispose();
        }
    }
}
