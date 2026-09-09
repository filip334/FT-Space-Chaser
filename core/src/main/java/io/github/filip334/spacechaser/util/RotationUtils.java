package io.github.filip334.spacechaser.util;

import com.badlogic.gdx.math.Vector2;

/**
 * Deljena geometrija za crtanje - rotira lokalni pomeraj (u odnosu na centar/
 * nos entiteta, npr. poziciju motora) za dati ugao i vraca svetsku poziciju.
 * Ista formula je ranije bila zasebno napisana i u EntityRenderer (plamen
 * motora broda/igraca) i u ChaseArt (plamen motora neprijatelja u meniju).
 */
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
