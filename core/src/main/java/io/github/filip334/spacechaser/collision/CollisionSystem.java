package io.github.filip334.spacechaser.collision;

import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Coin;
import io.github.filip334.spacechaser.entity.Wall;

import java.util.Map;

public class CollisionSystem {

    // Raspored novcica prati original (124 komada: 64 spoljni hodnik + 40
    // unutrasnji hodnik + 20 centar), pa je vrednost po novcicu snizena sa
    // ranijih 100 da ukupan skor ostane razuman.
    private static final int COIN_VALUE = 10;

    /**
     * Sudari koji se desavaju JEDNOM po frejmu (ne po igracu) - meci ne
     * pripadaju nijednom konkretnom igracu u smislu fizike, vec samo svetu.
     *
     * FIX: ranije se cela ova logika zvala IZNUTRA per-player petlje u
     * GameWorld (jednom za svakog igraca, nad ISTOM celom listom metaka).
     * Zbog bullet.isDead() provere ubistvo se fizicki racunalo samo jednom,
     * ali uvek se pripisivalo igracu koji je PRVI na redu u Map iteraciji
     * (obicno host), bez obzira ko je stvarno ispalio taj metak. Dok je
     * postojao samo zajednicki skor to se nije primetilo; sad kad svaki
     * igrac ima svoj skor, ubistvo se pripisuje pravom vlasniku metka
     * (Bullet.getOwnerId()).
     *
     * @return ukupno poena od ubijanja neprijatelja ovog frejma (vec je
     *         pojedinacno pripisano odgovarajucim igracima preko
     *         Player.addScore()) - GameWorld ga samo dodaje u ukupan skor.
     */
    public int checkWorldCollisions(Array<Bullet> bullets, Array<Enemy> enemies, Array<Wall> walls,
                                     Map<Integer, Player> players) {
        checkBulletsVsWalls(bullets, walls);
        int killScore = checkBulletsVsEnemies(bullets, enemies, players);
        checkEnemiesVsWalls(enemies, walls);
        return killScore;
    }

    /** Sudari specificni za jednog igraca - njegov sopstveni brod protiv zidova i neprijatelja. */
    public void checkPlayerCollisions(Player player, Array<Enemy> enemies, Array<Wall> walls) {
        checkPlayerVsWalls(player, walls);
        checkPlayerVsEnemies(player, enemies);
    }

    public int collectCoins(Player player, Array<Coin> coins) {
        if (player.isDead()) return 0;

        int scoreGained = 0;
        for (int i = coins.size - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            float dx = player.getX() - coin.getX();
            float dy = player.getY() - coin.getY();
            float pickupDistance = Coin.RADIUS + 42f;
            if (dx * dx + dy * dy <= pickupDistance * pickupDistance) {
                coins.removeIndex(i);
                scoreGained += COIN_VALUE;
            }
        }
        return scoreGained;
    }

    private void checkBulletsVsWalls(Array<Bullet> bullets, Array<Wall> walls) {
        for (Bullet bullet : bullets) {
            if (bullet.isDead()) continue;

            for (Wall wall : walls) {
                if (bullet.getHitbox().overlaps(wall.getHitbox()) || bulletCrossedWall(bullet, wall)) {
                    bullet.isDead(true);
                    break;
                }
            }
        }
    }

