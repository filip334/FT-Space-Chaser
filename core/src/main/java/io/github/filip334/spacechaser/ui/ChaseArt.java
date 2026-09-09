package io.github.filip334.spacechaser.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import io.github.filip334.spacechaser.util.RotationUtils;

public final class ChaseArt {

    public static final float TRAIL_ANGLE_DEG = 42f;
    private static final float SHIP_TRAVEL_ANGLE_DEG = TRAIL_ANGLE_DEG + 180f;
    private static final float FLAME_ANGLE_TOP_DEG = 27f;
    private static final float FLAME_ANGLE_BOTTOM_DEG = 30f;
    private static final float ENEMY_TEXTURE_NOSE_DEG = 90f;
    private static final float ENEMY_ROTATION_DEG = SHIP_TRAVEL_ANGLE_DEG - ENEMY_TEXTURE_NOSE_DEG;

    private ChaseArt() {
    }

    // ---------------- SHIP ----------------

    public static void drawShip(SpriteBatch batch, Texture shipTexture, Texture fireTexture,
                                 float centerX, float centerY, float shipHeight, float alpha) {
        float ratio = shipTexture.getWidth() / (float) shipTexture.getHeight();
        float shipWidth = shipHeight * ratio;
        float x = centerX - shipWidth / 2f;
        float y = centerY - shipHeight / 2f;

        drawEngineFire(batch, fireTexture, x + shipWidth * 0.70f, y + shipHeight * (1f - 0.235f),
                shipWidth * 0.42f, FLAME_ANGLE_TOP_DEG, alpha);
        drawEngineFire(batch, fireTexture, x + shipWidth * 0.84f, y + shipHeight * (1f - 0.53f),
                shipWidth * 0.42f, FLAME_ANGLE_BOTTOM_DEG, alpha);

        batch.begin();
        batch.setColor(0f, 0.9f, 1f, 0.10f * alpha);
        batch.draw(shipTexture, x, y, shipWidth / 2f, shipHeight / 2f, shipWidth, shipHeight,
                1f, 1f, 0f, 0, 0, shipTexture.getWidth(), shipTexture.getHeight(), false, false);

        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(shipTexture, x, y, shipWidth / 2f, shipHeight / 2f, shipWidth, shipHeight,
                1f, 1f, 0f, 0, 0, shipTexture.getWidth(), shipTexture.getHeight(), false, false);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    // ---------------- ENEMY ----------------

    public static void drawEnemy(SpriteBatch batch, Texture enemyTexture, Texture fireTexture,
                                  float centerX, float centerY, float enemyHeight, float alpha) {
        float enemyWidth = enemyHeight * (enemyTexture.getWidth() / (float) enemyTexture.getHeight());
        float x = centerX - enemyWidth / 2f;
        float y = centerY - enemyHeight / 2f;

        float localOffsetY = -enemyHeight * 0.38f;
        Vector2 flame = RotationUtils.rotateOffset(centerX, centerY, 0f, localOffsetY, ENEMY_ROTATION_DEG);

        drawEngineFire(batch, fireTexture, flame.x, flame.y, enemyWidth * 0.95f, TRAIL_ANGLE_DEG, alpha);

        batch.begin();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(enemyTexture, x, y, enemyWidth / 2f, enemyHeight / 2f, enemyWidth, enemyHeight,
                1f, 1f, ENEMY_ROTATION_DEG, 0, 0, enemyTexture.getWidth(), enemyTexture.getHeight(), false, false);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    // ---------------- ENGINE FIRE ----------------

    private static void drawEngineFire(SpriteBatch batch, Texture fireTexture, float anchorX, float anchorY,
                                        float flameWidth, float trailAngleDeg, float alpha) {
        float flameHeight = flameWidth * (fireTexture.getHeight() / (float) fireTexture.getWidth());
        float x = anchorX - flameWidth;
        float y = anchorY - flameHeight / 2f;
        float rotation = trailAngleDeg + 180f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.begin();
        batch.setColor(1f, 1f, 1f, 0.9f * alpha);
        batch.draw(fireTexture, x, y, flameWidth, flameHeight / 2f, flameWidth, flameHeight,
                1f, 1f, rotation, 0, 0, fireTexture.getWidth(), fireTexture.getHeight(), false, false);
        batch.setColor(Color.WHITE);
        batch.end();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
