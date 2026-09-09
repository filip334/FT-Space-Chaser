package io.github.filip334.spacechaser.renderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import io.github.filip334.spacechaser.ui.Fonts;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.world.GameLayout;
public class HudRenderer {

    private final BitmapFont font;
    private final BitmapFont countdownFont;
    private final ShapeRenderer shapeRenderer;
    // ---------------- KONSTRUKTOR ----------------

    public HudRenderer() {
        font = Fonts.generate(17, Theme.WHITE);
        countdownFont = Fonts.generate(90, Theme.CYAN);
        shapeRenderer = new ShapeRenderer();
    }

    // ---------------- GET / SET ----------------

    public void setProjectionMatrix(Matrix4 projectionMatrix) {
        shapeRenderer.setProjectionMatrix(projectionMatrix);
    }

    // ---------------- RENDER ----------------

    public void renderCountdown(SpriteBatch batch, int secondsRemaining) {
        if (secondsRemaining <= 0) return;

        float centerX = GameLayout.WINDOW_WIDTH / 2f;
        float centerY = GameLayout.WINDOW_HEIGHT / 2f;

        String text = String.valueOf(secondsRemaining);

        batch.begin();
        GlyphLayout layout = new GlyphLayout(countdownFont, text);
        countdownFont.draw(batch, text, centerX - layout.width / 2f, centerY + layout.height / 2f);
        batch.end();
    }

    // ---------------- HELPERS ----------------
    private void drawBar(float x, float y, float width, float height,
                         float value, float maxValue, Color fillColor) {
        float percent = value / maxValue;
        if (percent < 0f) {
            percent = 0f;
        }
        if (percent > 1f) {
            percent = 1f;
        }
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.setColor(fillColor);
        shapeRenderer.rect(x, y, width * percent, height);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();
    }
    private String formatTime(float gameTime) {
        int totalSeconds = (int) gameTime;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ---------------- RENDER ----------------

    public void render(SpriteBatch batch, float health, float maxHealth,
                       float fuel, float maxFuel, int score, float gameTime, int wave) {
        float screenHeight = GameLayout.WINDOW_HEIGHT;
        float barX = (GameLayout.SIDE_PANEL_WIDTH - 220f) / 2f;
        float healthY = screenHeight - 60f;
        float fuelY = screenHeight - 110f;
        drawBar(barX, healthY, 220f, 18f, health, maxHealth, Color.RED);
        drawBar(barX, fuelY, 220f, 18f, fuel, maxFuel, Color.CYAN);
        batch.begin();
        font.draw(batch, "Health", barX, healthY + 35f);
        font.draw(batch, "Fuel", barX, fuelY + 35f);
        font.draw(batch, "Score: " + score, barX, fuelY - 30f);
        font.draw(batch, "Time: " + formatTime(gameTime), barX, fuelY - 65f);
        font.draw(batch, "Wave: " + wave, barX, fuelY - 100f);
        batch.end();
    }

    public void renderMultiplayer(SpriteBatch batch,
                                   float localHealth, float localMaxHealth,
                                   float localFuel, float localMaxFuel, int localScore,
                                   float opponentHealth, float opponentMaxHealth,
                                   float opponentFuel, float opponentMaxFuel, int opponentScore,
                                   boolean opponentPresent,
                                   int totalScore, float gameTime, int wave) {

        float screenWidth = GameLayout.WINDOW_WIDTH;
        float screenHeight = GameLayout.WINDOW_HEIGHT;

        float barWidth = 220f;
        float barHeight = 18f;
        float healthY = screenHeight - 60f;
        float fuelY = screenHeight - 110f;

        float localX = (GameLayout.SIDE_PANEL_WIDTH - barWidth) / 2f;
        drawBar(localX, healthY, barWidth, barHeight, localHealth, localMaxHealth, Color.RED);
        drawBar(localX, fuelY, barWidth, barHeight, localFuel, localMaxFuel, Color.CYAN);

        float opponentX = screenWidth - GameLayout.SIDE_PANEL_WIDTH
                + (GameLayout.SIDE_PANEL_WIDTH - barWidth) / 2f;
        if (opponentPresent) {
            drawBar(opponentX, healthY, barWidth, barHeight, opponentHealth, opponentMaxHealth, Color.RED);
            drawBar(opponentX, fuelY, barWidth, barHeight, opponentFuel, opponentMaxFuel, Color.CYAN);
        }

        batch.begin();

        font.draw(batch, "YOU", localX, healthY + 55f);
        font.draw(batch, "Health", localX, healthY + 35f);
        font.draw(batch, "Fuel", localX, fuelY + 35f);
        font.draw(batch, "Score: " + localScore, localX, fuelY - 30f);

        if (opponentPresent) {
            font.draw(batch, "ENEMY", opponentX, healthY + 55f);
            font.draw(batch, "Health", opponentX, healthY + 35f);
            font.draw(batch, "Fuel", opponentX, fuelY + 35f);
            font.draw(batch, "Score: " + opponentScore, opponentX, fuelY - 30f);
        }

        font.draw(batch, "Total: " + totalScore, localX, screenHeight - 160f);
        font.draw(batch, "Time: " + formatTime(gameTime), localX, screenHeight - 195f);
        font.draw(batch, "Wave: " + wave, localX, screenHeight - 230f);

        batch.end();
    }

    // ---------------- DISPOSE ----------------

    public void dispose() {
        font.dispose();
        countdownFont.dispose();
        shapeRenderer.dispose();
    }
}
