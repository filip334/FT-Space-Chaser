package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.world.LanGameDiscovery;
import io.github.filip334.spacechaser.world.MultiplayerClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Prikazuje listu trenutno otvorenih hostovanih igara na LAN mrezi
 * (pronadjenih preko LanGameDiscovery) i omogucava klik za konekciju.
 */
public class JoinGameScreen implements Screen {

    private final Game game;
    private final SpriteBatch batch;
    private final BitmapFont font;

    private final List<LanGameDiscovery.DiscoveredHost> hosts = new CopyOnWriteArrayList<>();
    private final List<Rectangle> rowBounds = new ArrayList<>();
    private final Rectangle refreshButton = new Rectangle();
    private final Rectangle backButton = new Rectangle();

    private volatile boolean scanning = false;
    private boolean connecting = false;

    public JoinGameScreen(Game game) {
        this.game = game;
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.6f);
    }

    @Override
    public void show() {
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
        ScreenUtils.clear(Color.BLACK);

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        rowBounds.clear();
        float rowY = height - 140f;
        float rowHeight = 45f;

        batch.begin();

        font.setColor(Color.WHITE);
        font.draw(batch, "JOIN GAME", 40f, height - 40f);

        String status = scanning ? "Scanning..." : (hosts.isEmpty() ? "No games found" : hosts.size() + " game(s) found");
        font.draw(batch, status, 40f, height - 80f);

        for (LanGameDiscovery.DiscoveredHost host : hosts) {
            Rectangle bounds = new Rectangle(40f, rowY - rowHeight, width - 80f, rowHeight);
            boolean hovered = isMouseOver(bounds);

            font.setColor(hovered ? Color.CYAN : Color.WHITE);
            font.draw(batch, host.hostName + "   (" + host.address + ":" + host.port + ")", 60f, rowY);

            rowBounds.add(bounds);
            rowY -= rowHeight + 10f;
        }

        font.setColor(Color.WHITE);
        font.draw(batch, "Refresh", 40f, 80f);
        font.draw(batch, "Back", 40f, 40f);

        if (connecting) {
            font.draw(batch, "Connecting...", width / 2f - 60f, height / 2f);
        }

        batch.end();

        refreshButton.set(40f, 60f, 100f, 30f);
        backButton.set(40f, 20f, 100f, 30f);

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
            game.setScreen(new MultiplayerScreen(game));
            return;
        }

        for (int i = 0; i < rowBounds.size(); i++) {
            if (rowBounds.get(i).contains(mouseX, mouseY)) {
                connectTo(hosts.get(i));
                return;
            }
        }
    }

    private boolean isMouseOver(Rectangle bounds) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        return bounds.contains(mouseX, mouseY);
    }

    private void connectTo(LanGameDiscovery.DiscoveredHost host) {
        connecting = true;

        Thread connectThread = new Thread(() -> {
            MultiplayerClient client = new MultiplayerClient();
            boolean connected = client.connect(host.address, host.port);

            Gdx.app.postRunnable(() -> {
                if (connected) {
                    game.setScreen(new GameScreen(game, client));
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