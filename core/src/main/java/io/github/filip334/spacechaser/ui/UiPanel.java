package io.github.filip334.spacechaser.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Neonski "staklen" panel - providna tamno-cyan unutrasnjost + svetleca cyan
 * kontura sa blagim glow-om (nekoliko sve vecih i providnijih slojeva obruba,
 * najjeftinija aproksimacija stvarnog blur-a bez shadera/framebuffer-a).
 * Koristi se za sve dugmad/okvire u meniju - poziva se IZMEDJU
 * sr.begin(Filled)/sr.end() poziva koje pravi pozivalac nije potrebno raditi,
 * draw() sam upravlja begin/end i blend stanjem.
 */
public final class UiPanel {

    private static final float[] GLOW_EXPAND = {10f, 6f, 3f};
    private static final float[] GLOW_ALPHA = {0.05f, 0.10f, 0.18f};
    private static final float BORDER_THICKNESS = 2.5f;

    private UiPanel() {
    }

    // ---------------- DRAW ----------------

    public static void draw(ShapeRenderer sr, float x, float y, float w, float h, boolean hovered) {
        draw(sr, x, y, w, h, hovered, true);
    }

    /** @param enabled false iscrtava prigusenu sivu verziju bez glow-a (npr. Start dok ne udje drugi igrac). */
    public static void draw(ShapeRenderer sr, float x, float y, float w, float h, boolean hovered, boolean enabled) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);

        if (!enabled) {
            sr.setColor(0.15f, 0.15f, 0.15f, 0.5f);
            sr.rect(x, y, w, h);
            sr.setColor(Theme.DISABLED);
            drawBorder(sr, x, y, w, h, 0f);
            sr.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            return;
        }

        // providna "staklena" unutrasnjost
        sr.setColor(0f, hovered ? 0.22f : 0.10f, hovered ? 0.26f : 0.12f, 0.55f);
        sr.rect(x, y, w, h);

        // spoljni glow - slojevi sve veci i providniji
        for (int i = 0; i < GLOW_EXPAND.length; i++) {
            sr.setColor(Theme.CYAN.r, Theme.CYAN.g, Theme.CYAN.b, GLOW_ALPHA[i]);
            drawBorder(sr, x, y, w, h, GLOW_EXPAND[i]);
        }

        // ostra kontura
        Color borderColor = hovered ? Theme.WHITE : Theme.CYAN;
        sr.setColor(borderColor);
        drawBorder(sr, x, y, w, h, 0f);

        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ---------------- BORDER ----------------

    private static void drawBorder(ShapeRenderer sr, float x, float y, float w, float h, float expand) {
        float bx = x - expand;
        float by = y - expand;
        float bw = w + expand * 2f;
        float bh = h + expand * 2f;
        float t = BORDER_THICKNESS + expand * 0.4f;

        sr.rect(bx, by, bw, t);
        sr.rect(bx, by + bh - t, bw, t);
        sr.rect(bx, by, t, bh);
        sr.rect(bx + bw - t, by, t, bh);
    }
}
