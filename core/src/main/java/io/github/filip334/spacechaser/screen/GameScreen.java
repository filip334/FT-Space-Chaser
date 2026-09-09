package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.renderer.HudRenderer;
import io.github.filip334.spacechaser.server.GameServer;
import io.github.filip334.spacechaser.network.message.PlayerInputMessage;
import io.github.filip334.spacechaser.world.GameWorld;
import io.github.filip334.spacechaser.settings.GameSettings;
import io.github.filip334.spacechaser.world.GameLayout;
import io.github.filip334.spacechaser.network.MultiplayerClient;
import io.github.filip334.spacechaser.world.MultiplayerGameWorld;

public class GameScreen implements Screen {

    private static final float MAX_SIMULATION_DELTA = 1f / 30f;

    private final Game game;
    private final SpriteBatch batch;

    private GameWorld world;

    private MultiplayerClient multiplayerClient;
    private MultiplayerGameWorld multiplayerWorld;
    private GameServer hostServer;
    private HudRenderer hudRenderer;
    private final GameSettings settings;
    private final Matrix4 worldTransform = new Matrix4().setToTranslation(GameLayout.WORLD_OFFSET_X, 0f, 0f);
    private final Matrix4 identityTransform = new Matrix4();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(GameLayout.WINDOW_WIDTH, GameLayout.WINDOW_HEIGHT, camera);
    private volatile boolean serverDisconnected;
    private boolean returningToMenu;

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final PauseMenu pauseMenu = new PauseMenu();
    private final Vector2 mouseViewportPos = new Vector2();

    private boolean paused;
    private boolean showHitboxes;
    private volatile boolean multiplayerPaused;
    private volatile int pausedByPlayerId = -1;
    private boolean deadLeaveMenuOpen;

    private boolean newHighScoreAchieved;

    // ---------------- KONSTRUKTORI ----------------

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
        multiplayerClient.setOnPauseStatus(status -> {
            multiplayerPaused = status.paused;
            pausedByPlayerId = status.pausedByPlayerId;
        });

