package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.collision.CollisionSystem;
import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Coin;
import io.github.filip334.spacechaser.entity.Wall;
import io.github.filip334.spacechaser.enemy.PathFinder;
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
    private final Array<Coin> coins = new Array<>();
    private final EncounterField encounterField;
    private final PathFinder enemyPathFinder;
    private static final float NAVIGATION_CELL_SIZE = 30f;

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
    private static final int SCORE_PICKUP_COUNT = 5;
    private int nextCoinId = 1;

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
        enemyPathFinder = createEnemyPathFinder();
        spawnCoins();
    }

    /**
     * Singleplayer konstruktor sa renderovanjem (klijent).
     */
    public GameWorld(Game game, GameSettings settings) {
        this.game = game;

        Texture playerIdleTexture = new Texture("Original/shipPixel.png");
        Texture flameSheet = new Texture("Original/moveFire.png");
        Texture enemyTexture = new Texture("Original/rocketPixel.png");
        Texture bulletTexture = new Texture("Original/bullet.png");

        entityRenderer = new EntityRenderer(playerIdleTexture, flameSheet, enemyTexture, bulletTexture);
        shapeRenderer = new ShapeRenderer();
        hudRenderer = new HudRenderer();

        players.put(LOCAL_PLAYER_ID, new Player(100, 100, settings, true));
        encounterField = new EncounterField();
        enemyPathFinder = createEnemyPathFinder();

        spawnEnemyWave();
        spawnCoins();
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
            score += collisionSystem.collectCoins(p, coins);
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

    public void renderShapes(float offsetX, Matrix4 projectionMatrix) {
        if (shapeRenderer == null) return;

        shapeRenderer.setProjectionMatrix(projectionMatrix);
        shapeRenderer.setTransformMatrix(new Matrix4().setToTranslation(offsetX, 0f, 0f));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        encounterField.render(shapeRenderer);
        for (Coin coin : coins) {
            shapeRenderer.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
            shapeRenderer.circle(coin.getX(), coin.getY(), Coin.RADIUS);
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
            enemies.add(createEnemyInsideMap(minX, maxX, minY, maxY));
        }
    }

    private Enemy createEnemyInsideMap(float minX, float maxX, float minY, float maxY) {
        for (int attempt = 0; attempt < 30; attempt++) {
            Enemy enemy = new Enemy(MathUtils.random(minX, maxX), MathUtils.random(minY, maxY));
            configureEnemyNavigation(enemy);
            if (enemy.isInNavigablePosition()) {
                return enemy;
            }
        }

        // Ova tacka je u otvorenom uglu mape i koristi se samo ako je RNG vise puta pogodio zid.
        Enemy fallback = new Enemy(minX, minY);
        configureEnemyNavigation(fallback);
        return fallback;
    }

    private void spawnCoins() {
        float margin = 80f;
        float minX = encounterField.getFieldX() + margin;
        float maxX = encounterField.getFieldX() + encounterField.getFieldWidth() - margin;
        float minY = encounterField.getFieldY() + margin;
        float maxY = encounterField.getFieldY() + encounterField.getFieldHeight() - margin;

        for (int i = 0; i < SCORE_PICKUP_COUNT; i++) {
            for (int attempt = 0; attempt < 30; attempt++) {
                float x = MathUtils.random(minX, maxX);
                float y = MathUtils.random(minY, maxY);
                if (isPickupPositionClear(x, y)) {
                    coins.add(new Coin(nextCoinId++, x, y));
                    break;
                }
            }
        }
    }

    private boolean isPickupPositionClear(float x, float y) {
        float clearance = Coin.RADIUS + 8f;
        for (Wall wall : encounterField.getWalls()) {
            float closestX = MathUtils.clamp(x, wall.getX(), wall.getX() + wall.getWidth());
            float closestY = MathUtils.clamp(y, wall.getY(), wall.getY() + wall.getHeight());
            float dx = x - closestX;
            float dy = y - closestY;
            if (dx * dx + dy * dy < clearance * clearance) return false;
        }
        return true;
    }

    private void configureEnemyNavigation(Enemy enemy) {
        enemy.setNavigation(encounterField.getWalls(), encounterField.getFieldX(), encounterField.getFieldY(),
                encounterField.getFieldWidth(), encounterField.getFieldHeight(), enemyPathFinder, NAVIGATION_CELL_SIZE);
    }

    private PathFinder createEnemyPathFinder() {
        int columns = (int) (encounterField.getFieldWidth() / NAVIGATION_CELL_SIZE);
        int rows = (int) (encounterField.getFieldHeight() / NAVIGATION_CELL_SIZE);
        boolean[][] walkable = new boolean[rows][columns];

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                float x = encounterField.getFieldX() + (column + 0.5f) * NAVIGATION_CELL_SIZE;
                float y = encounterField.getFieldY() + (row + 0.5f) * NAVIGATION_CELL_SIZE;
                walkable[row][column] = isNavigationCellClear(x, y);
            }
        }
        return new PathFinder(walkable);
    }

    private boolean isNavigationCellClear(float x, float y) {
        // Malo sire od hitbox-a rakete, tako da A* ne bira celiju pored koje
        // raketa fizicki ne moze da prodje.
        final float clearance = 30f;
        for (Wall wall : encounterField.getWalls()) {
            if (x >= wall.getX() - clearance && x <= wall.getX() + wall.getWidth() + clearance
                    && y >= wall.getY() - clearance && y <= wall.getY() + wall.getHeight() + clearance) {
                return false;
            }
        }
        return true;
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

        players.put(playerId, new Player(x, y, new GameSettings(), false));
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
