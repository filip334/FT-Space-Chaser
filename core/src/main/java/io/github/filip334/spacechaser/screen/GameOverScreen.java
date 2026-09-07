package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import io.github.filip334.spacechaser.ui.Buttons;
import io.github.filip334.spacechaser.ui.Fonts;
import io.github.filip334.spacechaser.ui.Theme;

/**
 * Ekran posle meca (singleplayer smrt ili kraj multiplayer meca) - prikazuje
 * konacan skor, "novi rekord" poruku ako je zasluzena, i nudi Play Again
 * (nova partija istog moda) ili Main Menu.
 */
public class GameOverScreen extends BaseScreen {

    /** Odredjuje sta "Play Again" radi - nova singleplayer partija ili nazad na Multiplayer meni za novo hostovanje/pridruzivanje. */
    public enum Mode {
        SINGLEPLAYER, MULTIPLAYER
    }

    private final int score;
    private final boolean newHighScore;
    private final Mode mode;

    private final BitmapFont titleFont;
    private final BitmapFont highScoreFont;
    private final BitmapFont scoreFont;
    private final BitmapFont buttonFont;

    private static final float BUTTON_WIDTH = 280f;
    private static final float BUTTON_HEIGHT = 64f;
    private static final float BUTTON_SPACING = 26f;

    private final Rectangle playAgainButton = new Rectangle();
    private final Rectangle mainMenuButton = new Rectangle();

    public GameOverScreen(Game game, int score, boolean newHighScore, Mode mode) {
        super(game);
        this.score = score;
        this.newHighScore = newHighScore;
        this.mode = mode;

        titleFont = Fonts.generate(54, Theme.WHITE);
        highScoreFont = Fonts.generate(26, Theme.CYAN);
        scoreFont = Fonts.generate(32, Theme.WHITE);
        buttonFont = Fonts.generate(24, Theme.WHITE);
    }

    @Override
    public void render(float delta) {
        beginFrame();

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        drawBackground(width, height);
        drawTitleAndScore(width, height);
        drawButtons(width, height);

        handleInput();
    }

    private void drawTitleAndScore(float width, float height) {
        float centerX = width / 2f;
        float cursorY = height * 0.72f;

        String title = "GAME OVER";
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);

        batch.begin();
        titleFont.setColor(Theme.CYAN.r, Theme.CYAN.g, Theme.CYAN.b, 0.5f);
        titleFont.draw(batch, title, centerX - titleLayout.width / 2f, cursorY + 2f);
        titleFont.setColor(Theme.WHITE);
        titleFont.draw(batch, title, centerX - titleLayout.width / 2f, cursorY);
        batch.end();

        cursorY -= titleLayout.height + 45f;

        if (newHighScore) {
            String record = "NEW HIGH SCORE!";
            GlyphLayout recordLayout = new GlyphLayout(highScoreFont, record);
            batch.begin();
            highScoreFont.setColor(Theme.CYAN);
            highScoreFont.draw(batch, record, centerX - recordLayout.width / 2f, cursorY);
            batch.end();
            cursorY -= recordLayout.height + 35f;
        }

        String scoreText = "Score: " + score;
        GlyphLayout scoreLayout = new GlyphLayout(scoreFont, scoreText);
        batch.begin();
        scoreFont.setColor(Theme.WHITE);
        scoreFont.draw(batch, scoreText, centerX - scoreLayout.width / 2f, cursorY);
        batch.end();
    }

    private void drawButtons(float width, float height) {
        float centerX = width / 2f;
        float topY = height * 0.32f;

        topY = Buttons.layoutStack(playAgainButton, centerX, topY, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_SPACING);
        Buttons.draw(batch, shapeRenderer, buttonFont, playAgainButton, "Play Again", isMouseOver(playAgainButton));

        Buttons.layoutStack(mainMenuButton, centerX, topY, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_SPACING);
        Buttons.draw(batch, shapeRenderer, buttonFont, mainMenuButton, "Main Menu", isMouseOver(mainMenuButton));
    }

    private void handleInput() {
        if (!Gdx.input.justTouched()) return;

        if (isMouseOver(playAgainButton)) {
            playAgain();
        } else if (isMouseOver(mainMenuButton)) {
            navigateTo(new MainMenuScreen(game));
        }
    }

    private void playAgain() {
        if (mode == Mode.SINGLEPLAYER) {
            navigateTo(new GameScreen(game));
        } else {
            navigateTo(new MultiplayerScreen(game));
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        titleFont.dispose();
        highScoreFont.dispose();
        scoreFont.dispose();
        buttonFont.dispose();
    }
}