        hudRenderer = new HudRenderer();
    }

    public GameScreen(Game game, MultiplayerClient multiplayerClient, GameServer hostServer) {
        this(game, multiplayerClient);
        this.hostServer = hostServer;
    }

    // ---------------- RENDER ----------------

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);
        float simulationDelta = Math.min(delta, MAX_SIMULATION_DELTA);

        if (multiplayerClient != null) {
            renderMultiplayer(simulationDelta);
        } else {
            renderSingleplayer(simulationDelta);
        }
    }

    private void renderSingleplayer(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            showHitboxes = !showHitboxes;
        }

        if (!paused) {
            world.update(delta);
        }

        if (((SpaceChaserGame) game).getHighScoreManager().reportSingleplayerScore(world.getScore())) {
            newHighScoreAchieved = true;
        }

        if (world.isMatchOver()) {
            int finalScore = world.getScore();
            boolean isRecord = newHighScoreAchieved;
            dispose();
            game.setScreen(new GameOverScreen(game, finalScore, isRecord, GameOverScreen.Mode.SINGLEPLAYER));
            return;
        }

        batch.setTransformMatrix(worldTransform);
        batch.begin();
        world.render(batch);
        batch.end();
        batch.setTransformMatrix(identityTransform);
        world.renderShapes(GameLayout.WORLD_OFFSET_X, camera.combined);
        if (showHitboxes) {
            world.renderHitboxes(GameLayout.WORLD_OFFSET_X, camera.combined);
        }
        world.renderHud(batch, camera.combined);

        if (paused) {
            pauseMenu.drawOverlay(shapeRenderer, identityTransform);
            pauseMenu.drawMenu(batch, shapeRenderer, "PAUSED", true, pauseMouse());
            handleSingleplayerPauseInput();
        }
    }

    private void renderMultiplayer(float delta) {
        if (serverDisconnected) {
            returnToMainMenu("Host left the game.");
            return;
        }

        if (multiplayerWorld.isMatchOver()) {
            int finalScore = multiplayerWorld.getScore();
            boolean isRecord = newHighScoreAchieved;
            if (!endMultiplayerSession()) return;
            game.setScreen(new GameOverScreen(game, finalScore, isRecord, GameOverScreen.Mode.MULTIPLAYER));
            return;
        }

        handleMultiplayerPauseKey();

        if (!multiplayerPaused) {
            sendLocalInput();
        }

        multiplayerWorld.update(delta);

        if (((SpaceChaserGame) game).getHighScoreManager().reportMultiplayerScore(multiplayerWorld.getScore())) {
            newHighScoreAchieved = true;
        }

        batch.setTransformMatrix(worldTransform);
        batch.begin();
        multiplayerWorld.render(batch);
        batch.end();
        batch.setTransformMatrix(identityTransform);

        multiplayerWorld.renderShapes(GameLayout.WORLD_OFFSET_X, camera.combined);

        renderMultiplayerHud();

        if (multiplayerPaused) {
            deadLeaveMenuOpen = false;
            pauseMenu.drawOverlay(shapeRenderer, identityTransform);
            boolean isPauser = pausedByPlayerId == multiplayerClient.getLocalPlayerId();
            if (isPauser) {
                pauseMenu.drawMenu(batch, shapeRenderer, "PAUSED", false, pauseMouse());
                handleMultiplayerPauserInput();
            } else {
                pauseMenu.drawInfoLeave(batch, shapeRenderer, "PAUSED", "Game will continue when the other player is ready.", pauseMouse());
                handleLeaveOnlyInput();
            }
        } else if (deadLeaveMenuOpen) {
            pauseMenu.drawOverlay(shapeRenderer, identityTransform);
            pauseMenu.drawInfoLeave(batch, shapeRenderer, "ELIMINATED", "You can spectate or leave the match.", pauseMouse());
            handleLeaveOnlyInput();
        }
    }

    // ---------------- PAUZA ----------------

    private void handleMultiplayerPauseKey() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) return;

        Player local = multiplayerWorld.getLocalPlayer();
        if (local != null && local.isDead()) {
            deadLeaveMenuOpen = !deadLeaveMenuOpen;
            return;
        }

        if (!multiplayerPaused) {
            multiplayerClient.sendPause(true);
        } else if (pausedByPlayerId == multiplayerClient.getLocalPlayerId()) {
            multiplayerClient.sendPause(false);
        }
    }

    private void handleLeaveOnlyInput() {
        if (!Gdx.input.justTouched()) return;
        if (pauseMenu.isMainMenuClicked(pauseMouse())) {
            returnToMainMenu(null);
        }
    }

    private void handleSingleplayerPauseInput() {
        if (!Gdx.input.justTouched()) return;
        Vector2 mouse = pauseMouse();

        if (pauseMenu.isResumeClicked(mouse)) {
            paused = false;
        } else if (pauseMenu.isRestartClicked(mouse)) {
            world.dispose();
            world = new GameWorld(game, settings);
            paused = false;
        } else if (pauseMenu.isMainMenuClicked(mouse)) {
            dispose();
            game.setScreen(new MainMenuScreen(game));
        }
    }

    private void handleMultiplayerPauserInput() {
        if (!Gdx.input.justTouched()) return;
        Vector2 mouse = pauseMouse();

        if (pauseMenu.isResumeClicked(mouse)) {
            multiplayerClient.sendPause(false);
        } else if (pauseMenu.isMainMenuClicked(mouse)) {
            returnToMainMenu(null);
        }
    }

    private Vector2 pauseMouse() {
        mouseViewportPos.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseViewportPos);
        return mouseViewportPos;
    }

    // ---------------- MULTIPLAYER SESSION ----------------

    private boolean endMultiplayerSession() {
        if (returningToMenu) return false;
        returningToMenu = true;
        dispose();
        return true;
    }

    private void returnToMainMenu(String message) {
        if (!endMultiplayerSession()) return;
        game.setScreen(new MainMenuScreen(game, message));
    }

    // ---------------- HUD ----------------

    private void renderMultiplayerHud() {
        Player local = multiplayerWorld.getLocalPlayer();
        Player opponent = multiplayerWorld.getOpponentPlayer();

        float localHealth = local != null ? local.getHealth() : 0f;
        float localMaxHealth = local != null ? local.getMaxHealth() : 1f;
        float localFuel = local != null ? local.getFuel() : 0f;
        float localMaxFuel = local != null ? local.getMaxFuel() : 1f;
        int localScore = local != null ? local.getScore() : 0;

        boolean opponentPresent = opponent != null;
        float opponentHealth = opponentPresent ? opponent.getHealth() : 0f;
        float opponentMaxHealth = opponentPresent ? opponent.getMaxHealth() : 1f;
        float opponentFuel = opponentPresent ? opponent.getFuel() : 0f;
        float opponentMaxFuel = opponentPresent ? opponent.getMaxFuel() : 1f;
        int opponentScore = opponentPresent ? opponent.getScore() : 0;

        hudRenderer.setProjectionMatrix(camera.combined);
        hudRenderer.renderMultiplayer(batch,
                localHealth, localMaxHealth, localFuel, localMaxFuel, localScore,
                opponentHealth, opponentMaxHealth, opponentFuel, opponentMaxFuel, opponentScore,
                opponentPresent,
                multiplayerWorld.getScore(), multiplayerWorld.getGameTime(), multiplayerWorld.getWaveNumber());

        if (multiplayerWorld.isWaveCountingDown()) {
            hudRenderer.renderCountdown(batch, (int) Math.ceil(multiplayerWorld.getWaveCountdownSeconds()));
        }
    }

    // ---------------- INPUT ----------------

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
        if (hostServer != null) hostServer.stop();
        if (world != null) world.dispose();
        if (multiplayerWorld != null) multiplayerWorld.dispose();
        if (multiplayerClient != null) multiplayerClient.disconnect();
        if (hudRenderer != null) hudRenderer.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        pauseMenu.dispose();
    }

    // ---------------- SCREEN METHODS ----------------

    @Override
    public void show() {
    }

    private static final long RESIZE_AUTOPAUSE_GRACE_MILLIS = 1000L;
    private final long createdAtMillis = TimeUtils.millis();

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);

        boolean pastStartupGrace = TimeUtils.timeSinceMillis(createdAtMillis) >= RESIZE_AUTOPAUSE_GRACE_MILLIS;
        if (pastStartupGrace && multiplayerClient == null && world != null) {
            paused = true;
        }
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
