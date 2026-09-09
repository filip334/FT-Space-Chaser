package io.github.filip334.spacechaser.server;

import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Coin;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Wall;
import io.github.filip334.spacechaser.network.message.EntityState;
import io.github.filip334.spacechaser.network.message.GameStateSnapshot;
import io.github.filip334.spacechaser.world.GameWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pravi GameStateSnapshot iz trenutnog stanja GameWorld-a, za slanje
 * klijentima svaki server tick. Izdvojeno iz GameServer da mrezni/tick sloj
 * i pravljenje snapshot-a ne budu u istoj klasi.
 * <p>
 * Zidovi/novcici se ne racunaju svaki tick preko celog snapshot-a - vidi
 * buildWallStates()/buildCoinStates(). Zato ovaj objekat pamti sopstveno
 * stanje (koliko je tikova proslo, da li su zidovi vec poslati, koliko je
 * novcica bilo prosli put) izmedju poziva - jedan GameServer koristi tacno
 * jedan SnapshotBuilder za ceo mec.
 */
public class SnapshotBuilder {

    // VAZNO: prvi tick posle starta NIJE dovoljan trenutak da se puna lista
    // posalje samo jednom - host/gost tek na SVOM sledecem render frejmu
    // prebacuju ekran i tek tada MultiplayerClient dobija svoj
    // MultiplayerGameWorld (setWorld()). Do tada listenLoop() ima world==null
    // i tiho baca sve sto stigne. Zato puna lista ide kroz kratak "startup"
    // prozor (ne samo 1 tick) da sigurno stigne posle te tranzicije.
    private boolean wallsBroadcast = false;
    private int lastCoinCount = -1;
    private int ticksSinceStart = 0;
    private static final int STARTUP_FULL_SYNC_TICKS = 90; // ~1.5s @ 60Hz

    // ---------------- BUILD ----------------

    public GameStateSnapshot build(GameWorld world) {
        ticksSinceStart++;
        boolean startupWindow = ticksSinceStart <= STARTUP_FULL_SYNC_TICKS;

        GameStateSnapshot s = new GameStateSnapshot();
        s.players.addAll(buildPlayerStates(world));
        s.enemies.addAll(buildEnemyStates(world));
        s.bullets.addAll(buildBulletStates(world));
        buildWallStates(world, s, startupWindow);
        buildCoinStates(world, s, startupWindow);

        s.score = world.getScore();
        s.gameTime = world.getGameTime();
        s.matchOver = world.isMatchOver();
        s.wave = world.getWaveNumber();
        s.waveCountdown = world.isWaveCountingDown() ? world.getWaveCountdownSeconds() : 0f;

        return s;
    }

    private List<EntityState> buildPlayerStates(GameWorld world) {
        List<EntityState> states = new ArrayList<>();
        for (Map.Entry<Integer, Player> entry : world.getPlayersMap().entrySet()) {
            states.add(toPlayerState(entry.getKey(), entry.getValue()));
        }
        return states;
    }

    // TODO: zameniti sekvencijalne ID-jeve stabilnim ID-jem sa same Enemy/Bullet klase
    // kad budemo dodavali punu sinhronizaciju neprijatelja i metaka.
    private List<EntityState> buildEnemyStates(GameWorld world) {
        List<EntityState> states = new ArrayList<>();
        int enemyId = 100_000;
        for (Enemy e : world.getEnemies()) {
            states.add(toSimpleState(enemyId++, e.getX(), e.getY(), e.getRotation()));
        }
        return states;
    }

    private List<EntityState> buildBulletStates(GameWorld world) {
        List<EntityState> states = new ArrayList<>();
        int bulletId = 200_000;
        for (Bullet b : world.getBullets()) {
            states.add(toSimpleState(bulletId++, b.getX(), b.getY(), b.getRotation()));
        }
        return states;
    }

    /**
     * Zidovi su staticni za ceo mec (nikad se ne pomeraju/dodaju) - saljemo
     * ih ponovljeno samo tokom kratkog "startup" prozora (ne 60x/sec ceo
     * mec), da sigurno stignu i ako klijent zakasni da zakaci svoj svet.
     */
    private void buildWallStates(GameWorld world, GameStateSnapshot s, boolean startupWindow) {
        if (!startupWindow && wallsBroadcast) return;

        int wallId = 300_000;
        for (Wall w : world.getWalls()) {
            EntityState state = new EntityState();
            state.id = wallId++;
            state.x = w.getX();
            state.y = w.getY();
            state.width = w.getWidth();
            state.height = w.getHeight();
            s.walls.add(state);
        }
        s.wallsIncluded = true;
        wallsBroadcast = true;
    }

    /**
     * Novcici se samo skupljaju (nikad ne dodaju tokom meca) - van startup
     * prozora saljemo punu listu samo kad se broj promeni od prethodnog tick-a.
     */
    private void buildCoinStates(GameWorld world, GameStateSnapshot s, boolean startupWindow) {
        int coinCount = world.getCoins().size;
        if (!startupWindow && coinCount == lastCoinCount) return;

        for (Coin coin : world.getCoins()) {
            s.coins.add(toSimpleState(coin.getId(), coin.getX(), coin.getY(), 0f));
        }
        s.coinsChanged = true;
        lastCoinCount = coinCount;
    }

    // ---------------- ENTITY STATE ----------------

    private EntityState toPlayerState(int id, Player p) {
        EntityState s = new EntityState();
        s.id = id;
        s.x = p.getX();
        s.y = p.getY();
        s.rotation = p.getRotation();
        s.health = p.getHealth();
        s.maxHealth = p.getMaxHealth();
        s.fuel = p.getFuel();
        s.maxFuel = p.getMaxFuel();
        s.thrusting = p.isThrusting();
        s.boosting = p.isBoosting();
        s.score = p.getScore();
        return s;
    }

    private EntityState toSimpleState(int id, float x, float y, float rotation) {
        EntityState s = new EntityState();
        s.id = id;
        s.x = x;
        s.y = y;
        s.rotation = rotation;
        return s;
    }
}
