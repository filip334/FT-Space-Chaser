package io.github.filip334.spacechaser.util;

import com.badlogic.gdx.math.Vector2;

public final class RotationUtils {

    private RotationUtils() {
    }

    public static Vector2 rotateOffset(float originX, float originY,
                                        float localOffsetX, float localOffsetY, float angleDeg) {
        float rad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float worldX = originX + localOffsetX * cos - localOffsetY * sin;
        float worldY = originY + localOffsetX * sin + localOffsetY * cos;
        return new Vector2(worldX, worldY);
    }
}
