package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import io.github.filip334.spacechaser.network.message.LobbyStatusMessage;
import io.github.filip334.spacechaser.ui.Buttons;
import io.github.filip334.spacechaser.ui.Fonts;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.network.MultiplayerClient;

public class ClientLobbyScreen extends BaseScreen {

    private final MultiplayerClient client;
    private final BitmapFont titleFont;
    private final BitmapFont font;

    private volatile String hostName = "Host";
    private volatile String gameMode = "2 Players vs AI";
    private volatile boolean gameStarted;
    private volatile boolean countingDown;
    private volatile float countdownRemaining;

    private final Rectangle leaveButton = new Rectangle();
    private static final float CORNER_MARGIN = 30f;

    // ---------------- KONSTRUKTOR ----------------

    public ClientLobbyScreen(Game game, MultiplayerClient client) {
        super(game);
        this.client = client;
        titleFont = Fonts.generate(34, Theme.WHITE);
        font = Fonts.generate(20, Theme.WHITE);
        client.setOnLobbyStatus(this::applyLobbyStatus);
    }

    // ---------------- LOBBY STATUS ----------------

    private void applyLobbyStatus(LobbyStatusMessage status) {
        hostName = status.hostName;
        gameMode = status.gameMode;
        gameStarted = status.gameStarted;
        countingDown = status.countingDown;
        countdownRemaining = status.countdownRemaining;
    }

    // ---------------- RENDER ----------------

    @Override
    public void render(float delta) {
        beginFrame();
        if (gameStarted) {
            navigateTo(new GameScreen(game, client));
            return;
        }

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        drawBackground(width, height);

        String title = "LOBBY";
        String mode = "Mode: " + gameMode + " (PvP locked)";
        String host = "Host: " + hostName;
        String status = countingDown
                ? "Starting in " + (int) Math.ceil(countdownRemaining) + "..."
                : "Waiting for host to start the game...";

        batch.begin();
        titleFont.setColor(Theme.WHITE);
        drawCentered(titleFont, title, width, height / 2f + 100f);

        font.setColor(Theme.CYAN);
        drawCentered(font, mode, width, height / 2f + 35f);
        drawCentered(font, host, width, height / 2f - 5f);
        font.setColor(countingDown ? Theme.CYAN : Theme.WHITE);
        drawCentered(font, status, width, height / 2f - 70f);
        batch.end();

        drawLeaveButton();
        handleInput();
    }

    private void drawCentered(BitmapFont f, String text, float width, float y) {
        GlyphLayout layout = new GlyphLayout(f, text);
        f.draw(batch, text, width / 2f - layout.width / 2f, y);
    }

    private void drawLeaveButton() {
        String label = "Leave Match";
        GlyphLayout layout = new GlyphLayout(font, label);
        float w = layout.width + 60f;
        float h = layout.height + 36f;
        leaveButton.set(CORNER_MARGIN, CORNER_MARGIN, w, h);

        boolean hovered = isMouseOver(leaveButton);
        Buttons.draw(batch, shapeRenderer, font, leaveButton, label, hovered);
    }

    // ---------------- INPUT ----------------

    private void handleInput() {
        if ((Gdx.input.justTouched() && isMouseOver(leaveButton))
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            client.disconnect();
            navigateTo(new MultiplayerScreen(game));
        }
    }

    // ---------------- DISPOSE ----------------

    @Override
    public void dispose() {
        super.dispose();
        titleFont.dispose();
        font.dispose();
    }
}
