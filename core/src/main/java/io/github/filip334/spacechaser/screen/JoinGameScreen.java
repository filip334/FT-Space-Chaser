package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import io.github.filip334.spacechaser.network.LanGameDiscovery;
import io.github.filip334.spacechaser.network.MultiplayerClient;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.ui.Buttons;
import io.github.filip334.spacechaser.ui.Fonts;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.ui.UiPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Prikazuje listu trenutno otvorenih hostovanih igara na LAN mrezi
 * (pronadjenih preko LanGameDiscovery) i omogucava klik za konekciju.
 */
public class JoinGameScreen extends BaseScreen {

    private final BitmapFont titleFont;
    private final BitmapFont font;

    private final List<LanGameDiscovery.DiscoveredHost> hosts = new CopyOnWriteArrayList<>();
    private final List<Rectangle> rowBounds = new ArrayList<>();
    private final Rectangle refreshButton = new Rectangle();
    private final Rectangle backButton = new Rectangle();

    private volatile boolean scanning = false;
    private boolean connecting = false;

    public JoinGameScreen(Game game) {
        super(game);
        titleFont = Fonts.generate(30, Theme.WHITE);
        font = Fonts.generate(18, Theme.WHITE);
    }

    @Override
    public void show() {
        super.show();
        startScan();
    }

    private void startScan() {
        if (scanning) return;
        scanning = true;
        hosts.clear();

        Thread scanThread = new Thread(() -> {
            LanGameDiscovery discovery = new LanGameDiscovery();
            List<LanGameDiscovery.DiscoveredHost> found = discovery.scan(1500);
            hosts.addAll(found);
            scanning = false;
        }, "LanGameDiscovery-Scan");
        scanThread.setDaemon(true);
        scanThread.start();
    }

    @Override
    public void render(float delta) {
        beginFrame();

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        drawBackground(width, height);

        rowBounds.clear();
        float rowY = height - 150f;
        float rowHeight = 50f;

        batch.begin();
        titleFont.setColor(Theme.WHITE);
        titleFont.draw(batch, "JOIN GAME", 40f, height - 40f);

        String status = scanning ? "Scanning..." : (hosts.isEmpty() ? "No games found" : hosts.size() + " game(s) found");
        font.setColor(Theme.CYAN);
        font.draw(batch, status, 40f, height - 90f);
        batch.end();

        for (LanGameDiscovery.DiscoveredHost host : hosts) {
            Rectangle bounds = new Rectangle(40f, rowY - rowHeight, width - 80f, rowHeight);
            boolean hovered = isMouseOver(bounds);

            UiPanel.draw(shapeRenderer, bounds.x, bounds.y, bounds.width, bounds.height, hovered);

            batch.begin();
            font.setColor(hovered ? Theme.WHITE : Theme.CYAN);
            font.draw(batch, host.hostName + " - " + host.gameMode + "   (" + host.address + ":" + host.port + ")",
                    bounds.x + 16f, rowY - rowHeight / 2f + 8f);
            batch.end();

            rowBounds.add(bounds);
            rowY -= rowHeight + 14f;
        }

        // Back je "izlazna" akcija - veci razmak od Refresh nego sto bi ga
        // odvajao od bilo kog drugog susednog dugmeta.
        float refreshWidth = 130f;
        float backExtraGap = 70f;
        refreshButton.set(40f, 70f, refreshWidth, 44f);
        backButton.set(40f + refreshWidth + backExtraGap, 70f, 110f, 44f);

        Buttons.draw(batch, shapeRenderer, font, refreshButton, "Refresh", isMouseOver(refreshButton));
        Buttons.draw(batch, shapeRenderer, font, backButton, "Back", isMouseOver(backButton));

        if (connecting) {
            batch.begin();
            font.setColor(Theme.WHITE);
            font.draw(batch, "Connecting...", width / 2f - 60f, height / 2f);
            batch.end();
        }

        handleInput();
    }

    private void handleInput() {
        if (connecting) return;
        if (!Gdx.input.justTouched()) return;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        if (refreshButton.contains(mouseX, mouseY)) {
            startScan();
            return;
        }

        if (backButton.contains(mouseX, mouseY)) {
            navigateTo(new MultiplayerScreen(game));
            return;
        }

        for (int i = 0; i < rowBounds.size(); i++) {
            if (rowBounds.get(i).contains(mouseX, mouseY)) {
                connectTo(hosts.get(i));
                return;
            }
        }
    }

    private void connectTo(LanGameDiscovery.DiscoveredHost host) {
        connecting = true;

        Thread connectThread = new Thread(() -> {
            MultiplayerClient client = new MultiplayerClient(((SpaceChaserGame) game).getSettings().getPlayerName());
            boolean connected = client.connect(host.address, host.port);

            Gdx.app.postRunnable(() -> {
                if (connected) {
                    navigateTo(new ClientLobbyScreen(game, client));
                } else {
                    connecting = false;
                    System.out.println("Ne mogu da se konektujem na " + host.address + ":" + host.port);
                }
            });
        }, "JoinGame-Connect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    @Override
    public void dispose() {
        super.dispose();
        titleFont.dispose();
        font.dispose();
    }
}
