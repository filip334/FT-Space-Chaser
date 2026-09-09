package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.ui.ChaseArt;
import io.github.filip334.spacechaser.ui.Fonts;
import io.github.filip334.spacechaser.ui.NameEditor;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.settings.HighScoreManager;

public class MainMenuScreen extends BaseScreen {

    private final BitmapFont titleFont;
    private final BitmapFont menuFont;
    private final BitmapFont smallFont;

    private Texture shipTexture;
    private Texture enemyTexture;
    private Texture engineFireTexture;
    private final String statusMessage;
    // Ne mere se od konstruktora/prvog delta-a - konstruktor sinhrono ucitava
    // 3 teksture + 3 fonta (FreeType generise iznova svaki put, bez kesiranja),
    // sto moze potrajati i to bi "pojelo" veci deo od 10 sekundi ako bismo
    // sabirali delta od prvog render() poziva (taj prvi delta ukljucuje i
    // vreme ucitavanja). Umesto toga, satat se prvi put kad se poruka STVARNO
    // nacrta na ekranu, pa se broji stvarno proteklo vreme od tog trenutka.
    private long statusMessageShownAtMillis = -1L;
    private static final float STATUS_MESSAGE_DURATION = 10f;

    private final String[] menuItems = {
            "SINGLEPLAYER",
            "MULTIPLAYER",
            "SETTINGS",
            "EXIT"
    };

    private static final float BUTTON_WIDTH = 300f;
    private static final float BUTTON_HEIGHT = 58f;
    private static final float BUTTON_SPACING = 30f;
    private static final float EXIT_EXTRA_GAP = 55f;
    private static final float CORNER_MARGIN = 30f;

    private final Rectangle[] menuBounds;
    private final Rectangle pilotBox = new Rectangle();
    private final NameEditor nameEditor;

    // ---------------- KONSTRUKTORI ----------------

    public MainMenuScreen(Game game) {
        this(game, null);
    }

    public MainMenuScreen(Game game, String statusMessage) {
        super(game);
        this.statusMessage = statusMessage;

        titleFont = Fonts.generate(50, Theme.WHITE);
        menuFont = Fonts.generate(24, Theme.WHITE);
        smallFont = Fonts.generate(18, Theme.WHITE);

        shipTexture = new Texture(Gdx.files.internal("MenuAssets/shipBackground1.png"));
        enemyTexture = new Texture(Gdx.files.internal("MenuAssets/enemy.png"));
        engineFireTexture = new Texture(Gdx.files.internal("MenuAssets/engineFireMainMenu.png"));

        menuBounds = new Rectangle[menuItems.length];
        for (int i = 0; i < menuBounds.length; i++) {
            menuBounds[i] = new Rectangle();
        }

        nameEditor = new NameEditor(((SpaceChaserGame) game).getSettings());
    }

    // ---------------- RENDER ----------------

    @Override
    public void render(float delta) {
        beginFrame();

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        drawBackground(width, height);

        drawTitle(width, height);
        drawEnemy(width, height);
        drawShip(width, height);
        drawMenu(width, height);
        drawPilot(width, height);
        drawStatusMessage(width, height);

        handleInput(width, height);
    }

    // ---------------- TITLE ----------------

    private void drawTitle(float width, float height) {
        String title = "FT - SPACE CHASER";
        GlyphLayout layout = new GlyphLayout(titleFont, title);

        float x = width / 2f - layout.width / 2f;
        float y = height * 0.94f;

        batch.begin();
        // blagi glow iza teksta - isti tekst iscrtan malo vece/providnije pozadi
        titleFont.getData().setScale(1.04f);
        titleFont.setColor(Theme.CYAN.r, Theme.CYAN.g, Theme.CYAN.b, 0.5f);
        GlyphLayout glowLayout = new GlyphLayout(titleFont, title);
        titleFont.draw(batch, title, width / 2f - glowLayout.width / 2f, y + 2f);

        titleFont.getData().setScale(1f);
        titleFont.setColor(Theme.WHITE);
        titleFont.draw(batch, title, x, y);
        batch.end();

        float underlineY = y - layout.height - 10f;
        drawGlowLine(x, underlineY, layout.width, 3f);
    }

    // ---------------- SHIP / ENEMY ----------------

    private void drawShip(float width, float height) {
        float shipHeight = height * 0.62f;
        // Centrirano na sredini ekrana - slika vec ima svoj prirodan dijagonalan
        // ugao, ne treba joj vise rotacija kao staroj Original/ship.png teksturi.
        ChaseArt.drawShip(batch, shipTexture, engineFireTexture, width / 2f, height / 2f, shipHeight, 1f);
    }

