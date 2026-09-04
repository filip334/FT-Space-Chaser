package io.github.filip334.spacechaser.renderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import io.github.filip334.spacechaser.world.GameLayout;
public class HudRenderer {
    
    //
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    //
    public HudRenderer() {
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.5f);
        shapeRenderer = new ShapeRenderer();
    }

    public void setProjectionMatrix(Matrix4 projectionMatrix) {
        shapeRenderer.setProjectionMatrix(projectionMatrix);
    }
    // ---------------- HUD ----------------
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
                       float fuel, float maxFuel, int score, float gameTime) {
        float screenHeight = GameLayout.WINDOW_HEIGHT;
        float barX = (GameLayout.SIDE_PANEL_WIDTH - 220f) / 2f;
        float healthY = screenHeight - 40f;
        float fuelY = screenHeight - 75f;
        drawBar(barX, healthY, 220f, 18f, health, maxHealth, Color.RED);
        drawBar(barX, fuelY, 220f, 18f, fuel, maxFuel, Color.CYAN);
        batch.begin();
        font.draw(batch, "Health", barX, healthY + 35f);
        font.draw(batch, "Fuel", barX, fuelY + 35f);
        float infoX = GameLayout.SIDE_PANEL_WIDTH + GameLayout.MAP_VIEWPORT_SIZE
                + (GameLayout.SIDE_PANEL_WIDTH - 220f) / 2f;
        font.draw(batch, "Score: " + score, infoX, screenHeight - 110f);
        font.draw(batch, "Time: " + formatTime(gameTime), infoX, screenHeight - 145f);
        batch.end();
    }

    /**
     * Multiplayer varijanta: lokalni igrac levo, protivnik desno (mirror),
     * score/vreme deljeni (dolaze sa servera).
     */
    public void renderMultiplayer(SpriteBatch batch,
                                   float localHealth, float localMaxHealth,
                                   float localFuel, float localMaxFuel,
                                   float opponentHealth, float opponentMaxHealth,
                                   float opponentFuel, float opponentMaxFuel,
                                   boolean opponentPresent,
                                   int score, float gameTime) {

        float screenWidth = GameLayout.WINDOW_WIDTH;
        float screenHeight = GameLayout.WINDOW_HEIGHT;

        float barWidth = 220f;
        float barHeight = 18f;
        float healthY = screenHeight - 40f;
        float fuelY = screenHeight - 75f;

        // ---- LOKALNI IGRAC (levo) ----
        float localX = (GameLayout.SIDE_PANEL_WIDTH - barWidth) / 2f;
        drawBar(localX, healthY, barWidth, barHeight, localHealth, localMaxHealth, Color.RED);
        drawBar(localX, fuelY, barWidth, barHeight, localFuel, localMaxFuel, Color.CYAN);

        // ---- PROTIVNIK (desno, mirror) ----
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

        if (opponentPresent) {
            font.draw(batch, "ENEMY", opponentX, healthY + 55f);
            font.draw(batch, "Health", opponentX, healthY + 35f);
            font.draw(batch, "Fuel", opponentX, fuelY + 35f);
        }

        font.draw(batch, "Score: " + score, localX, screenHeight - 110f);
        font.draw(batch, "Time: " + formatTime(gameTime), localX, screenHeight - 145f);

        batch.end();
    }
    
    // ---------------- DISPOSE ----------------
    
    public void dispose() {
        font.dispose();
        shapeRenderer.dispose();
    }
}
