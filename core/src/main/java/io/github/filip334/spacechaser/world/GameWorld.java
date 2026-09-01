package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.collision.CollisionSystem;
import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Wall;
import io.github.filip334.spacechaser.renderer.EntityRenderer;
import io.github.filip334.spacechaser.renderer.HudRenderer;
import io.github.filip334.spacechaser.screen.MainMenuScreen;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameWorld {

    // GAME
    private final Game game;

    // ENTITIES
    // VISE igraca po ID-ju (id 0 = lokalni igrac u singleplayeru)
    private final Map<Integer, Player> players = new LinkedHashMap<>();
    private static final int LOCAL_PLAYER_ID = 0;

    private final Array<Bullet> bullets = new Array<>();
    private final Array<Enemy> enemies = new Array<>();
    private final EncounterField encounterField;

    // RENDER (mogu biti null ako je GameWorld napravljen bez Game-a, npr. na serveru)
    private final EntityRenderer entityRenderer;
    private final ShapeRenderer shapeRenderer;
    private final HudRenderer hudRenderer;

    // SYSTEMS
    private final CollisionSystem collisionSystem = new CollisionSystem();

    // GAME STATE
    private float gameTime = 0f;
    private int score = 0;
    private float shootCooldown = 0f;
    private static final float SHOOT_INTERVAL = 0.20f;

    // CONFIG
    private static final int ENEMY_WAVE_SIZE = 3;

    // ---------------- CONSTRUCTORS ----------------

    /**
     * Bez-arg konstruktor - NE pravi Texture/ShapeRenderer objekte (ne zahteva GL kontekst).
     * Koristi se na serveru (headless) i u testovima.
     */
    public GameWorld() {
        this.game = null;
        this.entityRenderer = null;
        this.shapeRenderer = null;
        this.hudRenderer = null;

        // NE dodajemo default igraca ovde - server ovaj konstruktor koristi
        // i igraci se dodaju dinamicki preko spawnPlayer(id) kad se konektuju.
        encounterField = new EncounterField();
    }

    /**
     * Singleplayer konstruktor sa renderovanjem (klijent).
     */
    public GameWorld(Game game) {
        this.game = game;

        Texture playerIdleTexture = new Texture("Original/ship.png");
        Texture playerThrustSheet = new Texture("Original/shipMove.png");
        Texture flameSheet = new Texture("Original/moveFire.png");
        Texture enemyTexture = new Texture("Original/rocket.png");
        Texture bulletTexture = new Texture("Original/bullet.png");

        entityRenderer = new EntityRenderer(playerIdleTexture, playerThrustSheet, flameSheet, enemyTexture, bulletTexture);
        shapeRenderer = new ShapeRenderer();
        hudRenderer = new HudRenderer();

        players.put(LOCAL_PLAYER_ID, new Player(100, 100));
        encounterField = new EncounterField();

        spawnEnemyWave();
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

        for (Player p : players.values()) {
            score += collisionSystem.checkCollisions(p, bullets, enemies, encounterField.getWalls());
        }

        handleGameOver();
        cleanupDeadEntities();

        if (enemies.size == 0 && !allPlayersDead()) {
            spawnEnemyWave();
        }
    }

    // ---------------- UPDATE PARTS ----------------

    private void updatePlayers(float delta) {
        for (Map.Entry<Integer, Player> entry : players.entrySet()) {
            int id = entry.getKey();
            Player p = entry.getValue();

            // Lokalni igrac na klijentu (singleplayer/host) cita tastaturu.
            // Svi ostali (mrezni igraci na klijentu, SVI igraci na serveru)
            // koriste input koji je vec postavljen preko applyInput().
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

    /**
     * Enemy.update() trenutno prima jednog Player-a kao metu.
     * Dok ne prosirimo Enemy da bira izmedju vise igraca, gadja prvog zivog.
     * TODO: prosiriti Enemy/StateMachine da bira najblizeg/bilo kog zivog igraca.
     */
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

    /**
     * Mec je gotov kad su SVI konektovani igraci mrtvi.
     * Prazna mapa (niko jos nije spawn-ovan) se NE racuna kao gotov mec.
     */
    public boolean isMatchOver() {
        return !players.isEmpty() && allPlayersDead();
    }

    // ---------------- GAME OVER ----------------

    private void handleGameOver() {
        // game je null na serveru (headless) - server ne menja ekrane, samo simulira
        if (game == null) return;

        Player local = players.get(LOCAL_PLAYER_ID);
        if (local != null && local.isDead()) {
            game.setScreen(new MainMenuScreen(game));
        }
    }

    // ---------------- RENDER ----------------

    public void render(SpriteBatch batch) {
        if (entityRenderer == null) return; // headless world (server) - nema sta da se crta

        for (Player p : players.values()) {
            entityRenderer.render(batch, p);
        }

        for (Bullet b : bullets) entityRenderer.render(batch, b);
        for (Enemy e : enemies) entityRenderer.render(batch, e);
    }

    public void renderShapes() {
        if (shapeRenderer == null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        encounterField.render(shapeRenderer);
        shapeRenderer.end();
    }

    public void renderHitboxes() {
        if (shapeRenderer == null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (Player p : players.values()) p.hitBoxDebugRenderer(shapeRenderer);
        for (Enemy e : enemies) e.hitBoxDebugRenderer(shapeRenderer);
        for (Bullet b : bullets) b.hitBoxDebugRenderer(shapeRenderer);
        shapeRenderer.end();
    }

    public void renderHud(SpriteBatch batch) {
        if (hudRenderer == null) return;

        Player local = players.get(LOCAL_PLAYER_ID);
        if (local == null) return;

        hudRenderer.render(batch, local.getHealth(), local.getMaxHealth(),
                local.getFuel(), local.getMaxFuel(), score, gameTime);
    }

    // ---------------- SPAWN ----------------

    private void spawnEnemyWave() {
        float margin = 80f;
        float minX = encounterField.getFieldX() + margin;
        float maxX = encounterField.getFieldX() + encounterField.getFieldWidth() - margin;
        float minY = encounterField.getFieldY() + margin;
        float maxY = encounterField.getFieldY() + encounterField.getFieldHeight() - margin;

        for (int i = 0; i < ENEMY_WAVE_SIZE; i++) {
            float x = MathUtils.random(minX, maxX);
            float y = MathUtils.random(minY, maxY);
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
                enemies.removeIndex(i);
            }
        }
    }

    // ---------------- HANDLE ----------------

    private void handleShooting(float delta) {
        if (shootCooldown > 0f) {
            shootCooldown -= delta;
        }

        for (Player p : players.values()) {
            if (p.isDead()) continue;
            if (!p.wantsToShoot()) continue;
            if (shootCooldown > 0f) continue;

            spawnBulletFor(p);
            shootCooldown = SHOOT_INTERVAL;
        }
    }

    private void spawnBulletFor(Player p) {
        float rot = (float) Math.toRadians(p.getRotation());

        float cos = (float) Math.cos(rot);
        float sin = (float) Math.sin(rot);

        // Lokalna pozicija vrha nosa u odnosu na centar broda
        float localNoseX = 50f;
        float localNoseY = 0f;

        float noseX = p.getX() + localNoseX * cos - localNoseY * sin;
        float noseY = p.getY() + localNoseX * sin + localNoseY * cos;

        bullets.add(new Bullet(noseX, noseY, cos, sin, p.getRotation()));
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

        float margin = 80f;
        float minX = encounterField.getFieldX() + margin;
        float maxX = encounterField.getFieldX() + encounterField.getFieldWidth() - margin;
        float minY = encounterField.getFieldY() + margin;
        float maxY = encounterField.getFieldY() + encounterField.getFieldHeight() - margin;

        float x = MathUtils.random(minX, maxX);
        float y = MathUtils.random(minY, maxY);

        players.put(playerId, new Player(x, y));
    }

    public void removePlayer(int playerId) {
        players.remove(playerId);
    }

    public Player getPlayerById(int playerId) {
        return players.get(playerId);
    }

    public Collection<Player> getPlayers() {
        return players.values();
    }

    public Map<Integer, Player> getPlayersMap() {
        return players;
    }

    public Array<Enemy> getEnemies() {
        return enemies;
    }

    public Array<Bullet> getBullets() {
        return bullets;
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