    /**
     * Neprijatelj negde iza broda, okrenut u istom pravcu kretanja kao igrac -
     * izgleda kao da ga juri. Manji je i dalje od centra (dublje u pozadini).
     */
    private void drawEnemy(float width, float height) {
        float enemyHeight = height * 0.17f;
        float chaseOffset = height * 0.53f; // malo vise ka gornjem desnom cosku
        float rad = (float) Math.toRadians(ChaseArt.TRAIL_ANGLE_DEG);
        float centerX = width / 2f + chaseOffset * (float) Math.cos(rad);
        float centerY = height / 2f + chaseOffset * (float) Math.sin(rad);

        ChaseArt.drawEnemy(batch, enemyTexture, engineFireTexture, centerX, centerY, enemyHeight, 0.85f);
    }

    // ---------------- MENU ----------------

    private void drawMenu(float width, float height) {
        float scale = computeUiScale(width, height);
        menuFont.getData().setScale(scale);

        drawVerticalMenu(menuFont, menuItems, menuBounds, CORNER_MARGIN, CORNER_MARGIN,
                BUTTON_WIDTH * scale, BUTTON_HEIGHT * scale, BUTTON_SPACING * scale, EXIT_EXTRA_GAP * scale);

        menuFont.getData().setScale(1f);
    }

    // ---------------- PILOT / BEST SCORE ----------------

    private void drawPilot(float width, float height) {
        HighScoreManager highScores = ((SpaceChaserGame) game).getHighScoreManager();
        drawPilotBox(pilotBox, smallFont, nameEditor.getDisplayText(), nameEditor.isEditing(),
                highScores.getSingleplayerHighScore(), highScores.getMultiplayerHighScore());
    }

    // ---------------- STATUS MESSAGE ----------------

    private void drawStatusMessage(float width, float height) {
        if (statusMessage == null || statusMessage.isEmpty()) return;

        if (statusMessageShownAtMillis < 0) {
            statusMessageShownAtMillis = TimeUtils.millis();
        }
        if (TimeUtils.timeSinceMillis(statusMessageShownAtMillis) >= STATUS_MESSAGE_DURATION * 1000L) return;

        // Centrirano, skroz na dnu ekrana - menu i pilot box su u uglovima,
        // ovde po sredini nema sa cim da se preklopi.
        GlyphLayout layout = new GlyphLayout(smallFont, statusMessage);
        float paddingX = 24f;
        float paddingY = 14f;
        float boxWidth = layout.width + paddingX * 2f;
        float boxHeight = layout.height + paddingY * 2f;
        float boxX = width / 2f - boxWidth / 2f;
        float boxY = CORNER_MARGIN;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.25f, 0f, 0f, 0.55f);
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
        shapeRenderer.setColor(Theme.RED);
        float t = 2.5f;
        shapeRenderer.rect(boxX, boxY, boxWidth, t);
        shapeRenderer.rect(boxX, boxY + boxHeight - t, boxWidth, t);
        shapeRenderer.rect(boxX, boxY, t, boxHeight);
        shapeRenderer.rect(boxX + boxWidth - t, boxY, t, boxHeight);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        smallFont.setColor(Theme.RED);
        smallFont.draw(batch, statusMessage, boxX + paddingX, boxY + paddingY + layout.height);
        smallFont.setColor(Theme.WHITE);
        batch.end();
    }

    // ---------------- INPUT ----------------

    private void handleInput(float width, float height) {
        if (!Gdx.input.justTouched()) {
            return;
        }

        float mouseX = Gdx.input.getX();
        float mouseY = height - Gdx.input.getY();

        if (pilotBox.contains(mouseX, mouseY)) {
            nameEditor.begin();
            return;
        }

        nameEditor.finish();

        for (int i = 0; i < menuBounds.length; i++) {
            if (!menuBounds[i].contains(mouseX, mouseY)) continue;

            switch (i) {
                case 0:
                    navigateTo(new GameScreen(game));
                    break;
                case 1:
                    navigateTo(new MultiplayerScreen(game));
                    break;
                case 2:
                    navigateTo(new SettingsScreen(game));
                    break;
                case 3:
                    Gdx.app.exit();
                    break;
            }
        }
    }

    // ---------------- SCREEN METHODS ----------------

    @Override
    public void hide() {
        nameEditor.finish();
    }

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