    private boolean bulletCrossedWall(Bullet bullet, Wall wall) {

        float minX = wall.getX();
        float maxX = wall.getX() + wall.getWidth();

        float minY = wall.getY();
        float maxY = wall.getY() + wall.getHeight();

        float startX = bullet.getPreviousX();
        float startY = bullet.getPreviousY();

        float endX = bullet.getX();
        float endY = bullet.getY();

        float dx = endX - startX;
        float dy = endY - startY;

        float tMin = 0f;
        float tMax = 1f;

        // X
        if (Math.abs(dx) < 0.00001f) {

            if (startX < minX || startX > maxX) {
                return false;
            }

        } else {

            float tx1 = (minX - startX) / dx;
            float tx2 = (maxX - startX) / dx;

            float nearX = Math.min(tx1, tx2);
            float farX = Math.max(tx1, tx2);

            tMin = Math.max(tMin, nearX);
            tMax = Math.min(tMax, farX);

            if (tMin > tMax) {
                return false;
            }
        }

        // Y
        if (Math.abs(dy) < 0.00001f) {

            if (startY < minY || startY > maxY) {
                return false;
            }

        } else {

            float ty1 = (minY - startY) / dy;
            float ty2 = (maxY - startY) / dy;

            float nearY = Math.min(ty1, ty2);
            float farY = Math.max(ty1, ty2);

            tMin = Math.max(tMin, nearY);
            tMax = Math.min(tMax, farY);

            if (tMin > tMax) {
                return false;
            }
        }

        return true;
    }
    
    private void checkPlayerVsWalls(Player player,Array<Wall> walls) {

        if (!overlapsAnyWall(player, walls)) {
            return;
        }

        /*
         * FIX: ranije se ovde prvo poništavala SAMO rotacija (vraćala na
         * prethodnu) kad bi novi ugao izazvao sudar. Dok god je brod stajao
         * uz zid pod uglom gde BILO KOJA nova rotacija dodiruje zid, taj
         * pokušaj se svaki frejm brisao - igrač fizički nije mogao da se
         * okrene (A/D nisu radili), samo kretanje unazad je pomeralo brod
         * dovoljno da rotacija prestane da izaziva sudar.
         *
         * Sad se sudar UVEK rešava kroz CollisionResolver, bez obzira da li
         * ga je izazvala rotacija ili pravo kretanje - on zadržava novu
         * rotaciju i samo odgurne brod (MTV push-out) taman toliko da
         * preklapanje nestane, plus ukloni brzinu koja ide ka zidu. Tako
         * okretanje uz zid brod blago odgurne od njega umesto da ga zakljuca.
         */
        CollisionResolver.resolvePlayerVsWalls(
                player,
                walls
        );
    }

    private boolean overlapsAnyWall(Player player, Array<Wall> walls) {
        for (Wall wall : walls) {
            if (player.getHitbox().overlaps(wall.getHitbox())) {
                return true;
            }
        }
        return false;
    }

    private void checkEnemiesVsWalls(Array<Enemy> enemies, Array<Wall> walls) {
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) continue;

            for (Wall wall : walls) {
                if (enemy.getHitbox().overlaps(wall.getHitbox())) {
                    enemy.restorePreviousPosition();
                    break;
                }
            }
        }
    }

    private int checkBulletsVsEnemies(Array<Bullet> bullets, Array<Enemy> enemies, Map<Integer, Player> players) {
        int scoreGained = 0;

        for (int i = 0; i < bullets.size; i++) {
            Bullet bullet = bullets.get(i);

            if (bullet.isDead()) {
                continue;
            }

            for (int j = 0; j < enemies.size; j++) {
                Enemy enemy = enemies.get(j);

                if (enemy.isDead()) {
                    continue;
                }

                if (bullet.getHitbox().overlaps(enemy.getHitbox())) {
                    bullet.isDead(true);
                    enemy.takeDamage(bullet.DAMAGE);
                    if (enemy.isDead()) {
                        int value = enemy.getScoreValue();
                        Player owner = players.get(bullet.getOwnerId());
                        if (owner != null) {
                            owner.addScore(value);
                        }
                        scoreGained += value;
                    }
                    break;
                }
            }
        }

        return scoreGained;
    }

    private void checkPlayerVsEnemies(Player player, Array<Enemy> enemies) {
        // Mrtav igrac vise nije fizicka meta. Njegov hitbox moze ostati na
        // poslednjoj poziciji radi stanja sveta, ali neprijatelji ne smeju da
        // nestaju kada ga dodirnu.
        if (player.isDead()) {
            return;
        }

        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                continue;
            }

            if (player.getHitbox().overlaps(enemy.getHitbox())) {
                player.takeDamage(enemy.DAMAGE);
                enemy.isDead(true);
            }
        }
    }
}
