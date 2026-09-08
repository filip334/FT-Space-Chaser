package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.renderer.HudRenderer;
import io.github.filip334.spacechaser.server.GameServer;
import io.github.filip334.spacechaser.network.message.PlayerInputMessage;
import io.github.filip334.spacechaser.ui.Buttons;
import io.github.filip334.spacechaser.ui.Fonts;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.world.GameWorld;
import io.github.filip334.spacechaser.settings.GameSettings;
import io.github.filip334.spacechaser.world.GameLayout;
import io.github.filip334.spacechaser.network.MultiplayerClient;
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
    private GameServer hostServer; // != null samo kod hosta - da moze da ugasi mec svima kad ode
    private HudRenderer hudRenderer;
    private final GameSettings settings;
    private final Matrix4 worldTransform = new Matrix4().setToTranslation(GameLayout.WORLD_OFFSET_X, 0f, 0f);
    private final Matrix4 identityTransform = new Matrix4();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(GameLayout.WINDOW_WIDTH, GameLayout.WINDOW_HEIGHT, camera);
    private volatile boolean serverDisconnected;
    private boolean returningToMenu;

    // PAUZA
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final BitmapFont pauseTitleFont = Fonts.generate(44, Theme.WHITE);
    private final BitmapFont pauseButtonFont = Fonts.generate(24, Theme.WHITE);
    private final BitmapFont pauseSubFont = Fonts.generate(18, Theme.WHITE);
    private final Vector2 mouseViewportPos = new Vector2();
    private final Rectangle resumeButton = new Rectangle();
    private final Rectangle restartButton = new Rectangle();
    private final Rectangle mainMenuButton = new Rectangle();

    private boolean paused; // singleplayer - lokalno stanje
    private boolean showHitboxes; // F1 - iskljuceno po podrazumevanju
    private volatile boolean multiplayerPaused; // multiplayer - stanje sa servera
    private volatile int pausedByPlayerId = -1;
    private boolean deadLeaveMenuOpen; // mrtav igrac u multiplayeru - ESC nudi samo Leave Match, ne pauzira zivog igraca

    // reportXScore() vraca true SAMO na frejmu kad skor prvi put predje rekord -
    // na sledecim frejmovima (skor isti/manji od tek upisanog rekorda) vraca
    // false, pa se mora pamtiti da li je rekord ikad oboren tokom ove partije.
    private boolean newHighScoreAchieved;

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

    /** Koristi host - GameScreen moze da ugasi ceo mec (server) kad host ode. */
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
            drawPauseOverlay();
            drawPauseMenu("PAUSED", true);
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
            drawPauseOverlay();
            boolean isPauser = pausedByPlayerId == multiplayerClient.getLocalPlayerId();
            if (isPauser) {
                drawPauseMenu("PAUSED", false);
                handleMultiplayerPauserInput();
            } else {
                drawInfoLeaveOverlay("PAUSED", "Game will continue when the other player is ready.");
                handleLeaveOnlyInput();
            }
        } else if (deadLeaveMenuOpen) {
            drawPauseOverlay();
            drawInfoLeaveOverlay("ELIMINATED", "You can spectate or leave the match.");
            handleLeaveOnlyInput();
        }
    }

    // =========================================================
    // PAUZA
    // =========================================================

    private void handleMultiplayerPauseKey() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) return;

        // Mrtav igrac samo ceka kraj meca - nema smisla da moze da pauzira
        // igru koja se jos igra za ziveg protivnika, pa mu ESC nudi samo izlaz.
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
        // Ako je pauzirao DRUGI igrac, ESC ovde ne radi nista -
        // samo taj igrac moze da nastavi igru.
    }

    private void drawPauseOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setTransformMatrix(identityTransform);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.65f);
        shapeRenderer.rect(0f, 0f, GameLayout.WINDOW_WIDTH, GameLayout.WINDOW_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // Fiksna sirina/visina za SVA dugmad pauze - ranije se svako dugme
    // pravilo tacno oko sirine sopstvenog teksta, pa je "Main Menu" ispalo
    // sire od "Resume"/"Restart".
    private static final float PAUSE_BUTTON_WIDTH = 260f;
    private static final float PAUSE_BUTTON_HEIGHT = 64f;
    private static final float PAUSE_BUTTON_SPACING = 26f;
    private static final float PAUSE_TITLE_GAP = 55f;

    /**
     * @param showRestart true (singleplayer) prikazuje i "Restart", inace
     *                    samo "Resume" i "Main Menu"/"Leave Match".
     */
    private void drawPauseMenu(String title, boolean showRestart) {
        float centerX = GameLayout.WINDOW_WIDTH / 2f;
        float centerY = GameLayout.WINDOW_HEIGHT / 2f;
        float titleY = centerY + 160f;

        batch.begin();
        pauseTitleFont.setColor(Theme.WHITE);
        GlyphLayout titleLayout = new GlyphLayout(pauseTitleFont, title);
        pauseTitleFont.draw(batch, title, centerX - titleLayout.width / 2f, titleY);
        batch.end();

        float cursorTop = titleY - titleLayout.height - PAUSE_TITLE_GAP;

        if (showRestart) {
            cursorTop = drawMenuButtonAndAdvance(resumeButton, "Resume", centerX, cursorTop);
            cursorTop = drawMenuButtonAndAdvance(restartButton, "Restart", centerX, cursorTop);
            drawMenuButton(mainMenuButton, "Main Menu", centerX, cursorTop);
        } else {
            cursorTop = drawMenuButtonAndAdvance(resumeButton, "Resume", centerX, cursorTop);
            drawMenuButton(mainMenuButton, "Leave Match", centerX, cursorTop);
        }
    }

    /** Iscrtava dugme sa gornjom ivicom na topY i vraca topY sledeceg dugmeta ispod. */
    private float drawMenuButtonAndAdvance(Rectangle bounds, String label, float centerX, float topY) {
        drawMenuButton(bounds, label, centerX, topY);
        return topY - PAUSE_BUTTON_HEIGHT - PAUSE_BUTTON_SPACING;
    }

    /** @param topY gornja ivica dugmeta (sva dugmad dele istu fiksnu sirinu/visinu). */
    private void drawMenuButton(Rectangle bounds, String label, float centerX, float topY) {
        Buttons.layoutStack(bounds, centerX, topY, PAUSE_BUTTON_WIDTH, PAUSE_BUTTON_HEIGHT, PAUSE_BUTTON_SPACING);
        boolean hovered = bounds.contains(pauseMouse());
        Buttons.draw(batch, shapeRenderer, pauseButtonFont, bounds, label, hovered);
    }

    /** Prikaz "info + Leave Match" overlaya - koristi ga i cekanje na pauziranog protivnika i mrtav igrac. */
    private void drawInfoLeaveOverlay(String title, String subtitle) {
        float centerX = GameLayout.WINDOW_WIDTH / 2f;
        float centerY = GameLayout.WINDOW_HEIGHT / 2f;

        batch.begin();
        pauseTitleFont.setColor(Theme.WHITE);
        GlyphLayout titleLayout = new GlyphLayout(pauseTitleFont, title);
        pauseTitleFont.draw(batch, titleLayout, centerX - titleLayout.width / 2f, centerY + 60f);

        pauseSubFont.setColor(Theme.CYAN);
        GlyphLayout sub = new GlyphLayout(pauseSubFont, subtitle);
        pauseSubFont.draw(batch, sub, centerX - sub.width / 2f, centerY);
        batch.end();

        // Vise razmaka izmedju podnaslova i dugmeta nego ranije (bilo je centerY - 30).
        drawMenuButton(mainMenuButton, "Leave Match", centerX, centerY - 60f);
    }

    private void handleLeaveOnlyInput() {
        if (!Gdx.input.justTouched()) return;
        if (mainMenuButton.contains(pauseMouse())) {
            returnToMainMenu(null);
        }
    }

    private void handleSingleplayerPauseInput() {
        if (!Gdx.input.justTouched()) return;
        Vector2 mouse = pauseMouse();

        if (resumeButton.contains(mouse)) {
            paused = false;
        } else if (restartButton.contains(mouse)) {
            world.dispose();
            world = new GameWorld(game, settings);
            paused = false;
        } else if (mainMenuButton.contains(mouse)) {
            dispose();
            game.setScreen(new MainMenuScreen(game));
        }
    }

    private void handleMultiplayerPauserInput() {
        if (!Gdx.input.justTouched()) return;
        Vector2 mouse = pauseMouse();

        if (resumeButton.contains(mouse)) {
            multiplayerClient.sendPause(false);
        } else if (mainMenuButton.contains(mouse)) {
            returnToMainMenu(null);
        }
    }

    /** Pretvara koordinate misa (piksel ekrana) u koordinate viewport-a - FitViewport moze da leteruje/skalira prozor. */
    private Vector2 pauseMouse() {
        mouseViewportPos.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseViewportPos);
        return mouseViewportPos;
    }

    /**
     * Prekida multiplayer sesiju (dispose ovog ekrana, sto vec gasi server/
     * klijenta - vidi dispose()) - zajednicko za "vrati se u meni" i "mec
     * gotov" puteve. @return false ako je sesija vec zavrsena (izbegava dupli setScreen).
     */
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
        // Host koji izlazi mora da ugasi ceo server - u suprotnom se samo
        // njegov sopstveni (loopback) klijent diskonektuje, a mec i server
        // ostaju da rade dalje i gost ostaje da igra sam bez ikakve poruke.
        // dispose() se poziva na SVAKOM putu napustanja ekrana (vidi
        // endMultiplayerSession()), pa je ovo i jedino mesto gde treba da stoji.
        if (hostServer != null) hostServer.stop();
        if (world != null) world.dispose();
        if (multiplayerWorld != null) multiplayerWorld.dispose();
        if (multiplayerClient != null) multiplayerClient.disconnect();
        if (hudRenderer != null) hudRenderer.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        pauseTitleFont.dispose();
        pauseButtonFont.dispose();
        pauseSubFont.dispose();
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
