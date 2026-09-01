package io.github.filip334.spacechaser.renderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.filip334.spacechaser.animation.AnimationController;
import io.github.filip334.spacechaser.entity.*;

import java.util.HashMap;
import java.util.Map;

public class EntityRenderer {

    private static final float FRAME_DURATION = 1f / 12f; // 12 fps - podesi po zelji

    // Flame sprite je nacrtan da podrazumevano "gleda" na gore -
    // ovaj offset ga zarotira da izgleda kao da izbija iz motora unazad.
    // Ako posle testa izgleda pod pogresnim uglom, promeni znak (+90 <-> -90).
    private static final float FLAME_ROTATION_OFFSET = -90f;
    private static final float FLAME_WIDTH = 36f;
    private static final float FLAME_HEIGHT = 60f;

    private final Texture playerIdleTexture;
    private final Texture playerThrustSheet;
    private final Texture flameSheet;

    private final Texture enemyTexture;
    private final Texture bulletTexture;

    // Tvoj AnimationController prati vreme po instanci - svaki igrac
    // dobija svoj kontroler (za telo broda i posebno za plamen motora).
    private final Map<Player, AnimationController> shipAnimations = new HashMap<>();
    private final Map<Player, AnimationController> flameAnimations = new HashMap<>();

    public EntityRenderer(Texture playerIdleTexture, Texture playerThrustSheet, Texture flameSheet,
                           Texture enemyTexture, Texture bulletTexture) {

        this.playerIdleTexture = playerIdleTexture;
        this.playerThrustSheet = playerThrustSheet;
        this.flameSheet = flameSheet;
        this.enemyTexture = enemyTexture;
        this.bulletTexture = bulletTexture;
    }

    /**
     * Poziva se jednom po frejmu da sve aktivne animacije odmaknu (iz GameWorld/MultiplayerGameWorld).
     */
    public void update(float delta) {
        for (AnimationController controller : shipAnimations.values()) {
            controller.update(delta);
        }
        for (AnimationController controller : flameAnimations.values()) {
            controller.update(delta);
        }
    }

    public void render(SpriteBatch batch, Entity entity){
        if (entity instanceof Player) {
            Player player = (Player)entity;
            if (player.isDead()) {
                return; // eliminisan - brod se vise ne prikazuje na tabli
            }
            renderPlayer(batch, player);
        }
        if (entity instanceof Enemy) {
            Enemy enemy = (Enemy)entity;
            renderEnemy(batch, enemy);
        }
        if (entity instanceof Bullet) {
            Bullet bullet = (Bullet)entity;
            renderBullet(batch, bullet);
        }
    }

    private void renderPlayer(SpriteBatch batch, Player player){
        AnimationController shipAnim = getShipAnimation(player);

        if (player.isThrusting()) {
            shipAnim.playLoop("thrust");
        } else {
            shipAnim.playLoop("idle");
        }

        TextureRegion frame = shipAnim.getFrame();

        batch.draw(
            frame,
            player.getX() - player.getWidth() / 2f,
            player.getY() - player.getHeight() / 2f,
            player.getWidth() / 2f,
            player.getHeight() / 2f,
            player.getWidth(),
            player.getHeight(),
            1f,
            1f,
            player.getRotation()
        );

        if (player.isBoosting()) {
            AnimationController flameAnim = getFlameAnimation(player);
            flameAnim.playLoop("flame");

            TextureRegion flameFrame = flameAnim.getFrame();
            float engineOffsetY = player.getHeight() * 0.16f;

            drawEngineFlame(batch, flameFrame, player, engineOffsetY);
            drawEngineFlame(batch, flameFrame, player, -engineOffsetY);
        }
    }

    private AnimationController getShipAnimation(Player player) {
        AnimationController controller = shipAnimations.get(player);
        if (controller == null) {
            controller = new AnimationController();

            // jedan staticni frejm - cela idle tekstura
            controller.add("idle", playerIdleTexture, 1,
                    playerIdleTexture.getWidth(), playerIdleTexture.getHeight(), 1f);

            // shipMove.png je grid 2 kolone x 4 reda = 8 frejmova od 500x500
            controller.add("thrust", playerThrustSheet, 8,
                    playerThrustSheet.getWidth(), playerThrustSheet.getHeight() / 8, FRAME_DURATION);

            controller.playLoop("idle");
            shipAnimations.put(player, controller);
        }
        return controller;
    }

    private AnimationController getFlameAnimation(Player player) {
        AnimationController controller = flameAnimations.get(player);
        if (controller == null) {
            controller = new AnimationController();

            // ship_flame.png je 8 frejmova u jednom redu, 40x88 svaki
            controller.add("flame", flameSheet, 8,
                    flameSheet.getWidth() / 8, flameSheet.getHeight(), FRAME_DURATION);

            controller.playLoop("flame");
            flameAnimations.put(player, controller);
        }
        return controller;
    }

    /**
     * Crta plamen na poziciji motora, rotiran zajedno sa brodom.
     * localOffsetY > 0 = gornji motor, < 0 = donji motor (u lokalnom sistemu broda).
     */
    private void drawEngineFlame(SpriteBatch batch, TextureRegion frame, Player player, float localOffsetY) {
        float rad = (float) Math.toRadians(player.getRotation());
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        // iza broda (suprotno od pravca "napred")
        float localOffsetX = -player.getWidth() * 0.32f;

        float worldX = player.getX() + localOffsetX * cos - localOffsetY * sin;
        float worldY = player.getY() + localOffsetX * sin + localOffsetY * cos;

        batch.draw(
            frame,
            worldX - FLAME_WIDTH / 2f,
            worldY - FLAME_HEIGHT / 2f,
            FLAME_WIDTH / 2f,
            FLAME_HEIGHT / 2f,
            FLAME_WIDTH,
            FLAME_HEIGHT,
            1f,
            1f,
            player.getRotation() + 180f + FLAME_ROTATION_OFFSET
        );
    }

    private void renderEnemy(SpriteBatch batch,Enemy enemy){
        batch.draw(
            enemyTexture,
            enemy.getX() - enemy.getWidth() / 2f,
            enemy.getY() - enemy.getHeight() / 2f,
            enemy.getWidth() / 2f,
            enemy.getHeight() / 2f,
            enemy.getWidth(),
            enemy.getHeight(),
            1f,
            1f,
            enemy.getRotation(),
            0,
            0,
            enemyTexture.getWidth(),
            enemyTexture.getHeight(),
            false,
            false
        );
    }

    private void renderBullet(SpriteBatch batch,Bullet bullet){
        batch.draw(
            bulletTexture,
            bullet.getX() - bullet.getWidth() / 2f,
            bullet.getY() - bullet.getHeight() / 2f,
            bullet.getWidth() / 2f,
            bullet.getHeight() / 2f,
            bullet.getWidth(),
            bullet.getHeight(),
            1f,
            1f,
            bullet.getRotation(),
            0,
            0,
            bulletTexture.getWidth(),
            bulletTexture.getHeight(),
            false,
            false
        );
    }

    public void dispose() {
        playerIdleTexture.dispose();
        playerThrustSheet.dispose();
        flameSheet.dispose();
        enemyTexture.dispose();
        bulletTexture.dispose();
    }
}