package io.github.filip334.spacechaser.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * Generise BitmapFont iz Audiowide.ttf TACNO na trazenoj velicini (FreeType),
 * umesto da se LibGDX-ov ugradjeni placeholder font (Arial 15px) rastegne
 * preko setScale() - to je razlog zasto je tekst ranije bio pikselizovan na
 * krupnim velicinama i nejasan na sitnim.
 */
public final class Fonts {

    private static final String FONT_PATH = "fonts/Audiowide-Regular.ttf";

    private Fonts() {
    }

    public static BitmapFont generate(int sizePx, Color color) {
        return generate(sizePx, color, 0, null);
    }

    /** @param borderWidth 0 = bez konture. borderColor moze biti null ako je borderWidth 0. */
    public static BitmapFont generate(int sizePx, Color color, float borderWidth, Color borderColor) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_PATH));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = sizePx;
        param.color = color;
        param.borderWidth = borderWidth;
        param.borderColor = borderColor;
        param.minFilter = Texture.TextureFilter.Linear;
        param.magFilter = Texture.TextureFilter.Linear;
        param.kerning = true;

        BitmapFont font = generator.generateFont(param);
        generator.dispose();
        return font;
    }
}
