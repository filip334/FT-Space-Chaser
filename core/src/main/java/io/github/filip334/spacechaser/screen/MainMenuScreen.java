package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MainMenuScreen implements Screen{
    
    //
    private Game game;
    private Stage stage;
    private Skin skin;
    
    //
    public MainMenuScreen(Game game) {
        this.game = game;

        stage = new Stage(new ScreenViewport());
        skin = createSkin();

        Label title = new Label("SPACE CHASER", skin);
        title.setFontScale(3.2f);
        title.setColor(new Color(0.35f, 0.85f, 1f, 1f));

        Label subtitle = new Label("SURVIVE THE ENEMY FLEET", skin);
        subtitle.setFontScale(1.15f);
        subtitle.setColor(new Color(0.68f, 0.78f, 0.9f, 1f));

        TextButton startButton = new TextButton("START MISSION", skin, "primary");
        TextButton exitButton = new TextButton("EXIT GAME", skin, "secondary");
        Label controls = new Label("WASD  Move     SHIFT  Boost     SPACE  Fire", skin);
        controls.setColor(new Color(0.58f, 0.68f, 0.8f, 1f));

        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
            }
        });

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        Table root = new Table();
        root.setFillParent(true);
        root.background(skin.getDrawable("screen-background"));

        Table card = new Table();
        card.background(skin.getDrawable("menu-card"));
        card.pad(42f, 64f, 38f, 64f);

        card.add(title).padBottom(12f);
        card.row();
        card.add(subtitle).padBottom(44f);
        card.row();
        card.add(startButton).width(300f).height(62f).padBottom(14f);
        card.row();
        card.add(exitButton).width(300f).height(52f).padBottom(34f);
        card.row();
        card.add(controls);

        root.add(card);
        stage.addActor(root);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    private Skin createSkin() {
        Skin menuSkin = new Skin();
        BitmapFont font = new BitmapFont();
        menuSkin.add("default-font", font);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        menuSkin.add("default", labelStyle);

        Texture backgroundTexture = createColorTexture(new Color(0.015f, 0.025f, 0.07f, 1f));
        Texture cardTexture = createColorTexture(new Color(0.055f, 0.09f, 0.16f, 0.96f));
        Texture primaryUpTexture = createColorTexture(new Color(0.08f, 0.42f, 0.68f, 1f));
        Texture primaryDownTexture = createColorTexture(new Color(0.16f, 0.62f, 0.9f, 1f));
        Texture secondaryUpTexture = createColorTexture(new Color(0.18f, 0.22f, 0.3f, 1f));
        Texture secondaryDownTexture = createColorTexture(new Color(0.32f, 0.16f, 0.2f, 1f));
        menuSkin.add("screen-background", backgroundTexture);
        menuSkin.add("menu-card", cardTexture);
        menuSkin.add("primary-up", primaryUpTexture);
        menuSkin.add("primary-down", primaryDownTexture);
        menuSkin.add("secondary-up", secondaryUpTexture);
        menuSkin.add("secondary-down", secondaryDownTexture);

        TextButton.TextButtonStyle primaryStyle = createButtonStyle(font, primaryUpTexture, primaryDownTexture);
        TextButton.TextButtonStyle secondaryStyle = createButtonStyle(font, secondaryUpTexture, secondaryDownTexture);
        menuSkin.add("primary", primaryStyle);
        menuSkin.add("secondary", secondaryStyle);
        return menuSkin;
    }

    private TextButton.TextButtonStyle createButtonStyle(BitmapFont font, Texture upTexture, Texture downTexture) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up = new TextureRegionDrawable(new TextureRegion(upTexture));
        style.down = new TextureRegionDrawable(new TextureRegion(downTexture));
        style.font = font;
        style.fontColor = Color.WHITE;
        style.downFontColor = new Color(0.02f, 0.06f, 0.1f, 1f);
        return style;
    }

    private Texture createColorTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
}
