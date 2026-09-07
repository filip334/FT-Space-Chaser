package io.github.filip334.spacechaser.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Deljena "brod kog juri raketa" dekorativna scena - koristi je Main Menu i
 * Multiplayer ekran (MenuAssets/shipBackground1.png, MenuAssets/enemy.png,
 * MenuAssets/engineFireMainMenu.png).
 * <p>
 * Uglovi ispod NISU proizvoljni - izmereni su analizom piksela samih slika
 * (najudaljenija tacka od centra/PCA osa svakog motora posebno). Ne menjati
 * bez ponovne analize ako se slike promene.
 */
public final class ChaseArt {

    // Javno dostupno da bi ekrani mogli da racunaju pozicije "iza broda" duz
    // istog pravca (npr. gde da stavi neprijatelja u odnosu na brod).
    public static final float TRAIL_ANGLE_DEG = 42f; // pravac repa/kretanja cele siluete broda
    private static final float SHIP_TRAVEL_ANGLE_DEG = TRAIL_ANGLE_DEG + 180f; // pravac nosa
    // Ugao SVAKOG pojedinacnog motora broda (gornji je nagnut plice od donjeg).
    private static final float FLAME_ANGLE_TOP_DEG = 27f;
    private static final float FLAME_ANGLE_BOTTOM_DEG = 30f;
    // enemy.png je nacrtan da nos gleda pravo gore (90 stepeni).
    private static final float ENEMY_TEXTURE_NOSE_DEG = 90f;
    private static final float ENEMY_ROTATION_DEG = SHIP_TRAVEL_ANGLE_DEG - ENEMY_TEXTURE_NOSE_DEG;

    private ChaseArt() {
    }

    /** Igracev brod (uvek prirodan ugao slike, bez rotacije) sa dva plamena iz motora. */
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
        // blagi cyan glow iza broda
        batch.setColor(0f, 0.9f, 1f, 0.10f * alpha);
        batch.draw(shipTexture, x, y, shipWidth / 2f, shipHeight / 2f, shipWidth, shipHeight,
                1f, 1f, 0f, 0, 0, shipTexture.getWidth(), shipTexture.getHeight(), false, false);

        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(shipTexture, x, y, shipWidth / 2f, shipHeight / 2f, shipWidth, shipHeight,
                1f, 1f, 0f, 0, 0, shipTexture.getWidth(), shipTexture.getHeight(), false, false);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Neprijatelj rotiran u pravcu kretanja (kao da juri) sa jednim plamenom iz motora. */
    public static void drawEnemy(SpriteBatch batch, Texture enemyTexture, Texture fireTexture,
                                  float centerX, float centerY, float enemyHeight, float alpha) {
        float enemyWidth = enemyHeight * (enemyTexture.getWidth() / (float) enemyTexture.getHeight());
        float x = centerX - enemyWidth / 2f;
        float y = centerY - enemyHeight / 2f;

        // Motor je pri dnu (necentrirano) enemy.png teksture pre rotacije -
        // pretvori taj lokalni pomeraj u svetske koordinate NAKON rotacije tela.
        float localOffsetY = -enemyHeight * 0.38f;
        float rad = (float) Math.toRadians(ENEMY_ROTATION_DEG);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float flameX = centerX - localOffsetY * sin;
        float flameY = centerY + localOffsetY * cos;

        drawEngineFire(batch, fireTexture, flameX, flameY, enemyWidth * 0.95f, TRAIL_ANGLE_DEG, alpha);

        batch.begin();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(enemyTexture, x, y, enemyWidth / 2f, enemyHeight / 2f, enemyWidth, enemyHeight,
                1f, 1f, ENEMY_ROTATION_DEG, 0, 0, enemyTexture.getWidth(), enemyTexture.getHeight(), false, false);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * Crta plamen motora tako da mu je svetao vrh na (anchorX, anchorY) i
     * proteze se unazad u trailAngleDeg pravcu (iza broda/neprijatelja).
     */
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
