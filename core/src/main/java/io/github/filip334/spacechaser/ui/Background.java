package io.github.filip334.spacechaser.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public final class Background {

    private Background() {
    }

    public static Texture createGradientTexture() {
        int width = 1024;
        int height = 1;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        Color left = new Color(0.01f, 0.05f, 0.06f, 1f);
        Color middle = new Color(0.0f, 0.16f, 0.19f, 1f);
        Color right = new Color(0.0f, 0.55f, 0.62f, 1f);

        for (int x = 0; x < width; x++) {
            float t = x / (float) (width - 1);
            Color color;
            if (t < 0.65f) {
                color = new Color(left).lerp(middle, t / 0.65f);
            } else {
                color = new Color(middle).lerp(right, (t - 0.65f) / 0.35f);
            }
            pixmap.setColor(color);
            pixmap.drawPixel(x, 0);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
}
