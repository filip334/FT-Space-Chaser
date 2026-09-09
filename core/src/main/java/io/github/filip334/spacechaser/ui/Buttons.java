package io.github.filip334.spacechaser.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public final class Buttons {

    private Buttons() {
    }

    // ---------------- DRAW ----------------

    public static void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font,
                             Rectangle bounds, String label, boolean hovered, boolean enabled) {
        UiPanel.draw(shapeRenderer, bounds.x, bounds.y, bounds.width, bounds.height, hovered, enabled);

        GlyphLayout layout = new GlyphLayout(font, label);
        float textX = bounds.x + bounds.width / 2f - layout.width / 2f;
        float textY = bounds.y + bounds.height / 2f + font.getCapHeight() / 2f;

        batch.begin();
        font.setColor(!enabled ? Theme.DISABLED : (hovered ? Theme.WHITE : Theme.CYAN));
        font.draw(batch, label, textX, textY);
        batch.end();
    }

    public static void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font,
                             Rectangle bounds, String label, boolean hovered) {
        draw(batch, shapeRenderer, font, bounds, label, hovered, true);
    }

    // ---------------- LAYOUT ----------------

    public static float layoutStack(Rectangle bounds, float centerX, float topY,
                                     float width, float height, float spacing) {
        bounds.set(centerX - width / 2f, topY - height, width, height);
        return topY - height - spacing;
    }
}
