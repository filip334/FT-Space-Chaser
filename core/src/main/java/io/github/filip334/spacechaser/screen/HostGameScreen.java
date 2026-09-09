package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import io.github.filip334.spacechaser.server.GameServer;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.ui.Buttons;
import io.github.filip334.spacechaser.ui.Fonts;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.network.LanHostAdvertiser;
import io.github.filip334.spacechaser.network.MultiplayerClient;

public class HostGameScreen extends BaseScreen {

    private static final int MAX_PLAYERS = 2;

    private final BitmapFont font;
    private final BitmapFont buttonFont;

    private GameServer server;
    private LanHostAdvertiser advertiser;
    private MultiplayerClient client;

    private boolean started = false;
    private boolean leftScreen = false;
    private float dots = 0f;
    private final String hostName;

    private final Rectangle startButton = new Rectangle();
    private final Rectangle backButton = new Rectangle();
    private static final float CORNER_MARGIN = 30f;

    // ---------------- KONSTRUKTOR ----------------

    public HostGameScreen(Game game) {
        super(game);
        font = Fonts.generate(26, Theme.WHITE);
        buttonFont = Fonts.generate(24, Theme.WHITE);
        hostName = ((SpaceChaserGame) game).getSettings().getPlayerName();
    }

    // ---------------- LOBBY SETUP ----------------

    @Override
    public void show() {
        super.show();

        server = new GameServer(0);

        Thread serverThread = new Thread(server::start, "GameServer");
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(200);
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

    // ---------------- RENDER ----------------

    @Override
    public void render(float delta) {
        beginFrame();

        if (leftScreen) return;

        dots += delta;

        if (server.isGameStarted()) {
            proceedToGame();
            return;
        }

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        drawBackground(width, height);
        drawWaitingText();
        drawBackButton();
        handleInput();
    }

    private void proceedToGame() {
        leftScreen = true;
        if (advertiser != null) advertiser.stop();
        navigateTo(new GameScreen(game, client, server));
    }

    private void drawWaitingText() {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        int dotCount = ((int) (dots * 2)) % 4;
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < dotCount; i++) suffix.append(".");

        String line1 = "LOBBY - 2 PLAYERS VS AI (PVP LOCKED)";
        String line2 = server != null
                ? "Host: " + hostName + " | Guest: " + server.getGuestName()
                : "";

        batch.begin();
        font.setColor(Theme.CYAN);
        GlyphLayout layout1 = new GlyphLayout(font, line1);
        font.draw(batch, line1, width / 2f - layout1.width / 2f, height / 2f + 60f);

        font.setColor(Theme.WHITE);
        GlyphLayout layout2 = new GlyphLayout(font, line2);
        font.draw(batch, line2, width / 2f - layout2.width / 2f, height / 2f + 10f);
        batch.end();

        boolean ready = started && server.getConnectedPlayerCount() >= MAX_PLAYERS;

        if (server.isCountingDown()) {
            drawCenteredLine("Starting in " + (int) Math.ceil(server.getCountdownRemaining()) + "...", width, height, Theme.CYAN, -60f);
        } else {
            drawStartButton(width, height, ready);
            if (!ready) {
                drawCenteredLine("Waiting for opponent" + suffix, width, height, Theme.WHITE, -130f);
            }
        }
    }

    private void drawCenteredLine(String text, float width, float height, Color color, float yOffset) {
        batch.begin();
        GlyphLayout layout = new GlyphLayout(font, text);
        font.setColor(color);
        font.draw(batch, text, width / 2f - layout.width / 2f, height / 2f + yOffset);
        batch.end();
    }

    private void drawStartButton(float width, float height, boolean enabled) {
        String label = "START";
        GlyphLayout layout = new GlyphLayout(buttonFont, label);
        float centerX = width / 2f;
        float y = height / 2f - 60f;
        startButton.set(centerX - layout.width / 2f - 40f, y - layout.height / 2f - 18f,
                layout.width + 80f, layout.height + 36f);

        boolean hovered = enabled && isMouseOver(startButton);
        Buttons.draw(batch, shapeRenderer, buttonFont, startButton, label, hovered, enabled);
    }

    private void drawBackButton() {
        String label = "Back";
        GlyphLayout layout = new GlyphLayout(buttonFont, label);
        float w = layout.width + 60f;
        float h = layout.height + 36f;
        backButton.set(CORNER_MARGIN, CORNER_MARGIN, w, h);

        boolean hovered = isMouseOver(backButton);
        Buttons.draw(batch, shapeRenderer, buttonFont, backButton, label, hovered);
    }

    // ---------------- INPUT ----------------

    private void handleInput() {
        boolean ready = started && server.getConnectedPlayerCount() >= MAX_PLAYERS;

        if (Gdx.input.justTouched()) {
            if (isMouseOver(backButton)) {
                cancelAndGoBack();
                return;
            }
            if (!server.isCountingDown() && ready && isMouseOver(startButton)) {
                server.requestStartCountdown();
                return;
            }
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

        navigateTo(new MultiplayerScreen(game));
    }

    // ---------------- DISPOSE ----------------

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        buttonFont.dispose();
    }
}
