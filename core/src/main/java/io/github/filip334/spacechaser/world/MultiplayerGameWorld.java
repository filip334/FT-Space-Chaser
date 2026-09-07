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
import io.github.filip334.spacechaser.server.EntityState;
import io.github.filip334.spacechaser.server.GameStateSnapshot;
import io.github.filip334.spacechaser.world.GameSettings;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    public MultiplayerGameWorld(Texture playerIdleTexture, Texture flameSheet,
                                 Texture enemyTexture, Texture bulletTexture) {
        entityRenderer = new EntityRenderer(playerIdleTexture, flameSheet, enemyTexture, bulletTexture);
    }

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

    public void setLocalPlayerId(int id) {
        this.localPlayerId = id;
    }

    public int getLocalPlayerId() {
        return localPlayerId;
    }

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
        Set<Integer> ids = new HashSet<>();

        for (EntityState state : snapshot.players) {
            ids.add(state.id);

            Player p = players.get(state.id);
            if (p == null) {
                p = new Player(state.x, state.y, new GameSettings(), false);
                players.put(state.id, p);
            }
            p.setNetworkState(state.x, state.y, state.rotation);
            p.setNetworkHealth(state.health);
            p.setNetworkFuel(state.fuel);
            p.setNetworkThrusting(state.thrusting);
            p.setNetworkBoosting(state.boosting);
            p.setNetworkScore(state.score);
        }

        players.keySet().retainAll(ids);
    }

    private void syncEnemies(GameStateSnapshot snapshot) {
        Set<Integer> ids = new HashSet<>();

        for (EntityState state : snapshot.enemies) {
            ids.add(state.id);

            Enemy e = enemies.get(state.id);
            if (e == null) {
                e = new Enemy(state.x, state.y);
                enemies.put(state.id, e);
            }
            e.setNetworkState(state.x, state.y, state.rotation);
        }

        enemies.keySet().retainAll(ids);
    }

    private void syncBullets(GameStateSnapshot snapshot) {
        Set<Integer> ids = new HashSet<>();

        for (EntityState state : snapshot.bullets) {
            ids.add(state.id);

            Bullet b = bullets.get(state.id);
            if (b == null) {
                b = new Bullet(state.x, state.y, 0f, 0f, state.rotation);
                bullets.put(state.id, b);
            }
            b.setNetworkState(state.x, state.y, state.rotation);
        }

        bullets.keySet().retainAll(ids);
    }

    private void syncWalls(GameStateSnapshot snapshot) {
        // Server salje zidove samo jednom (staticni su ceo mec) - ignorisi
        // praznu listu u svim ostalim snapshotovima, ne brisi ono sto vec imamo.
        if (!snapshot.wallsIncluded) return;

        Set<Integer> ids = new HashSet<>();

        for (EntityState state : snapshot.walls) {
            ids.add(state.id);

            if (!walls.containsKey(state.id)) {
                walls.put(state.id, new Wall(state.x, state.y, state.width, state.height));
            }
        }

        walls.keySet().retainAll(ids);
    }

    private void syncCoins(GameStateSnapshot snapshot) {
        // Server salje novcice samo kad se neki pokupi - ignorisi praznu
        // listu u svim ostalim snapshotovima, ne brisi ono sto vec imamo.
        if (!snapshot.coinsChanged) return;

        Set<Integer> ids = new HashSet<>();
        for (EntityState state : snapshot.coins) {
            ids.add(state.id);
            if (!coins.containsKey(state.id)) {
                coins.put(state.id, new Coin(state.id, state.x, state.y));
            }
        }
        coins.keySet().retainAll(ids);
    }

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
            shapeRenderer.circle(coin.getX(), coin.getY(), Coin.RADIUS);
        }
        shapeRenderer.end();
        shapeRenderer.setTransformMatrix(new Matrix4());
    }

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

    public void dispose() {
        entityRenderer.dispose();
        shapeRenderer.dispose();
    }
}
