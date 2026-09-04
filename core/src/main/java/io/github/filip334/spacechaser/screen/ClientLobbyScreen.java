package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.server.LobbyStatusMessage;
import io.github.filip334.spacechaser.world.MultiplayerClient;

/** Lobby koji gost vidi nakon konekcije, dok host ne pokrene partiju. */
public class ClientLobbyScreen implements Screen {
    private final Game game;
    private final MultiplayerClient client;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();

    private volatile String hostName = "Host";
    private volatile String gameMode = "2 Players vs AI";
    private volatile boolean gameStarted;

    public ClientLobbyScreen(Game game, MultiplayerClient client) {
        this.game = game;
        this.client = client;
        font.getData().setScale(2f);
        client.setOnLobbyStatus(this::applyLobbyStatus);
    }

    private void applyLobbyStatus(LobbyStatusMessage status) {
        hostName = status.hostName;
        gameMode = status.gameMode;
        gameStarted = status.gameStarted;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(Color.BLACK);
        if (gameStarted) {
            game.setScreen(new GameScreen(game, client));
            return;
        }

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        String title = "LOBBY";
        String mode = "Mode: " + gameMode + " (PvP locked)";
        String host = "Host: " + hostName;
        String waiting = "Waiting for host to start the game...";
        String cancel = "Press ESC to leave";

        batch.begin();
        font.setColor(Color.WHITE);
        drawCentered(title, width, height / 2f + 100f);
        font.getData().setScale(1.3f);
        drawCentered(mode, width, height / 2f + 35f);
        drawCentered(host, width, height / 2f - 5f);
        drawCentered(waiting, width, height / 2f - 70f);
        drawCentered(cancel, width, height / 2f - 120f);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            client.disconnect();
            game.setScreen(new MultiplayerScreen(game));
        }
    }

    private void drawCentered(String text, float width, float y) {
        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, text, width / 2f - layout.width / 2f, y);
    }

    @Override public void show() { }
    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { batch.dispose(); font.dispose(); }
}
