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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.server.GameServer;

import io.github.filip334.spacechaser.world.MultiplayerClient;

public class MultiplayerScreen implements Screen {

    private final Game game;

    private final SpriteBatch batch;
    private final BitmapFont font;

    private Texture backgroundGradient;

    private final String[] menuItems = {
        "Host Game",
        "Join Game",
        "Server",
        "Back"
    };

    private final Rectangle[] menuBounds;

    private final Color cyan =
        new Color(0.0f, 0.95f, 1.0f, 1.0f);

    private final Color white =
        new Color(1f, 1f, 1f, 1f);

    private float time = 0f;

    public MultiplayerScreen(Game game) {

        this.game = game;

        batch = new SpriteBatch();
        font = new BitmapFont();

        backgroundGradient =
            createGradientTexture();

        menuBounds =
            new Rectangle[menuItems.length];

        for (int i = 0; i < menuBounds.length; i++) {
            menuBounds[i] = new Rectangle();
        }
    }

    @Override
    public void render(float delta) {

        time += delta;

        float width =
            Gdx.graphics.getWidth();

        float height =
            Gdx.graphics.getHeight();

        ScreenUtils.clear(Color.BLACK);

        drawBackground(width, height);
        drawTitle(width, height);
        drawMenu(width, height);

        handleInput(width, height);
    }

    // =========================================================
    // BACKGROUND
    // =========================================================

    private void drawBackground(
        float width,
        float height
    ) {

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

    private void drawTitle(
        float width,
        float height
    ) {

        batch.begin();

        font.getData().setScale(3.0f);

        font.setColor(white);

        String title = "MULTIPLAYER";

        GlyphLayout layout =
            new GlyphLayout(font, title);

        float x =
            width * 0.075f;

        float y =
            height * 0.82f;

        font.draw(
            batch,
            layout,
            x,
            y
        );

        batch.end();
    }

    // =========================================================
    // MENU
    // =========================================================

    private void drawMenu(
        float width,
        float height
    ) {

        batch.begin();

        font.getData().setScale(1.8f);

        float startX =
            width * 0.075f;

        float firstY =
            height * 0.62f;

        float spacing =
            height * 0.11f;

        for (int i = 0; i < menuItems.length; i++) {

            float y;

            if (i < 3) {

                y =
                    firstY - i * spacing;

            } else {

                y =
                    height * 0.17f;
            }

            boolean hovered =
                isMouseOver(
                    startX,
                    y - 40,
                    width * 0.30f,
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

            GlyphLayout layout =
                new GlyphLayout(
                    font,
                    menuItems[i]
                );

            menuBounds[i].set(
                startX,
                y - layout.height,
                layout.width,
                layout.height + 20
            );
        }

        batch.end();
    }

    // =========================================================
    // INPUT
    // =========================================================

    private void handleInput(
        float width,
        float height
    ) {

        if (!Gdx.input.justTouched()) {
            return;
        }

        float mouseX =
            Gdx.input.getX();

        float mouseY =
            height - Gdx.input.getY();

        for (int i = 0;
             i < menuBounds.length;
             i++) {

            if (!menuBounds[i].contains(
                mouseX,
                mouseY
            )) {
                continue;
            }

            switch (i) {

                // =================================================
                // HOST
                // =================================================

                case 0:

                    hostGame();

                    break;


                // =================================================
                // JOIN
                // =================================================

                case 1:

                    joinGame();

                    break;


                // =================================================
                // SERVER
                // =================================================

                case 2:

                    connectToDedicatedServer();

                    break;


                // =================================================
                // BACK
                // =================================================

                case 3:

                    game.setScreen(
                        new MainMenuScreen(game)
                    );

                    break;
            }
        }
    }

    // =========================================================
    // HOST GAME
    // =========================================================

    private void hostGame() {

        System.out.println(
            "Starting host game..."
        );

        // =====================================================
        // POKRENI SERVER
        // =====================================================

        GameServer server =
            new GameServer(5555);

        Thread serverThread =
            new Thread(
                server::start,
                "GameServer"
            );
        
        
        serverThread.setDaemon(true);

        serverThread.start();


        // =====================================================
        // SAČEKAJ DA SERVER POČNE
        // =====================================================

        try {

            Thread.sleep(200);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return;
        }


        // =====================================================
        // POVEŽI HOST KAO PLAYER 1
        // =====================================================

        MultiplayerClient client =
            new MultiplayerClient();

        boolean connected =
            client.connect(
                "127.0.0.1",
                5555
            );

        if (connected) {

            System.out.println(
                "Host connected as Player 1!"
            );

            game.setScreen(
                new GameScreen(
                    game,
                    client
                )
            );

        } else {

            System.out.println(
                "Host could not connect to server."
            );

            server.stop();
        }
    }

    // =========================================================
    // JOIN GAME
    // =========================================================

    private void joinGame() {

        System.out.println(
            "Joining game..."
        );

        MultiplayerClient client =
            new MultiplayerClient();

        boolean connected =
            client.connect(
                "127.0.0.1",
                5555
            );

        if (connected) {

            System.out.println(
                "Joined game!"
            );

            game.setScreen(
                new GameScreen(
                    game,
                    client
                )
            );

        } else {

            System.out.println(
                "Could not join game."
            );
        }
    }

    // =========================================================
    // DEDICATED SERVER
    // =========================================================

    private void connectToDedicatedServer() {

        MultiplayerClient client =
            new MultiplayerClient();

        boolean connected =
            client.connect(
                "127.0.0.1",
                5555
            );

        if (connected) {

            game.setScreen(
                new GameScreen(
                    game,
                    client
                )
            );

        } else {

            System.out.println(
                "Dedicated server unavailable."
            );
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

        float mouseX =
            Gdx.input.getX();

        float mouseY =
            Gdx.graphics.getHeight()
                - Gdx.input.getY();

        return mouseX >= x
            && mouseX <= x + width
            && mouseY >= y
            && mouseY <= y + height;
    }

    // =========================================================
    // GRADIENT
    // =========================================================

    private Texture createGradientTexture() {

        int width = 1024;
        int height = 1;

        Pixmap pixmap =
            new Pixmap(
                width,
                height,
                Pixmap.Format.RGBA8888
            );

        Color left =
            new Color(
                0.005f,
                0.005f,
                0.005f,
                1f
            );

        Color middle =
            new Color(
                0.0f,
                0.15f,
                0.17f,
                1f
            );

        Color right =
            new Color(
                0.0f,
                0.95f,
                1.0f,
                1f
            );

        for (int x = 0; x < width; x++) {

            float t =
                x / (float)(width - 1);

            Color color;

            if (t < 0.65f) {

                float localT =
                    t / 0.65f;

                color =
                    new Color(left)
                        .lerp(
                            middle,
                            localT
                        );

            } else {

                float localT =
                    (t - 0.65f) / 0.35f;

                color =
                    new Color(middle)
                        .lerp(
                            right,
                            localT
                        );
            }

            pixmap.setColor(color);
            pixmap.drawPixel(x, 0);
        }

        Texture texture =
            new Texture(pixmap);

        pixmap.dispose();

        return texture;
    }

    // =========================================================
    // SCREEN
    // =========================================================

    @Override
    public void show() {
    }

    @Override
    public void resize(
        int width,
        int height
    ) {
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
        font.dispose();

        if (backgroundGradient != null) {
            backgroundGradient.dispose();
        }
    }
}