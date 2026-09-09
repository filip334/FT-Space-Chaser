package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Coin;
import io.github.filip334.spacechaser.entity.Wall;
import io.github.filip334.spacechaser.renderer.EntityRenderer;
import io.github.filip334.spacechaser.network.message.EntityState;
import io.github.filip334.spacechaser.network.message.GameStateSnapshot;
import io.github.filip334.spacechaser.settings.GameSettings;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Client-side "glupi" svet za multiplayer - ne pokrece enemy AI ni kolizije,
 * samo ogledalo stanja koje server salje kroz GameStateSnapshot.
 * Svi entiteti se identifikuju po ID-ju iz EntityState i azuriraju preko
 * setNetworkState(...) - nikad se ne poziva njihov update(delta).
 */
public class MultiplayerGameWorld {

    private final Map<Integer, Player> players = new LinkedHashMap<>();
    private final Map<Integer, Enemy> enemies = new LinkedHashMap<>();
    private final Map<Integer, Bullet> bullets = new LinkedHashMap<>();
    private final Map<Integer, Wall> walls = new LinkedHashMap<>();
    private final Map<Integer, Coin> coins = new LinkedHashMap<>();

    private final EntityRenderer entityRenderer;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();

    private volatile int localPlayerId = -1;
    private volatile int score = 0;
    private volatile float gameTime = 0f;
    private volatile boolean matchOver = false;
    private volatile int wave = 0;
    private volatile float waveCountdown = 0f;

    // ---------------- KONSTRUKTOR ----------------

    public MultiplayerGameWorld(Texture playerIdleTexture, Texture flameSheet,
                                 Texture enemyTexture, Texture bulletTexture) {
        entityRenderer = new EntityRenderer(playerIdleTexture, flameSheet, enemyTexture, bulletTexture);
    }

    // ---------------- UPDATE ----------------

    /**
     * Poziva se jednom po frejmu iz GameScreen-a da animacija odmice, kao i
     * da se pozicije igraca priblize poslednjem mreznom stanju (interpolacija -
     * vidi Player.interpolateNetworkState).
     */
    public synchronized void update(float delta) {
        entityRenderer.update(delta);
        for (Player p : players.values()) {
            p.interpolateNetworkState(delta);
        }
    }

    // ---------------- GET / SET ----------------

    public void setLocalPlayerId(int id) {
        this.localPlayerId = id;
    }

    public int getLocalPlayerId() {
        return localPlayerId;
    }

    // ---------------- SNAPSHOT SYNC ----------------

    public synchronized void applySnapshot(GameStateSnapshot snapshot) {
        syncPlayers(snapshot);
        syncEnemies(snapshot);
        syncBullets(snapshot);
        syncWalls(snapshot);
        syncCoins(snapshot);

        this.score = snapshot.score;
        this.gameTime = snapshot.gameTime;
        this.matchOver = snapshot.matchOver;
        this.wave = snapshot.wave;
        this.waveCountdown = snapshot.waveCountdown;
    }

    private void syncPlayers(GameStateSnapshot snapshot) {
        syncEntities(snapshot.players, players,
                state -> new Player(state.x, state.y, new GameSettings(), false),
                (p, state) -> {
                    p.setNetworkState(state.x, state.y, state.rotation);
                    p.setNetworkHealth(state.health);
                    p.setNetworkFuel(state.fuel);
                    p.setNetworkThrusting(state.thrusting);
                    p.setNetworkBoosting(state.boosting);
                    p.setNetworkScore(state.score);
                });
    }

    private void syncEnemies(GameStateSnapshot snapshot) {
        syncEntities(snapshot.enemies, enemies,
                state -> new Enemy(state.x, state.y),
                (e, state) -> e.setNetworkState(state.x, state.y, state.rotation));
    }

    private void syncBullets(GameStateSnapshot snapshot) {
        syncEntities(snapshot.bullets, bullets,
                state -> new Bullet(state.x, state.y, 0f, 0f, state.rotation),
                (b, state) -> b.setNetworkState(state.x, state.y, state.rotation));
    }

