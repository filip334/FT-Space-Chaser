/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.filip334.spacechaser.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 *
 * @author Todorovic
 */
public class HudRenderer {
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;

    public HudRenderer() {
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.5f);

        shapeRenderer = new ShapeRenderer();
    }

    public void render(SpriteBatch batch, float health, float maxHealth,
                       float fuel, float maxFuel, int score, float gameTime) {

        float screenHeight = Gdx.graphics.getHeight();

        float barX = 20f;
        float healthY = screenHeight - 40f;
        float fuelY = screenHeight - 75f;

        drawBar(barX, healthY, 220f, 18f, health, maxHealth, Color.RED);
        drawBar(barX, fuelY, 220f, 18f, fuel, maxFuel, Color.CYAN);

        batch.begin();

        font.draw(batch, "Health", barX, healthY + 35f);
        font.draw(batch, "Fuel", barX, fuelY + 35f);

        font.draw(batch, "Score: " + score, 20f, screenHeight - 110f);
        font.draw(batch, "Time: " + formatTime(gameTime), 20f, screenHeight - 145f);

        batch.end();
    }

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

    public void dispose() {
        font.dispose();
        shapeRenderer.dispose();
    }
}
