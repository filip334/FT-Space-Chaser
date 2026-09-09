package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Coin;
import io.github.filip334.spacechaser.entity.Wall;

public class CoinSpawner {

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

    private final EncounterField encounterField;
    private int nextCoinId = 1;

    // ---------------- KONSTRUKTOR ----------------

    public CoinSpawner(EncounterField encounterField) {
        this.encounterField = encounterField;
    }

    // ---------------- SPAWN ----------------

    public void spawnCoins(Array<Coin> coins) {
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
}
