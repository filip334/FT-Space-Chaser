package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.collision.CollisionSystem;
import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.renderer.HudRenderer;
import io.github.filip334.spacechaser.screen.MainMenuScreen;

public class GameWorld {
    
    // GAME
    private final Game game;

    // ENTITIES
    private final Player player;
    private final Array<Bullet> bullets = new Array<>();
    private final Array<Enemy> enemies = new Array<>();
    private final EncounterField encounterField;

    // RENDER
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final HudRenderer hudRenderer = new HudRenderer();

    // SYSTEMS
   private final CollisionSystem collisionSystem = new CollisionSystem();

    // GAME STATE
    private float gameTime = 0f;
    private int score = 0;
    private float shootCooldown = 0f;
    private static final float SHOOT_INTERVAL = 0.20f;

    // RESOURCES
    private final Texture bulletTexture;

    // CONFIG
    private static final int ENEMY_WAVE_SIZE = 3;

    //
    public GameWorld(Game game) {

        this.game = game;

        bulletTexture = new Texture("Original/bullet.png");

        player = new Player(100, 100);
        encounterField = new EncounterField();

        spawnEnemyWave();
    }

    // ---------------- UPDATE ----------------

    public void update(float delta) {

        gameTime += delta;
        encounterField.update();

        updatePlayer(delta);
        updateEnemies(delta);

        handleShooting();

        updateBullets(delta);

        score += collisionSystem.checkCollisions(player, bullets, enemies, encounterField.getWalls());

        handleGameOver();
        cleanupDeadEntities();

        if (enemies.size == 0 && !player.isDead()) {
            spawnEnemyWave();
        }
        if (shootCooldown > 0f) {
            shootCooldown -= delta;
        }
    }

    // ---------------- UPDATE PARTS ----------------

    private void updatePlayer(float delta) {
        player.update(delta);
    }
    private void updateEnemies(float delta) {
         for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).update(delta, player);
            enemies.get(i).applySeparation(enemies, delta);
        }
    }
    private void updateBullets(float delta){
        for(Bullet b : bullets){
            b.update(delta);
        }
    }
    

    // ---------------- GAME OVER ----------------

    private void handleGameOver() {
        if (player.isDead()) {
            game.setScreen(new MainMenuScreen(game));
        }
    }

    // ---------------- RENDER ----------------

    public void render(SpriteBatch batch) {
        player.render(batch);
        for (Bullet b : bullets) b.render(batch);
        for (Enemy e : enemies) e.render(batch);
        
    }

    public void renderShapes() {

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        encounterField.render(shapeRenderer);
        shapeRenderer.end();
    }

    
    public void renderHitboxes(){
         shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        player.debugRender(shapeRenderer);
        for(Enemy e : enemies) e.debugRender(shapeRenderer);
        for(Bullet b : bullets) b.debugRender(shapeRenderer);
        shapeRenderer.end();
    }
    
    public void renderHud(SpriteBatch batch){
        hudRenderer.render(batch, player.getHealth(), player.getMaxHealth(), player.getFuel(), player.getMaxFuel(), score, gameTime);
    }
    // ---------------- SPAWN ----------------

    private void spawnEnemyWave() {

        for (int i = 0; i < ENEMY_WAVE_SIZE; i++) {

            float x = MathUtils.random(80f, Gdx.graphics.getWidth() - 80f);
            float y = MathUtils.random(80f, Gdx.graphics.getHeight() - 80f);

            enemies.add(new Enemy(x, y));
        }
    }
    
    // ---------------- CLEAN UP ----------------
    private void cleanupDeadEntities() {

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
    }
    
    // ---------------- HANDLE ----------------
   
    private void handleShooting() {

        if (!player.wantsToShoot()) return;

         if (shootCooldown > 0f) {
            return;
        }
        
        float rot = (float) Math.toRadians(player.getRotation());

        float cos = (float) Math.cos(rot);
        float sin = (float) Math.sin(rot);

        // Lokalna pozicija vrha nosa u odnosu na centar broda
        float localNoseX = 50f;
        float localNoseY = 0f;

        // Rotacija lokalne tačke u world space
        float noseX = player.getX()
                + localNoseX * cos
                - localNoseY * sin;

        float noseY = player.getY()
                + localNoseX * sin
                + localNoseY * cos;

        bullets.add(
            new Bullet(
                noseX,
                noseY,
                cos,
                sin,
                bulletTexture
            )
        );
        shootCooldown = SHOOT_INTERVAL;
    }
    // ---------------- DISPOSE ----------------

    public void dispose() {

        player.dispose();
        shapeRenderer.dispose();
        bulletTexture.dispose();

        for (Bullet b : bullets) b.dispose();
        for (Enemy e : enemies) e.dispose();

        hudRenderer.dispose();
    }
}
