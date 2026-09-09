package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Wall;
import io.github.filip334.spacechaser.enemy.PathFinder;

/**
 * Upravlja talasima neprijatelja - odbrojavanje izmedju talasa, stvaranje
 * novog talasa (sve teze sto igra odmice, uvek u sredini mape) i
 * "besnjenje" kad malo raketa ostane u talasu. Takodje gradi navigacionu
 * mrezu (A*) koju neprijatelji koriste da izbegnu zidove. Izdvojeno iz
 * GameWorld da simulacija sveta i upravljanje talasima ne budu u istoj klasi.
 */
public class WaveManager {

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
    private static final float NAVIGATION_CELL_SIZE = 30f;

    private final EncounterField encounterField;
    private final PathFinder pathFinder;

    private int waveNumber = 0;
    private int waveInitialEnemyCount = 0;
    private boolean waveCountingDown = false;
    private float waveCountdown = 0f;

    // ---------------- KONSTRUKTOR ----------------

    public WaveManager(EncounterField encounterField) {
        this.encounterField = encounterField;
        this.pathFinder = createPathFinder();
    }

    // ---------------- UPDATE ----------------

    /**
     * Kad nestane poslednja raketa iz talasa, sledeci talas se ne stvara
     * odmah - prvo 3 sekunde odbrojavanja (prikazano na sredini mape), pa tek
     * onda spawnWave().
     */
    public void update(float delta, Array<Enemy> enemies, boolean allPlayersDead) {
        if (enemies.size > 0 || allPlayersDead) {
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
            spawnWave(enemies);
        }
    }

    /**
     * Kad u talasu ostane malo raketa (<= ENRAGE_THRESHOLD od pocetnog broja),
     * sve preostale odmah krecu direktno na igraca - bez ovoga bi igrac mogao
     * da ostavi 1-2 rakete u patroli i bezbedno farmi novcice/vreme
     * izbegavajuci ih.
     */
    public void updateEnrage(Array<Enemy> enemies) {
        if (waveInitialEnemyCount <= 0) return;

        int threshold = Math.max(1, Math.round(waveInitialEnemyCount * ENRAGE_THRESHOLD));
        boolean shouldEnrage = enemies.size <= threshold;

        for (Enemy enemy : enemies) {
            enemy.setEnraged(shouldEnrage);
        }
    }

    // ---------------- SPAWN ----------------

    /**
     * Svaki talas neprijatelja se stvara u sredini mape (umesto nasumicno po
     * celoj mapi) - sto je igra dalje odmakla, talas je teze: vise
     * neprijatelja (do MAX_WAVE_ENEMY_COUNT), vise zivota (N pogodaka za
     * talas N) i vise poena po ubistvu.
     */
    private void spawnWave(Array<Enemy> enemies) {
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
            configureNavigation(enemy);
            if (enemy.isInNavigablePosition()) {
                return enemy;
            }
        }

        // Ako su svi pokusaji pogodili zid (blizu centra ima vise prepreka),
        // koristi tacan centar - navigacija (A*) ce ga izbaciti odatle.
        Enemy fallback = new Enemy(centerX, centerY);
        configureNavigation(fallback);
        return fallback;
    }

    // ---------------- NAVIGATION ----------------

    private void configureNavigation(Enemy enemy) {
        enemy.setNavigation(encounterField.getWalls(), encounterField.getFieldX(), encounterField.getFieldY(),
                encounterField.getFieldWidth(), encounterField.getFieldHeight(), pathFinder, NAVIGATION_CELL_SIZE);
    }

    private PathFinder createPathFinder() {
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

    // ---------------- GET / SET ----------------

    public boolean isWaveCountingDown() {
        return waveCountingDown;
    }

    public float getWaveCountdownSeconds() {
        return waveCountdown;
    }

    public int getWaveNumber() {
        return waveNumber;
    }
}
