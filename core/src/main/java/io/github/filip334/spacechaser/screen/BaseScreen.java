package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.ui.Background;
import io.github.filip334.spacechaser.ui.Buttons;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.ui.UiPanel;
import io.github.filip334.spacechaser.world.GameLayout;

public abstract class BaseScreen implements Screen {

    protected final Game game;
    protected final SpriteBatch batch;
    protected final ShapeRenderer shapeRenderer;

    private Texture backgroundGradient;

    // ---------------- KONSTRUKTOR ----------------

    protected BaseScreen(Game game) {
        this.game = game;
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
    }

    // ---------------- SCREEN METHODS ----------------

    @Override
    public void show() {
        updateScreenProjection(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void resize(int width, int height) {
        updateScreenProjection(width, height);
    }

    // ---------------- RENDER HELPERS ----------------

    protected void updateScreenProjection(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0f, 0f, width, height);
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
    }

    protected void beginFrame() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(Color.BLACK);
    }

    protected void drawBackground(float width, float height) {
        if (backgroundGradient == null) {
            backgroundGradient = Background.createGradientTexture();
        }
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(backgroundGradient, 0, 0, width, height);
        batch.end();
    }

    protected void drawGlowLine(float x, float y, float width, float thickness) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Theme.CYAN.r, Theme.CYAN.g, Theme.CYAN.b, 0.15f);
        shapeRenderer.rect(x, y - 4f, width, thickness + 8f);
        shapeRenderer.setColor(Theme.CYAN);
        shapeRenderer.rect(x, y, width, thickness);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    protected void drawPilotBox(Rectangle bounds, BitmapFont font, String pilotName, boolean highlighted,
                                 int spHighScore, int mpHighScore) {
        float boxWidth = 260f;
        float boxHeight = 102f;
        float x = Gdx.graphics.getWidth() - boxWidth - 30f;
        float y = 24f;
        bounds.set(x, y, boxWidth, boxHeight);

        UiPanel.draw(shapeRenderer, x, y, boxWidth, boxHeight, highlighted);

        batch.begin();
        font.setColor(highlighted ? Theme.WHITE : Theme.CYAN);
        font.draw(batch, "Pilot : " + pilotName, x + 16f, y + boxHeight - 16f);
        font.setColor(Theme.WHITE);
        font.draw(batch, "Best (SP) : " + spHighScore, x + 16f, y + boxHeight - 44f);
        font.draw(batch, "Best (MP) : " + mpHighScore, x + 16f, y + boxHeight - 72f);
        batch.end();
    }

    protected void drawVerticalMenu(BitmapFont font, String[] items, Rectangle[] bounds,
                                     float startX, float startY, float buttonWidth, float buttonHeight,
                                     float spacing, float lastItemExtraGap) {
        float y = startY;

        for (int i = items.length - 1; i >= 0; i--) {
            bounds[i].set(startX, y, buttonWidth, buttonHeight);
            boolean hovered = isMouseOver(bounds[i]);
            Buttons.draw(batch, shapeRenderer, font, bounds[i], items[i], hovered);

            float gap = (i == items.length - 1) ? lastItemExtraGap : spacing;
            y += buttonHeight + gap;
        }
    }

    // ---------------- UTIL ----------------

    protected float computeUiScale(float width, float height) {
        return Math.min(width / GameLayout.WINDOW_WIDTH, height / GameLayout.WINDOW_HEIGHT);
    }

    protected boolean isMouseOver(Rectangle bounds) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        return bounds.contains(mouseX, mouseY);
    }

    // ---------------- NAVIGATION ----------------

    protected void navigateTo(Screen next) {
        dispose();
        game.setScreen(next);
    }

    // ---------------- SCREEN METHODS ----------------

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    // ---------------- DISPOSE ----------------

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (backgroundGradient != null) {
            backgroundGradient.dispose();
        }
    }
}
