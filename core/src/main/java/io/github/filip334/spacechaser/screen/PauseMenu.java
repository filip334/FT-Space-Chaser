package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.filip334.spacechaser.ui.Buttons;
import io.github.filip334.spacechaser.ui.Fonts;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.world.GameLayout;

public class PauseMenu {

    private static final float BUTTON_WIDTH = 260f;
    private static final float BUTTON_HEIGHT = 64f;
    private static final float BUTTON_SPACING = 26f;
    private static final float TITLE_GAP = 55f;

    private final BitmapFont titleFont = Fonts.generate(44, Theme.WHITE);
    private final BitmapFont buttonFont = Fonts.generate(24, Theme.WHITE);
    private final BitmapFont subFont = Fonts.generate(18, Theme.WHITE);

    private final Rectangle resumeButton = new Rectangle();
    private final Rectangle restartButton = new Rectangle();
    private final Rectangle mainMenuButton = new Rectangle();

    // ---------------- RENDER ----------------

    public void drawOverlay(ShapeRenderer shapeRenderer, Matrix4 identityTransform) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setTransformMatrix(identityTransform);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.65f);
        shapeRenderer.rect(0f, 0f, GameLayout.WINDOW_WIDTH, GameLayout.WINDOW_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void drawMenu(SpriteBatch batch, ShapeRenderer shapeRenderer, String title, boolean showRestart, Vector2 mouse) {
        float centerX = GameLayout.WINDOW_WIDTH / 2f;
        float centerY = GameLayout.WINDOW_HEIGHT / 2f;
        float titleY = centerY + 160f;

        batch.begin();
        titleFont.setColor(Theme.WHITE);
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);
        titleFont.draw(batch, title, centerX - titleLayout.width / 2f, titleY);
        batch.end();

        float cursorTop = titleY - titleLayout.height - TITLE_GAP;

        if (showRestart) {
            cursorTop = drawButtonAndAdvance(batch, shapeRenderer, resumeButton, "Resume", centerX, cursorTop, mouse);
            cursorTop = drawButtonAndAdvance(batch, shapeRenderer, restartButton, "Restart", centerX, cursorTop, mouse);
            drawButton(batch, shapeRenderer, mainMenuButton, "Main Menu", centerX, cursorTop, mouse);
        } else {
            cursorTop = drawButtonAndAdvance(batch, shapeRenderer, resumeButton, "Resume", centerX, cursorTop, mouse);
            drawButton(batch, shapeRenderer, mainMenuButton, "Leave Match", centerX, cursorTop, mouse);
        }
    }

    public void drawInfoLeave(SpriteBatch batch, ShapeRenderer shapeRenderer, String title, String subtitle, Vector2 mouse) {
        float centerX = GameLayout.WINDOW_WIDTH / 2f;
        float centerY = GameLayout.WINDOW_HEIGHT / 2f;

        batch.begin();
        titleFont.setColor(Theme.WHITE);
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);
        titleFont.draw(batch, titleLayout, centerX - titleLayout.width / 2f, centerY + 60f);

        subFont.setColor(Theme.CYAN);
        GlyphLayout sub = new GlyphLayout(subFont, subtitle);
        subFont.draw(batch, sub, centerX - sub.width / 2f, centerY);
        batch.end();

        drawButton(batch, shapeRenderer, mainMenuButton, "Leave Match", centerX, centerY - 60f, mouse);
    }

    private float drawButtonAndAdvance(SpriteBatch batch, ShapeRenderer shapeRenderer, Rectangle bounds, String label,
                                        float centerX, float topY, Vector2 mouse) {
        drawButton(batch, shapeRenderer, bounds, label, centerX, topY, mouse);
        return topY - BUTTON_HEIGHT - BUTTON_SPACING;
    }

    private void drawButton(SpriteBatch batch, ShapeRenderer shapeRenderer, Rectangle bounds, String label,
                             float centerX, float topY, Vector2 mouse) {
        Buttons.layoutStack(bounds, centerX, topY, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_SPACING);
        boolean hovered = bounds.contains(mouse);
        Buttons.draw(batch, shapeRenderer, buttonFont, bounds, label, hovered);
    }

    // ---------------- HIT TEST ----------------

    public boolean isResumeClicked(Vector2 mouse) {
        return resumeButton.contains(mouse);
    }

    public boolean isRestartClicked(Vector2 mouse) {
        return restartButton.contains(mouse);
    }

    public boolean isMainMenuClicked(Vector2 mouse) {
        return mainMenuButton.contains(mouse);
    }

    // ---------------- DISPOSE ----------------

    public void dispose() {
        titleFont.dispose();
        buttonFont.dispose();
        subFont.dispose();
    }
}
