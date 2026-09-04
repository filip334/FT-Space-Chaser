package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.renderer.HudRenderer;
import io.github.filip334.spacechaser.server.PlayerInputMessage;
import io.github.filip334.spacechaser.world.GameWorld;
import io.github.filip334.spacechaser.world.GameSettings;
import io.github.filip334.spacechaser.world.GameLayout;
import io.github.filip334.spacechaser.world.MultiplayerClient;
import io.github.filip334.spacechaser.world.MultiplayerGameWorld;

public class GameScreen implements Screen {

    // Kada se prozor minimizuje ili izgubi fokus, sledeci frame moze imati
    // delta od vise sekundi. Ne dozvoljavamo da taj jedan frame pomeri svet.
    private static final float MAX_SIMULATION_DELTA = 1f / 30f;

    private final Game game;
    private final SpriteBatch batch;

    // SINGLEPLAYER
    private GameWorld world;

    // MULTIPLAYER
    private MultiplayerClient multiplayerClient;
    private MultiplayerGameWorld multiplayerWorld;
    private HudRenderer hudRenderer;
    private final GameSettings settings;
    private final Matrix4 worldTransform = new Matrix4().setToTranslation(GameLayout.WORLD_OFFSET_X, 0f, 0f);
    private final Matrix4 identityTransform = new Matrix4();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(GameLayout.WINDOW_WIDTH, GameLayout.WINDOW_HEIGHT, camera);
    private volatile boolean serverDisconnected;
    private boolean returningToMenu;

    public GameScreen(Game game) {
        this.game = game;
        settings = ((SpaceChaserGame) game).getSettings();
        batch = new SpriteBatch();
        world = new GameWorld(game, settings);
    }

    public GameScreen(Game game, MultiplayerClient multiplayerClient) {
        this.game = game;
        settings = ((SpaceChaserGame) game).getSettings();
        this.multiplayerClient = multiplayerClient;
        batch = new SpriteBatch();

        Texture playerIdleTexture = new Texture("Original/shipPixel.png");
        Texture flameSheet = new Texture("Original/moveFire.png");
        Texture enemyTexture = new Texture("Original/rocketPixel.png");
        Texture bulletTexture = new Texture("Original/bullet.png");

        multiplayerWorld = new MultiplayerGameWorld(playerIdleTexture, flameSheet, enemyTexture, bulletTexture);
        multiplayerClient.setWorld(multiplayerWorld);
        multiplayerClient.setOnDisconnected(() -> serverDisconnected = true);

        hudRenderer = new HudRenderer();
    }

    // ---------------- RENDER ----------------

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        float simulationDelta = Math.min(delta, MAX_SIMULATION_DELTA);

        if (multiplayerClient != null) {
            renderMultiplayer(simulationDelta);
        } else {
            renderSingleplayer(simulationDelta);
        }
    }

    private void renderSingleplayer(float delta) {
        world.update(delta);
        batch.setTransformMatrix(worldTransform);
        batch.begin();
        world.render(batch);
        batch.end();
        batch.setTransformMatrix(identityTransform);
        world.renderShapes(GameLayout.WORLD_OFFSET_X, camera.combined);
        world.renderHitboxes(GameLayout.WORLD_OFFSET_X, camera.combined);
        world.renderHud(batch, camera.combined);
    }

    private void renderMultiplayer(float delta) {
        if (serverDisconnected) {
            returnToMainMenu("Host left the game.");
            return;
        }

        if (multiplayerWorld.isMatchOver()) {
            returnToMainMenu(null);
            return;
        }

        sendLocalInput();

        multiplayerWorld.update(delta);

        batch.setTransformMatrix(worldTransform);
        batch.begin();
        multiplayerWorld.render(batch);
        batch.end();
        batch.setTransformMatrix(identityTransform);

        multiplayerWorld.renderShapes(GameLayout.WORLD_OFFSET_X, camera.combined);

        renderMultiplayerHud();
    }

    private void returnToMainMenu(String message) {
        if (returningToMenu) return;
        returningToMenu = true;
        if (multiplayerClient != null) multiplayerClient.disconnect();
        dispose();
        game.setScreen(new MainMenuScreen(game, message));
    }

    private void renderMultiplayerHud() {
        Player local = multiplayerWorld.getLocalPlayer();
        Player opponent = multiplayerWorld.getOpponentPlayer();

        float localHealth = local != null ? local.getHealth() : 0f;
        float localMaxHealth = local != null ? local.getMaxHealth() : 1f;
        float localFuel = local != null ? local.getFuel() : 0f;
        float localMaxFuel = local != null ? local.getMaxFuel() : 1f;

        boolean opponentPresent = opponent != null;
        float opponentHealth = opponentPresent ? opponent.getHealth() : 0f;
        float opponentMaxHealth = opponentPresent ? opponent.getMaxHealth() : 1f;
        float opponentFuel = opponentPresent ? opponent.getFuel() : 0f;
        float opponentMaxFuel = opponentPresent ? opponent.getMaxFuel() : 1f;

        hudRenderer.setProjectionMatrix(camera.combined);
        hudRenderer.renderMultiplayer(batch,
                localHealth, localMaxHealth, localFuel, localMaxFuel,
                opponentHealth, opponentMaxHealth, opponentFuel, opponentMaxFuel,
                opponentPresent,
                multiplayerWorld.getScore(), multiplayerWorld.getGameTime());
    }

    /**
     * Cita lokalnu tastaturu i salje input serveru - server je autoritativan,
     * ovaj klijent NE simulira sopstveno kretanje, samo prikazuje ono sto
     * server vrati kroz snapshot (jednostavno, bez client-side prediction za sad).
     */
    private void sendLocalInput() {
        PlayerInputMessage input = new PlayerInputMessage();
        input.left = Gdx.input.isKeyPressed(settings.getMoveLeft());
        input.right = Gdx.input.isKeyPressed(settings.getMoveRight());
        input.forward = Gdx.input.isKeyPressed(settings.getMoveUp());
        input.backward = Gdx.input.isKeyPressed(settings.getMoveDown());
        input.boost = Gdx.input.isKeyPressed(settings.getIsBoosting());
        input.shoot = Gdx.input.isKeyJustPressed(settings.getShoot());

        multiplayerClient.sendInput(input);
    }

    // ---------------- DISPOSE ----------------

    @Override
    public void dispose() {
        if (world != null) world.dispose();
        if (multiplayerWorld != null) multiplayerWorld.dispose();
        if (multiplayerClient != null) multiplayerClient.disconnect();
        if (hudRenderer != null) hudRenderer.dispose();
        batch.dispose();
    }

    // ---------------- ABSTRAKTNE ----------------

    @Override
    public void show() {
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
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
}
