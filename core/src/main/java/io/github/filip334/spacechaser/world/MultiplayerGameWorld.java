package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Wall;
import io.github.filip334.spacechaser.renderer.EntityRenderer;
import io.github.filip334.spacechaser.server.EntityState;
import io.github.filip334.spacechaser.server.GameStateSnapshot;

import java.util.Collection;
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

    private final EntityRenderer entityRenderer;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();

    private volatile int localPlayerId = -1;

    public MultiplayerGameWorld(Texture playerTexture, Texture enemyTexture, Texture bulletTexture) {
        entityRenderer = new EntityRenderer(playerTexture, enemyTexture, bulletTexture);
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
    }

    private void syncPlayers(GameStateSnapshot snapshot) {
        Set<Integer> ids = new HashSet<>();

        for (EntityState state : snapshot.players) {
            ids.add(state.id);

            Player p = players.get(state.id);
            if (p == null) {
                p = new Player(state.x, state.y);
                players.put(state.id, p);
            }
            p.setNetworkState(state.x, state.y, state.rotation);
            p.setNetworkHealth(state.health);
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

    /**
     * Zidovi se nikad ne pomeraju - dovoljno ih je napraviti jednom po ID-ju
     * i vise ih ne diramo, samo ih zadrzimo dok god server salje taj ID.
     */
    private void syncWalls(GameStateSnapshot snapshot) {
        Set<Integer> ids = new HashSet<>();

        for (EntityState state : snapshot.walls) {
            ids.add(state.id);

            if (!walls.containsKey(state.id)) {
                walls.put(state.id, new Wall(state.x, state.y, state.width, state.height));
            }
        }

        walls.keySet().retainAll(ids);
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

    /**
     * Poziva se van SpriteBatch.begin()/end() bloka, isto kao GameWorld.renderShapes().
     */
    public synchronized void renderShapes() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Wall w : walls.values()) {
            w.render(shapeRenderer);
        }
        shapeRenderer.end();
    }

    public synchronized Collection<Player> getPlayers() {
        return players.values();
    }

    public synchronized Collection<Enemy> getEnemies() {
        return enemies.values();
    }

    public synchronized Collection<Bullet> getBullets() {
        return bullets.values();
    }

    public void dispose() {
        entityRenderer.dispose();
        shapeRenderer.dispose();
    }
}