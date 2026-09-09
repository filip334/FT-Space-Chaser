package io.github.filip334.spacechaser.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Dugme "u okviru" (UiPanel pozadina + centriran tekst, cyan/belo prema
 * hover-u) - isti oblik se ponavljao skoro identican u svakom ekranu
 * (pauza, game over, host/join/client lobby, settings...).
 */
public final class Buttons {

    private Buttons() {
    }

    // ---------------- DRAW ----------------

    /** Crta dugme unutar vec postavljenih bounds. enabled=false crta prigusenu/neklikabilnu verziju (vidi UiPanel). */
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

    /**
     * SAMO postavlja bounds (fiksna sirina/visina, centrirano na centerX,
     * gornja ivica na topY) i vraca topY sledeceg dugmeta ispod - za
     * vertikalan stek dugmadi iste velicine (npr. pauza/game-over meni).
     * Namerno odvojeno od draw() - pozivalac mora prvo da postavi bounds pa
     * TEK ONDA da racuna hover (isMouseOver koristi vec postavljene bounds).
     */
    public static float layoutStack(Rectangle bounds, float centerX, float topY,
                                     float width, float height, float spacing) {
        bounds.set(centerX - width / 2f, topY - height, width, height);
        return topY - height - spacing;
    }
}
