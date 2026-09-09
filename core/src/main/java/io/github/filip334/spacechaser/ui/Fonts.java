package io.github.filip334.spacechaser.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public final class Fonts {

    private static final String FONT_PATH = "fonts/Audiowide-Regular.ttf";

    private Fonts() {
    }

    public static BitmapFont generate(int sizePx, Color color) {
        return generate(sizePx, color, 0, null);
    }

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
