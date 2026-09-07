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
    private static final int MAX_WAVE_ENEMY_COUNT = 8;
    // = Bullet.DAMAGE, tako da neprijatelji u talasu N ginu na tacno N pogodaka.
    private static final float ENEMY_HEALTH_PER_WAVE = 33f;
    private static final int SCORE_PER_KILL_BASE = 10;
    private static final float ENEMY_SPAWN_RADIUS = 70f;
    private static final float WAVE_COUNTDOWN_SECONDS = 3f;
    // Kad broj preostalih raketa u talasu padne na ovaj procenat pocetnog
    // broja (ili manje), sve preostale postaju "besne" (direktno jure igraca).
    private static final float ENRAGE_THRESHOLD = 0.3f;
    private int nextCoinId = 1;
    private int waveNumber = 0;
    private int waveInitialEnemyCount = 0;
    private boolean waveCountingDown = false;
    private float waveCountdown = 0f;

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

        // Jednom po frejmu (ne po igracu) - meci vec znaju svog vlasnika
        // (Bullet.getOwnerId()), pa se ubistvo pripisuje pravom igracu.
        score += collisionSystem.checkWorldCollisions(bullets, enemies, encounterField.getWalls(), players);

        for (Player p : players.values()) {
            collisionSystem.checkPlayerCollisions(p, enemies, encounterField.getWalls());
            int coinsGained = collisionSystem.collectCoins(p, coins);
            p.addScore(coinsGained);
            score += coinsGained;
        }

        // Kad su svi novcici na mapi pokupljeni, spawnuj ceo raspored ponovo
        // umesto da mapa ostane prazna do kraja meca.
        if (coins.size == 0) {
            spawnCoins();
        }

        cleanupDeadEntities();

        updateWaveCountdown(delta);
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
        updateEnrage();
        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).update(delta, target);
            enemies.get(i).applySeparation(enemies, delta);
        }
    }

    /**
     * Kad u talasu ostane malo raketa (<= ENRAGE_THRESHOLD od pocetnog broja),
     * sve preostale odmah krecu direktno na igraca - bez ovoga bi igrac mogao
     * da ostavi 1-2 rakete u patroli i bezbedno farmi novcice/vreme
     * izbegavajuci ih.
     */
    private void updateEnrage() {
        if (waveInitialEnemyCount <= 0) return;

        int threshold = Math.max(1, Math.round(waveInitialEnemyCount * ENRAGE_THRESHOLD));
        boolean shouldEnrage = enemies.size <= threshold;

        for (Enemy enemy : enemies) {
            enemy.setEnraged(shouldEnrage);
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
                local.getFuel(), local.getMaxFuel(), score, gameTime, waveNumber);

        if (waveCountingDown) {
            hudRenderer.renderCountdown(batch, (int) Math.ceil(waveCountdown));
        }
    }

    // ---------------- SPAWN ----------------

    /**
     * Kad nestane poslednja raketa iz talasa, sledeci talas se ne stvara
     * odmah - prvo 3 sekunde odbrojavanja (prikazano na sredini mape), pa tek
     * onda spawnEnemyWave().
     */
    private void updateWaveCountdown(float delta) {
        if (enemies.size > 0 || allPlayersDead()) {
            waveCountingDown = false;
            return;
        }

        if (!waveCountingDown) {
            waveCountingDown = true;
            waveCountdown = WAVE_COUNTDOWN_SECONDS;
            return;
        }

        waveCountdown -= delta;
        if (waveCountdown <= 0f) {
            waveCountingDown = false;
            spawnEnemyWave();
        }
    }

    public boolean isWaveCountingDown() {
        return waveCountingDown;
    }

    public float getWaveCountdownSeconds() {
        return waveCountdown;
    }

    /**
     * Svaki talas neprijatelja se stvara u sredini mape (umesto nasumicno po
     * celoj mapi) - sto je igra dalje odmakla, talas je teze: vise
     * neprijatelja (do MAX_WAVE_ENEMY_COUNT), vise zivota (N pogodaka za
     * talas N) i vise poena po ubistvu.
     */
    private void spawnEnemyWave() {
        waveNumber++;

        int enemyCount = Math.min(ENEMY_WAVE_SIZE + (waveNumber - 1), MAX_WAVE_ENEMY_COUNT);
        float waveHealth = ENEMY_HEALTH_PER_WAVE * waveNumber;
        int waveScoreValue = SCORE_PER_KILL_BASE * waveNumber;

        float centerX = encounterField.getFieldX() + encounterField.getFieldWidth() / 2f;
        float centerY = encounterField.getFieldY() + encounterField.getFieldHeight() / 2f;

        for (int i = 0; i < enemyCount; i++) {
            Enemy enemy = createEnemyNearCenter(centerX, centerY);
            enemy.setMaxHealth(waveHealth);
            enemy.setScoreValue(waveScoreValue);
            enemies.add(enemy);
        }

        waveInitialEnemyCount = enemyCount;
    }

    private Enemy createEnemyNearCenter(float centerX, float centerY) {
        for (int attempt = 0; attempt < 30; attempt++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float distance = MathUtils.random(0f, ENEMY_SPAWN_RADIUS);
            float x = centerX + MathUtils.cos(angle) * distance;
            float y = centerY + MathUtils.sin(angle) * distance;

            Enemy enemy = new Enemy(x, y);
            configureEnemyNavigation(enemy);
            if (enemy.isInNavigablePosition()) {
                return enemy;
            }
        }

        // Ako su svi pokusaji pogodili zid (blizu centra ima vise prepreka),
        // koristi tacan centar - navigacija (A*) ce ga izbaciti odatle.
        Enemy fallback = new Enemy(centerX, centerY);
        configureEnemyNavigation(fallback);
        return fallback;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    /**
     * Pozicije novcica sa originalne Taito Space Chaser mape - izvucene iz
     * vektorizovanog traga screenshot-a, pa poravnate u prave linije i
     * simetrizovane (levo-desno i gore-dole ogledalo) da bi izgledale
     * uredno umesto sa sumom iz trasiranja. Koordinate su u jedinicama
     * celije {col, row} - lako za rucno dotericanje.
     */
    private static final float[][] COIN_POSITIONS = {
            {1.43f,0.53f}, {1.75f,0.53f}, {2.12f,0.53f}, {2.43f,0.53f}, {2.80f,0.53f}, {3.13f,0.53f},
            {3.70f,0.53f}, {6.30f,0.53f}, {6.87f,0.53f}, {7.57f,0.53f}, {7.89f,0.53f}, {8.25f,0.53f},
            {8.57f,0.53f}, {8.95f,0.53f}, {3.94f,0.53f}, {7.19f,0.53f}, {0.58f,1.41f}, {9.42f,1.41f},
            {2.43f,1.57f}, {2.80f,1.57f}, {3.24f,1.57f}, {3.62f,1.57f}, {3.94f,1.57f}, {6.38f,1.57f},
            {6.76f,1.57f}, {7.19f,1.57f}, {7.57f,1.57f}, {7.89f,1.57f}, {0.58f,1.77f}, {9.42f,1.77f},
            {0.58f,2.13f}, {9.42f,2.13f}, {1.59f,2.25f}, {8.41f,2.25f}, {0.58f,2.52f}, {9.42f,2.52f},
            {1.59f,2.64f}, {8.41f,2.64f}, {0.58f,2.78f}, {9.42f,2.78f}, {0.58f,3.15f}, {1.59f,3.15f},
            {8.41f,3.15f}, {9.42f,3.15f}, {0.58f,3.56f}, {1.59f,3.56f}, {2.60f,3.56f}, {3.46f,3.56f},
            {5.23f,3.56f}, {6.54f,3.56f}, {7.40f,3.56f}, {8.41f,3.56f}, {9.42f,3.56f}, {0.58f,3.86f},
            {1.59f,3.86f}, {2.60f,3.86f}, {3.46f,3.86f}, {5.23f,3.86f}, {6.54f,3.86f}, {7.40f,3.86f},
            {8.41f,3.86f}, {9.42f,3.86f}, {0.58f,6.44f}, {1.59f,6.44f}, {2.60f,6.44f}, {3.46f,6.44f},
            {5.23f,6.44f}, {6.54f,6.44f}, {7.40f,6.44f}, {8.41f,6.44f}, {9.42f,6.44f}, {0.58f,6.83f},
            {1.59f,6.83f}, {2.60f,6.83f}, {3.46f,6.83f}, {5.23f,6.83f}, {6.54f,6.83f}, {7.40f,6.83f},
            {8.41f,6.83f}, {9.42f,6.83f}, {1.59f,7.36f}, {8.41f,7.36f}, {0.58f,7.48f}, {9.42f,7.48f},
            {1.59f,7.75f}, {8.41f,7.75f}, {0.58f,7.87f}, {9.42f,7.87f}, {0.58f,8.23f}, {9.42f,8.23f},
            {2.43f,8.43f}, {2.80f,8.43f}, {3.24f,8.43f}, {3.62f,8.43f}, {3.94f,8.43f}, {6.38f,8.43f},
            {6.76f,8.43f}, {7.19f,8.43f}, {7.57f,8.43f}, {7.89f,8.43f}, {0.58f,8.59f}, {9.42f,8.59f},
            {0.58f,8.85f}, {9.42f,8.85f}, {1.43f,9.47f}, {1.75f,9.47f}, {2.12f,9.47f}, {2.43f,9.47f},
            {2.80f,9.47f}, {3.13f,9.47f}, {3.70f,9.47f}, {6.30f,9.47f}, {6.87f,9.47f}, {7.57f,9.47f},
            {7.89f,9.47f}, {8.25f,9.47f}, {8.57f,9.47f}, {8.95f,9.47f}, {3.46f,9.47f}, {6.54f,9.47f},
    };

    private void spawnCoins() {
        float fx = encounterField.getFieldX();
        float fy = encounterField.getFieldY();
        float cellSize = encounterField.getCellSize();

        for (float[] point : COIN_POSITIONS) {
            float x = fx + point[0] * cellSize;
            float y = fy + point[1] * cellSize;

            if (isPickupPositionClear(x, y)) {
                coins.add(new Coin(nextCoinId++, x, y));
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
        // raketa fizicki ne moze da prodje. Stvarna fizicka bezbednost se
        // svakako proverava posebno (overlapsNavigationWall) pri svakom
        // pokusaju kretanja - ovaj clearance samo blago favorizuje putanje
        // dalje od zidova. FIX: sa 30 (= cela velicina navigacione celije)
        // preko 55% mreze na gusce zidanim mapama je bilo markirano kao
        // neprohodno, pa se raketa cesto nalazila TACNO u toj baferskoj
        // zoni - findPath() je tada odmah odustajao (viz. PathFinder fix)
        // sto je izgledalo kao zamrzavanje/zbunjivanje. Manji clearance
        // ostavlja mnogo vise stvarno gazljivih celija.
        final float clearance = 15f;
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

        // Lokalna pozicija vrha nosa u odnosu na centar broda
        float localNoseX = 50f;
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
