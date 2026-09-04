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
import io.github.filip334.spacechaser.server.GameServer;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.world.LanHostAdvertiser;
import io.github.filip334.spacechaser.world.MultiplayerClient;

/**
 * Ekran na kome host ceka da se pridruzi drugi igrac.
 * Pokrece sopstveni GameServer (na slobodnom, OS-dodeljenom portu, da izbegne
 * sudar sa drugim host-ovima na istoj masini) + UDP oglasivac da bi Join Game
 * ekran mogao da ga pronadje.
 */
public class HostGameScreen implements Screen {

    private static final int MAX_PLAYERS = 2;

    private final Game game;
    private final SpriteBatch batch;
    private final BitmapFont font;

    private GameServer server;
    private LanHostAdvertiser advertiser;
    private MultiplayerClient client;

    private boolean started = false;
    private boolean leftScreen = false;
    private float dots = 0f;
    private final String hostName;

    public HostGameScreen(Game game) {
        this.game = game;
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2f);
        hostName = ((SpaceChaserGame) game).getSettings().getPlayerName();
    }

    @Override
    public void show() {
        server = new GameServer(0); // 0 = OS bira slobodan port

        Thread serverThread = new Thread(server::start, "GameServer");
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(200); // sacekaj da server zavrsi bind pre nego sto se host konektuje
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int port = server.getPort();

        advertiser = new LanHostAdvertiser(port, hostName, "2 Players vs AI");
        advertiser.start();

        client = new MultiplayerClient(hostName);
        boolean connected = client.connect("127.0.0.1", port);

        if (connected) {
            started = true;
        } else {
            System.out.println("Host ne moze da se konektuje na sopstveni server.");
            cancelAndGoBack();
        }
    }

    private String resolveHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Host";
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(Color.BLACK);

        if (leftScreen) return;

        dots += delta;

        drawWaitingText();
        handleInput();
    }

    private void proceedToGame() {
        leftScreen = true;
        if (advertiser != null) advertiser.stop(); // igra je puna, prestani da se oglasavas
        game.setScreen(new GameScreen(game, client));
    }

    private void drawWaitingText() {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        int dotCount = ((int) (dots * 2)) % 4;
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < dotCount; i++) suffix.append(".");

        String line1 = "LOBBY - 2 Players vs AI (PvP locked)";
        String line2 = server != null
                ? "Host: " + hostName + " | Guest: " + server.getGuestName()
                : "";
        String line3 = server != null && server.getConnectedPlayerCount() >= MAX_PLAYERS
                ? "Press ENTER to start game" : "Waiting for opponent" + suffix;

        batch.begin();

        GlyphLayout layout1 = new GlyphLayout(font, line1);
        font.draw(batch, line1, width / 2f - layout1.width / 2f, height / 2f + 40f);

        GlyphLayout layout2 = new GlyphLayout(font, line2);
        font.draw(batch, line2, width / 2f - layout2.width / 2f, height / 2f);

        GlyphLayout layout3 = new GlyphLayout(font, line3);
        font.draw(batch, line3, width / 2f - layout3.width / 2f, height / 2f - 60f);

        batch.end();
    }

    private void handleInput() {
        if (started && server.getConnectedPlayerCount() >= MAX_PLAYERS
                && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            server.startGame();
            proceedToGame();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            cancelAndGoBack();
        }
    }

    private void cancelAndGoBack() {
        if (leftScreen) return;
        leftScreen = true;

        if (advertiser != null) advertiser.stop();
        if (client != null) client.disconnect();
        if (server != null) server.stop();

        game.setScreen(new MultiplayerScreen(game));
    }

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
        font.dispose();
    }
}
