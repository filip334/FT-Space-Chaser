package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.collision.CollisionSystem;
import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Coin;
import io.github.filip334.spacechaser.entity.Wall;
import io.github.filip334.spacechaser.renderer.EntityRenderer;
import io.github.filip334.spacechaser.renderer.HudRenderer;
import io.github.filip334.spacechaser.settings.GameSettings;

import java.util.LinkedHashMap;
import java.util.Map;

public class GameWorld {

    private final Game game;

    private final Map<Integer, Player> players = new LinkedHashMap<>();
    private static final int LOCAL_PLAYER_ID = 0;
    private static final int HOST_PLAYER_ID = 1;
    private static final float PLAYER_SPAWN_MARGIN = 80f;
    private static final float PLAYER_SPAWN_SIDE_OFFSET = 150f;

    private final Array<Bullet> bullets = new Array<>();
    private final Array<Enemy> enemies = new Array<>();
    private final Array<Coin> coins = new Array<>();
    private final EncounterField encounterField;
    private final WaveManager waveManager;
    private final CoinSpawner coinSpawner;

    private final EntityRenderer entityRenderer;
    private final ShapeRenderer shapeRenderer;
    private final HudRenderer hudRenderer;

    private final CollisionSystem collisionSystem = new CollisionSystem();

    private float gameTime = 0f;
    private int score = 0;
    private float shootCooldown = 0f;
    private static final float SHOOT_INTERVAL = 0.20f;

    // ---------------- KONSTRUKTORI ----------------

    public GameWorld() {
        this.game = null;
        this.entityRenderer = null;
        this.shapeRenderer = null;
        this.hudRenderer = null;

        encounterField = new EncounterField();
        waveManager = new WaveManager(encounterField);
        coinSpawner = new CoinSpawner(encounterField);
        coinSpawner.spawnCoins(coins);
    }

    public GameWorld(Game game, GameSettings settings) {
        this.game = game;

        Texture playerIdleTexture = new Texture("Original/shipPixel.png");
        Texture flameSheet = new Texture("Original/moveFire.png");
        Texture enemyTexture = new Texture("Original/rocketPixel.png");
        Texture bulletTexture = new Texture("Original/bullet.png");

        entityRenderer = new EntityRenderer(playerIdleTexture, flameSheet, enemyTexture, bulletTexture);
        shapeRenderer = new ShapeRenderer();
        hudRenderer = new HudRenderer();

        encounterField = new EncounterField();
        float spawnX = encounterField.getFieldX() + encounterField.getFieldWidth() / 2f;
        float spawnY = encounterField.getFieldY() + PLAYER_SPAWN_MARGIN;
        players.put(LOCAL_PLAYER_ID, new Player(spawnX, spawnY, settings, true));
        waveManager = new WaveManager(encounterField);
        coinSpawner = new CoinSpawner(encounterField);

        coinSpawner.spawnCoins(coins);
    }

    // ---------------- UPDATE ----------------

    public void update(float delta) {

        gameTime += delta;
        encounterField.update();

        if (entityRenderer != null) {
            entityRenderer.update(delta);
        }

        updatePlayers(delta);
        updateEnemies(delta);
        handleShooting(delta);
        updateBullets(delta);

        score += collisionSystem.checkWorldCollisions(bullets, enemies, encounterField.getWalls(), players);

        for (Player p : players.values()) {
            int contactKillScore = collisionSystem.checkPlayerCollisions(p, enemies, encounterField.getWalls());
            p.addScore(contactKillScore);
            score += contactKillScore;

            int coinsGained = collisionSystem.collectCoins(p, coins);
            p.addScore(coinsGained);
            score += coinsGained;
        }

        if (coins.size == 0) {
            coinSpawner.spawnCoins(coins);
        }

        cleanupDeadEntities();

        waveManager.update(delta, enemies, allPlayersDead());
    }

    // ---------------- UPDATE PARTS ----------------

    private void updatePlayers(float delta) {
        for (Map.Entry<Integer, Player> entry : players.entrySet()) {
            int id = entry.getKey();
            Player p = entry.getValue();

            boolean isLocalKeyboardPlayer = (game != null) && (id == LOCAL_PLAYER_ID);

            if (isLocalKeyboardPlayer) {
                p.update(delta);
            } else {
                p.updateNetworked(delta);
            }
        }
    }

