package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.world.MultiplayerClient;

public class MainMenuScreen implements Screen {

    private final Game game;
    
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;

    private Texture backgroundGradient;
    
    private Texture shipTexture;

    // Menu opcije
    private final String[] menuItems = {
        "Singleplayer",
        "Multiplayer",
        "Settings",
        "Exit"
    };

    private final Rectangle[] menuBounds;

    // Boje
    private final Color cyan = new Color(0.0f, 0.95f, 1.0f, 1.0f);
    private final Color darkCyan = new Color(0.0f, 0.25f, 0.28f, 1.0f);
    private final Color white = new Color(1f, 1f, 1f, 1f);

    // Animacija glow-a
    private float time = 0f;

    public MainMenuScreen(Game game) {

        this.game = game;
        
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        backgroundGradient = createGradientTexture();
        
        /*
         * Ovde stavi svoj spaceship.
         *
         * npr:
         * assets/ship.png
         */
        shipTexture = new Texture(Gdx.files.internal("Original/ship.png"));

        menuBounds = new Rectangle[menuItems.length];

        for (int i = 0; i < menuBounds.length; i++) {
            menuBounds[i] = new Rectangle();
        }
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {

        time += delta;

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        ScreenUtils.clear(Color.BLACK);

        drawBackground(width, height);
        drawTitle(width, height);
        drawShip(width, height);
        drawMenu(width, height);

        handleInput(width, height);
    }

    private Texture createGradientTexture() {

        int width = 1024;
        int height = 1;

        Pixmap pixmap = new Pixmap(
            width,
            height,
            Pixmap.Format.RGBA8888
        );

        Color left = new Color(
            0.005f,
            0.005f,
            0.005f,
            1f
        );

        Color middle = new Color(
            0.0f,
            0.15f,
            0.17f,
            1f
        );

        Color right = new Color(
            0.0f,
            0.95f,
            1.0f,
            1f
        );

        for (int x = 0; x < width; x++) {

            float t = x / (float)(width - 1);

            Color color;

            if (t < 0.65f) {

                float localT = t / 0.65f;

                color = new Color(
                    left
                ).lerp(
                    middle,
                    localT
                );

            } else {

                float localT = (t - 0.65f) / 0.35f;

                color = new Color(
                    middle
                ).lerp(
                    right,
                    localT
                );
            }

            pixmap.setColor(color);
            pixmap.drawPixel(x, 0);
        }

        Texture texture = new Texture(pixmap);

        pixmap.dispose();

        return texture;
    }
    
    // =========================================================
    // BACKGROUND
    // =========================================================

    private void drawBackground(float width, float height) {

        batch.begin();

        batch.setColor(Color.WHITE);

        batch.draw(
            backgroundGradient,
            0,
            0,
            width,
            height
        );

        batch.end();
    }

    // =========================================================
    // TITLE
    // =========================================================

    private void drawTitle(float width, float height) {

        batch.begin();

        font.getData().setScale(3.0f);
        font.setColor(white);

        String title = "FT - SPACE CHASER";

        GlyphLayout layout = new GlyphLayout(font, title);

        float x = width * 0.035f;
        float y = height * 0.91f;

        font.draw(
            batch,
            layout,
            x,
            y
        );

        batch.end();
    }

    // =========================================================
    // SHIP
    // =========================================================

    private void drawShip(float width, float height) {

        batch.begin();

        /*
         * Veličina broda zavisi od visine ekrana.
         */
        float shipHeight = height * 0.72f;

        float ratio =
            shipTexture.getWidth() /
            (float) shipTexture.getHeight();

        float shipWidth = shipHeight * ratio;

        float x = width * 0.55f;
        float y = height * 0.14f;

        /*
         * Blagi cyan glow iza broda.
         *
         * Ovde koristimo jednostavan alpha overlay.
         */

        batch.setColor(
            0.0f,
            0.9f,
            1.0f,
            0.10f
        );

        batch.draw(
            shipTexture,
            x,
            y,
            shipWidth / 2f,   // originX
            shipHeight / 2f,  // originY
            shipWidth,
            shipHeight,
            1f,
            1f,
            -90f,             // rotacija 90° udesno
            0,
            0,
            shipTexture.getWidth(),
            shipTexture.getHeight(),
            false,
            false
        );

        /*
         * Pravi brod
         */
        batch.setColor(Color.WHITE);

        batch.draw(
            shipTexture,
            x,
            y,
            shipWidth / 2f,   // originX
            shipHeight / 2f,  // originY
            shipWidth,
            shipHeight,
            1f,
            1f,
            -90f,             // rotacija 90° udesno
            0,
            0,
            shipTexture.getWidth(),
            shipTexture.getHeight(),
            false,
            false
        );

        batch.setColor(Color.WHITE);

        batch.end();
    }

    // =========================================================
    // MENU
    // =========================================================

    private void drawMenu(float width, float height) {

        batch.begin();

        font.getData().setScale(1.8f);

        float startX = width * 0.075f;

        /*
         * Prve tri opcije
         */
        float firstY = height * 0.66f;
        float spacing = height * 0.105f;

        for (int i = 0; i < menuItems.length; i++) {

            float y;

            if (i < 3) {
                y = firstY - i * spacing;
            } else {
                /*
                 * Exit je odvojen od ostalih
                 */
                y = height * 0.17f;
            }

            boolean hovered =
                isMouseOver(
                    startX,
                    y - 40,
                    width * 0.20f,
                    60
                );

            if (hovered) {
                font.setColor(cyan);
            } else {
                font.setColor(white);
            }

            font.draw(
                batch,
                menuItems[i],
                startX,
                y
            );

            /*
             * Zapamti bounding box opcije
             */
            GlyphLayout layout =
                new GlyphLayout(font, menuItems[i]);

            menuBounds[i].set(
                startX,
                y - layout.height,
                layout.width,
                layout.height + 20
            );
        }

        batch.end();

        drawMenuLines(width, height);
    }

    // =========================================================
    // MENU LINES
    // =========================================================

    private void drawMenuLines(float width, float height) {

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float lineX = width * 0.06f;
        float lineWidth = width * 0.18f;

        float firstY = height * 0.635f;
        float spacing = height * 0.105f;

        for (int i = 0; i < 3; i++) {

            float y = firstY - i * spacing;

            boolean hovered =
                isMouseOver(
                    width * 0.075f,
                    y - 20,
                    width * 0.20f,
                    60
                );

            if (hovered) {

                /*
                 * Glow
                 */
                shapeRenderer.setColor(
                    new Color(0f, 1f, 1f, 0.20f)
                );

                shapeRenderer.rect(
                    lineX,
                    y - 3,
                    lineWidth,
                    8
                );

                shapeRenderer.setColor(cyan);

            } else {

                shapeRenderer.setColor(
                    new Color(0f, 0.85f, 0.9f, 0.85f)
                );
            }

            /*
             * Horizontalna linija
             */
            shapeRenderer.rect(
                lineX,
                y,
                lineWidth,
                2
            );

            /*
             * Dijagonalni završetak
             */
            shapeRenderer.rect(
                lineX + lineWidth,
                y,
                25,
                2
            );
        }

        /*
         * Exit linija
         */
        float exitY = height * 0.145f;

        shapeRenderer.setColor(
            new Color(0f, 0.85f, 0.9f, 0.85f)
        );

        shapeRenderer.rect(
            lineX,
            exitY,
            lineWidth,
            2
        );

        shapeRenderer.rect(
            lineX + lineWidth,
            exitY,
            25,
            2
        );

        shapeRenderer.end();
    }

    // =========================================================
    // INPUT
    // =========================================================

    private void handleInput(float width, float height) {

        if (!Gdx.input.justTouched()) {
            return;
        }

        float mouseX = Gdx.input.getX();

        /*
         * LibGDX mouse Y ide od vrha ekrana,
         * dok naše koordinate idu od dna.
         */
        float mouseY =
            height - Gdx.input.getY();

        for (int i = 0; i < menuBounds.length; i++) {

            if (menuBounds[i].contains(mouseX, mouseY)) {

                switch (i) {

                    case 0:
                        game.setScreen(new GameScreen(game));
                        /*
                         * Ovde kasnije:
                         *
                         * ((Game) Gdx.app.getApplicationListener())
                         *     .setScreen(new GameScreen(...));
                         */
                        break;

                    case 1:

                        game.setScreen(
                            new MultiplayerScreen(game)
                        );

                        break;

                    case 2:
                        System.out.println(
                            "Settings selected"
                        );
                        break;

                    case 3:
                        System.out.println(
                            "Exit selected"
                        );

                        Gdx.app.exit();
                        break;
                }
            }
        }
    }

    // =========================================================
    // HOVER
    // =========================================================

    private boolean isMouseOver(
        float x,
        float y,
        float width,
        float height
    ) {

        float mouseX = Gdx.input.getX();
        float mouseY =
            Gdx.graphics.getHeight() - Gdx.input.getY();

        return mouseX >= x
            && mouseX <= x + width
            && mouseY >= y
            && mouseY <= y + height;
    }

    // =========================================================
    // SCREEN METHODS
    // =========================================================

    @Override
    public void resize(int width, int height) {
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

        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();

        if (shipTexture != null) {
            shipTexture.dispose();
        }
    }
}