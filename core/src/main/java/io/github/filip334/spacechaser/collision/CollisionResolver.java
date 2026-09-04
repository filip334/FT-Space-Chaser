package io.github.filip334.spacechaser.collision;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.entity.Wall;

/**
 *
 * @author Todorovic
 */
public class CollisionResolver {
    private static final float SKIN = 0.01f;

    private CollisionResolver() {
    }

    public static void resolvePlayerVsWalls(
            Player player,
            Array<Wall> walls) {

        for (Wall wall : walls) {

            CollisionData collision =
                    findCollision(player, wall);

            if (collision == null) {
                continue;
            }

            // Normalu okreni tako da pokazuje
            // OD zida KA igraču.
            orientNormal(
                    collision.normal,
                    player,
                    wall
            );

            // 1. Izbaci igrača iz zida.
            pushPlayerOut(
                    player,
                    collision.normal,
                    collision.depth
            );

            // 2. Ukloni brzinu koja ide prema zidu.
            removeVelocityIntoWall(
                    player,
                    collision.normal
            );
        }
    }

    private static CollisionData findCollision(
            Player player,
            Wall wall) {

        CollisionData best = null;

        for (Polygon playerBox :
                player.getHitbox().getWorldBoxes()) {

            for (Polygon wallBox :
                    wall.getHitbox().getWorldBoxes()) {

                Intersector.MinimumTranslationVector mtv =
                        new Intersector.MinimumTranslationVector();

                boolean overlap =
                        Intersector.overlapConvexPolygons(
                                playerBox,
                                wallBox,
                                mtv
                        );

                if (!overlap) {
                    continue;
                }

                if (best == null ||
                        mtv.depth > best.depth) {

                    best = new CollisionData();

                    best.depth = mtv.depth;
                    best.normal.set(mtv.normal);
                }
            }
        }

        return best;
    }

    private static void orientNormal(
            Vector2 normal,
            Player player,
            Wall wall) {

        float playerX = player.getX();
        float playerY = player.getY();

        float wallCenterX =
                wall.getX() + wall.getWidth() / 2f;

        float wallCenterY =
                wall.getY() + wall.getHeight() / 2f;

        float dx = playerX - wallCenterX;
        float dy = playerY - wallCenterY;

        if (normal.x * dx +
            normal.y * dy < 0f) {

            normal.scl(-1f);
        }
    }

    private static void pushPlayerOut(
            Player player,
            Vector2 normal,
            float depth) {

        float correction =
                depth + SKIN;

        player.setX(
                player.getX() +
                normal.x * correction
        );

        player.setY(
                player.getY() +
                normal.y * correction
        );

        player.updateHitbox();
    }

    private static void removeVelocityIntoWall(
            Player player,
            Vector2 normal) {

        float vx = player.getVelocityX();
        float vy = player.getVelocityY();

        float velocityIntoWall =
                vx * normal.x +
                vy * normal.y;

        /*
         * Ako je velocity u pravcu normale,
         * proveravamo da li ide U zid.
         */
        if (velocityIntoWall < 0f) {

            vx -=
                    normal.x *
                    velocityIntoWall;

            vy -=
                    normal.y *
                    velocityIntoWall;

            player.setVelocity(vx, vy);
        }
    }

    private static class CollisionData {
        float depth;
        Vector2 normal = new Vector2();
    }
}