    private void updateEnemies(float delta) {
        Player target = getPrimaryTarget();
        waveManager.updateEnrage(enemies);
        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).update(delta, target);
            enemies.get(i).applySeparation(enemies, delta);
        }
    }

    private void updateBullets(float delta) {
        for (Bullet b : bullets) {
            b.update(delta);
        }
    }

    private Player getPrimaryTarget() {
        for (Player p : players.values()) {
            if (!p.isDead()) return p;
        }
        return players.get(LOCAL_PLAYER_ID);
    }

    private boolean allPlayersDead() {
        for (Player p : players.values()) {
            if (!p.isDead()) return false;
        }
        return true;
    }

    public boolean isMatchOver() {
        return !players.isEmpty() && allPlayersDead();
    }

    // ---------------- RENDER ----------------

    public void render(SpriteBatch batch) {
        if (entityRenderer == null) return;

        for (Player p : players.values()) {
            entityRenderer.render(batch, p);
        }

        for (Bullet b : bullets) entityRenderer.render(batch, b);
        for (Enemy e : enemies) entityRenderer.render(batch, e);
    }

    public void renderShapes(float offsetX, Matrix4 projectionMatrix) {
        if (shapeRenderer == null) return;

        shapeRenderer.setProjectionMatrix(projectionMatrix);
        shapeRenderer.setTransformMatrix(new Matrix4().setToTranslation(offsetX, 0f, 0f));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        encounterField.render(shapeRenderer);
        for (Coin coin : coins) {
            shapeRenderer.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
            shapeRenderer.rect(coin.getX() - Coin.RADIUS, coin.getY() - Coin.RADIUS, Coin.RADIUS * 2f, Coin.RADIUS * 2f);
        }
        shapeRenderer.end();
        shapeRenderer.setTransformMatrix(new Matrix4());
    }

    public void renderHitboxes(float offsetX, Matrix4 projectionMatrix) {
        if (shapeRenderer == null) return;

        shapeRenderer.setProjectionMatrix(projectionMatrix);
        shapeRenderer.setTransformMatrix(new Matrix4().setToTranslation(offsetX, 0f, 0f));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (Player p : players.values()) p.hitBoxDebugRenderer(shapeRenderer);
        for (Enemy e : enemies) e.hitBoxDebugRenderer(shapeRenderer);
        for (Bullet b : bullets) b.hitBoxDebugRenderer(shapeRenderer);
        shapeRenderer.end();
        shapeRenderer.setTransformMatrix(new Matrix4());
    }

    public void renderHud(SpriteBatch batch, Matrix4 projectionMatrix) {
        if (hudRenderer == null) return;

        Player local = players.get(LOCAL_PLAYER_ID);
        if (local == null) return;

        hudRenderer.setProjectionMatrix(projectionMatrix);
        hudRenderer.render(batch, local.getHealth(), local.getMaxHealth(),
                local.getFuel(), local.getMaxFuel(), score, gameTime, waveManager.getWaveNumber());

        if (waveManager.isWaveCountingDown()) {
            hudRenderer.renderCountdown(batch, (int) Math.ceil(waveManager.getWaveCountdownSeconds()));
        }
    }

    // ---------------- WAVE / SPAWN (delegira WaveManager/CoinSpawner) ----------------

    public boolean isWaveCountingDown() {
        return waveManager.isWaveCountingDown();
    }

    public float getWaveCountdownSeconds() {
        return waveManager.getWaveCountdownSeconds();
    }

    public int getWaveNumber() {
        return waveManager.getWaveNumber();
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
                enemies.removeIndex(i);
            }
        }
    }

    // ---------------- HANDLE ----------------

    private void handleShooting(float delta) {
        if (shootCooldown > 0f) {
            shootCooldown -= delta;
        }

        for (Map.Entry<Integer, Player> entry : players.entrySet()) {
            Player p = entry.getValue();
            if (p.isDead()) continue;
            if (!p.wantsToShoot()) continue;
            if (shootCooldown > 0f) continue;

            spawnBulletFor(entry.getKey(), p);
            shootCooldown = SHOOT_INTERVAL;
        }
    }

    private void spawnBulletFor(int ownerId, Player p) {
        float rot = (float) Math.toRadians(p.getRotation());

        float cos = (float) Math.cos(rot);
        float sin = (float) Math.sin(rot);

        float localNoseX = 30f;
        float localNoseY = 0f;

        float noseX = p.getX() + localNoseX * cos - localNoseY * sin;
        float noseY = p.getY() + localNoseX * sin + localNoseY * cos;

        Bullet bullet = new Bullet(noseX, noseY, cos, sin, p.getRotation());
        bullet.setOwnerId(ownerId);
        bullets.add(bullet);
    }

    // ---------------- DISPOSE ----------------

    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (entityRenderer != null) entityRenderer.dispose();
        if (hudRenderer != null) hudRenderer.dispose();
    }

    // ---------------- MULTIPLAYER PLAYER MANAGEMENT ----------------

    public void spawnPlayer(int playerId) {
        if (players.containsKey(playerId)) return;

        float centerX = encounterField.getFieldX() + encounterField.getFieldWidth() / 2f;
        float bottomY = encounterField.getFieldY() + PLAYER_SPAWN_MARGIN;
        float x = (playerId == HOST_PLAYER_ID) ? centerX - PLAYER_SPAWN_SIDE_OFFSET : centerX + PLAYER_SPAWN_SIDE_OFFSET;

        players.put(playerId, new Player(x, bottomY, new GameSettings(), false));
    }

    public void removePlayer(int playerId) {
        players.remove(playerId);
    }

    public Player getPlayerById(int playerId) {
        return players.get(playerId);
    }

    // ---------------- GET / SET ----------------

    public Map<Integer, Player> getPlayersMap() {
        return players;
    }

    public Array<Enemy> getEnemies() {
        return enemies;
    }

    public Array<Bullet> getBullets() {
        return bullets;
    }

    public Array<Coin> getCoins() {
        return coins;
    }

    public Array<Wall> getWalls() {
        return encounterField.getWalls();
    }

    public int getScore() {
        return score;
    }

    public float getGameTime() {
        return gameTime;
    }
}
