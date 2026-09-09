package io.github.filip334.spacechaser.collision;

import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Bullet;
import io.github.filip334.spacechaser.entity.Enemy;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Coin;
import io.github.filip334.spacechaser.entity.Wall;

import java.util.Map;

public class CollisionSystem {

    private static final int COIN_VALUE = 10;

    // ---------------- PUBLIC API ----------------

    public int checkWorldCollisions(Array<Bullet> bullets, Array<Enemy> enemies, Array<Wall> walls,
                                     Map<Integer, Player> players) {
        checkBulletsVsWalls(bullets, walls);
        int killScore = checkBulletsVsEnemies(bullets, enemies, players);
        checkEnemiesVsWalls(enemies, walls);
        return killScore;
    }

    public int checkPlayerCollisions(Player player, Array<Enemy> enemies, Array<Wall> walls) {
        checkPlayerVsWalls(player, walls);
        return checkPlayerVsEnemies(player, enemies);
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

    // ---------------- BULLETS VS WALLS ----------------

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
    
    // ---------------- PLAYER VS WALLS ----------------

    private void checkPlayerVsWalls(Player player,Array<Wall> walls) {

        if (!overlapsAnyWall(player, walls)) {
            return;
        }

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

    // ---------------- ENEMIES VS WALLS ----------------

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

    // ---------------- BULLETS VS ENEMIES ----------------

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

    // ---------------- PLAYER VS ENEMIES ----------------

    private int checkPlayerVsEnemies(Player player, Array<Enemy> enemies) {
        if (player.isDead()) {
            return 0;
        }

        int scoreGained = 0;

        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                continue;
            }

            if (player.getHitbox().overlaps(enemy.getHitbox())) {
                player.takeDamage(enemy.DAMAGE);
                enemy.isDead(true);
                scoreGained += enemy.getScoreValue();
            }
        }

        return scoreGained;
    }
}
