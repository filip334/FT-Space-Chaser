/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;

/**
 *
 * @author Todorovic
 */
public class GameWorld {
    private final Game game;

    // ENTITIES
    private final Player player;
    private final Array<Bullet> bullets = new Array<>();
    private final Array<Enemy> enemies = new Array<>();
    //private final EncounterField encounterField;

    // RENDER
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    //private final HudRenderer hudRenderer = new HudRenderer();

    // SYSTEMS
    //private final CollisionSystem collisionSystem = new CollisionSystem();

    // GAME STATE
    private float gameTime = 0f;
    private int score = 0;

    // RESOURCES
    private final Texture bulletTexture;

    // CONFIG
    private static final int ENEMY_WAVE_SIZE = 3;

    public GameWorld(Game game) {

        this.game = game;

        bulletTexture = new Texture("Fighter/Charge_2.png");

        player = new Player(100, 100);
        //encounterField = new EncounterField();

        spawnEnemyWave();
    }

    // ---------------- UPDATE ----------------

    public void update(float delta) {

        gameTime += delta;
        //encounterField.update();

        updatePlayer(delta);
        //updateBullets(delta);
        //updateEnemies(delta);

        //handleShooting();

        //score += collisionSystem.checkCollisions(player, bullets, enemies, encounterField.getWalls());

        handleGameOver();
        //cleanupDeadEntities();

        if (enemies.size == 0 && !player.isDead()) {
            spawnEnemyWave();
        }
    }

    // ---------------- UPDATE PARTS ----------------

    private void updatePlayer(float delta) {
        player.update(delta);
    }

    /*private void updateBullets(float delta) {
        for (Bullet bullet : bullets) {
            bullet.update(delta);
        }
    }*/

    /*private void updateEnemies(float delta) {
        for (Enemy enemy : enemies) {
            enemy.update(delta, player);
        }

        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).applySeparation(enemies, delta);
        }
    }*/

    // ---------------- SHOOTING ----------------

    /*private void handleShooting() {

        if (!player.wantsToShoot()) return;

        float rot = (float) Math.toRadians(player.getRotation());

        float forwardX = (float) Math.cos(rot);
        float forwardY = (float) Math.sin(rot);

        float rightX = -(float) Math.sin(rot);
        float rightY = (float) Math.cos(rot);

        float centerX = player.getX() + player.getWidth() / 2f;
        float centerY = player.getY() + player.getHeight() / 2f;

        float wingOffset = player.getHeight() * 0.25f;
        float forwardOffset = player.getWidth() * 0.38f;

        spawnBullet(centerX, centerY, forwardX, forwardY, rightX, rightY, wingOffset, forwardOffset);
    }*/

    /*private void spawnBullet(float cx, float cy,
                             float fx, float fy,
                             float rx, float ry,
                             float wingOffset,
                             float forwardOffset) {

        float leftX = cx - rx * wingOffset + fx * forwardOffset;
        float leftY = cy - ry * wingOffset + fy * forwardOffset;

        float rightX = cx + rx * wingOffset + fx * forwardOffset;
        float rightY = cy + ry * wingOffset + fy * forwardOffset;

        bullets.add(new Bullet(leftX, leftY, fx, fy, bulletTexture));
        bullets.add(new Bullet(rightX, rightY, fx, fy, bulletTexture));
    }*/

    // ---------------- CLEANUP ----------------

    /*private void cleanupDeadEntities() {

        for (int i = bullets.size - 1; i >= 0; i--) {
            if (bullets.get(i).isDead()) {
                bullets.removeIndex(i);
            }
        }

        for (int i = enemies.size - 1; i >= 0; i--) {
            if (enemies.get(i).isDead()) {
                enemies.get(i).dispose();
                enemies.removeIndex(i);
            }
        }
    }*/

    // ---------------- GAME OVER ----------------

    private void handleGameOver() {
        if (player.isDead()) {
            //game.setScreen(new MainMenuScreen(game));
        }
    }

    // ---------------- RENDER ----------------

    public void render(SpriteBatch batch) {

        player.render(batch);

       // for (Bullet b : bullets) b.render(batch);
        //for (Enemy e : enemies) e.render(batch);
    }

    public void renderShapes() {

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        //encounterField.render(shapeRenderer);
        shapeRenderer.end();
    }

    /*public void renderHitboxes() {

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        player.getHitbox().render(shapeRenderer, Color.BLUE);

        for (Bullet b : bullets) {
            b.getHitbox().render(shapeRenderer, Color.RED);
        }

        for (Enemy e : enemies) {
            e.getHitbox().render(shapeRenderer, Color.RED);
        }

        for (io.github.filip334.spacechaser.entity.Wall wall : encounterField.getWalls()) {
            wall.getHitbox().render(shapeRenderer, Color.LIGHT_GRAY);
        }

        shapeRenderer.end();
    }*/

    /*public void renderHud(SpriteBatch batch) {

        hudRenderer.render(
                batch,
                player.getHealth().getHealth(),
                player.getHealth().getMaxHealth(),
                player.getFuel().getFuel(),
                player.getFuel().getMaxFuel(),
                score,
                gameTime
        );
    }*/

    // ---------------- SPAWN ----------------

    private void spawnEnemyWave() {

        for (int i = 0; i < ENEMY_WAVE_SIZE; i++) {

            float x = MathUtils.random(80f, Gdx.graphics.getWidth() - 80f);
            float y = MathUtils.random(80f, Gdx.graphics.getHeight() - 80f);

            //enemies.add(new Enemy(x, y));
        }
    }

    // ---------------- DISPOSE ----------------

    public void dispose() {

        player.dispose();
        shapeRenderer.dispose();
        bulletTexture.dispose();

        //for (Bullet b : bullets) b.dispose();
        //for (Enemy e : enemies) e.dispose();

        //hudRenderer.dispose();
    }
}