    private void syncWalls(GameStateSnapshot snapshot) {
        // Server salje zidove samo jednom (staticni su ceo mec) - ignorisi
        // praznu listu u svim ostalim snapshotovima, ne brisi ono sto vec imamo.
        if (!snapshot.wallsIncluded) return;

        syncEntities(snapshot.walls, walls,
                state -> new Wall(state.x, state.y, state.width, state.height), null);
    }

    private void syncCoins(GameStateSnapshot snapshot) {
        // Server salje novcice samo kad se neki pokupi - ignorisi praznu
        // listu u svim ostalim snapshotovima, ne brisi ono sto vec imamo.
        if (!snapshot.coinsChanged) return;

        syncEntities(snapshot.coins, coins,
                state -> new Coin(state.id, state.x, state.y), null);
    }

    /**
     * Zajednicka logika za sve sync* metode iznad - upisi/dodaj entitet za
     * svaki EntityState iz snapshot-a, pa ukloni sve sto vise nije u listi
     * (server ga je uklonio - npr. metak je pogodio, novcic pokupljen).
     * updater je null za zidove/novcice - oni se samo jednom postave i
     * nikad kasnije ne menjaju.
     */
    private <T> void syncEntities(List<EntityState> states, Map<Integer, T> map,
                                   Function<EntityState, T> factory, BiConsumer<T, EntityState> updater) {
        Set<Integer> ids = new HashSet<>();

        for (EntityState state : states) {
            ids.add(state.id);

            T entity = map.get(state.id);
            if (entity == null) {
                entity = factory.apply(state);
                map.put(state.id, entity);
            }
            if (updater != null) {
                updater.accept(entity, state);
            }
        }

        map.keySet().retainAll(ids);
    }

    // ---------------- RENDER ----------------

    public synchronized void render(SpriteBatch batch) {
        for (Player p : players.values()) {
            entityRenderer.render(batch, p);
        }
        for (Enemy e : enemies.values()) {
            entityRenderer.render(batch, e);
        }
        for (Bullet b : bullets.values()) {
            entityRenderer.render(batch, b);
        }
    }

    public synchronized void renderShapes(float offsetX, Matrix4 projectionMatrix) {
        shapeRenderer.setProjectionMatrix(projectionMatrix);
        shapeRenderer.setTransformMatrix(new Matrix4().setToTranslation(offsetX, 0f, 0f));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Wall w : walls.values()) {
            w.render(shapeRenderer);
        }
        shapeRenderer.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
        for (Coin coin : coins.values()) {
            shapeRenderer.rect(coin.getX() - Coin.RADIUS, coin.getY() - Coin.RADIUS, Coin.RADIUS * 2f, Coin.RADIUS * 2f);
        }
        shapeRenderer.end();
        shapeRenderer.setTransformMatrix(new Matrix4());
    }

    // ---------------- GET / SET ----------------

    public synchronized Player getLocalPlayer() {
        return players.get(localPlayerId);
    }

    /**
     * Vraca prvog igraca koji nije lokalni. Za sad dovoljno za 1v1 HUD;
     * kad podrzimo vise od 2 igraca, HUD ce trebati da prikaze listu.
     */
    public synchronized Player getOpponentPlayer() {
        for (Map.Entry<Integer, Player> entry : players.entrySet()) {
            if (entry.getKey() != localPlayerId) {
                return entry.getValue();
            }
        }
        return null;
    }

    public int getScore() {
        return score;
    }

    public float getGameTime() {
        return gameTime;
    }

    public boolean isMatchOver() {
        return matchOver;
    }

    public int getWaveNumber() {
        return wave;
    }

    public boolean isWaveCountingDown() {
        return waveCountdown > 0f;
    }

    public float getWaveCountdownSeconds() {
        return waveCountdown;
    }

    // ---------------- DISPOSE ----------------

    public void dispose() {
        entityRenderer.dispose();
        shapeRenderer.dispose();
    }
}
