package io.github.filip334.spacechaser.collision;

import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Coin;
import io.github.filip334.spacechaser.entity.Wall;

public class CollisionSystem {
    
    public int checkCollisions(Player player, Array<Bullet> bullets, Array<Enemy> enemies, Array<Wall> walls) {
        int scoreGained = 0;

        checkBulletsVsWalls(bullets, walls);
        scoreGained += checkBulletsVsEnemies(bullets, enemies);
        checkPlayerVsWalls(player, walls);
        checkEnemiesVsWalls(enemies, walls);
        checkPlayerVsEnemies(player, enemies);
        return scoreGained;
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
                scoreGained += 100;
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
         * Prvo pokušavamo da poništimo samo rotaciju.
         *
         * Ako je nova rotacija uzrokovala sudar,
         * vraćamo staru rotaciju.
         */
        player.restorePreviousRotation();

        if (!overlapsAnyWall(player, walls)) {
            return;
        }

        /*
         * Ako je igrač i dalje u zidu,
         * CollisionResolver rešava poziciju
         * i velocity.
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

    private int checkBulletsVsEnemies(Array<Bullet> bullets, Array<Enemy> enemies) {
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
                        scoreGained += 10;
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
