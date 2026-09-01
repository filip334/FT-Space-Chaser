package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.entity.Player;
import io.github.filip334.spacechaser.renderer.HudRenderer;
import io.github.filip334.spacechaser.server.PlayerInputMessage;
import io.github.filip334.spacechaser.world.GameWorld;
import io.github.filip334.spacechaser.world.MultiplayerClient;
import io.github.filip334.spacechaser.world.MultiplayerGameWorld;

public class GameScreen implements Screen {

    private final Game game;
    private final SpriteBatch batch;

    // SINGLEPLAYER
    private GameWorld world;

    // MULTIPLAYER
    private MultiplayerClient multiplayerClient;
    private MultiplayerGameWorld multiplayerWorld;
    private HudRenderer hudRenderer;

    public GameScreen(Game game) {
        this.game = game;
        batch = new SpriteBatch();
        world = new GameWorld(game);
    }

    public GameScreen(Game game, MultiplayerClient multiplayerClient) {
        this.game = game;
        this.multiplayerClient = multiplayerClient;
        batch = new SpriteBatch();

        Texture playerIdleTexture = new Texture("Original/ship.png");
        Texture playerThrustSheet = new Texture("Original/shipMove.png");
        Texture flameSheet = new Texture("Original/moveFire.png");
        Texture enemyTexture = new Texture("Original/rocket.png");
        Texture bulletTexture = new Texture("Original/bullet.png");

        multiplayerWorld = new MultiplayerGameWorld(playerIdleTexture, playerThrustSheet, flameSheet, enemyTexture, bulletTexture);
        multiplayerClient.setWorld(multiplayerWorld);

        hudRenderer = new HudRenderer();
    }

    // ---------------- RENDER ----------------

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        if (multiplayerClient != null) {
            renderMultiplayer(delta);
        } else {
            renderSingleplayer(delta);
        }
    }

    private void renderSingleplayer(float delta) {
        world.update(delta);
        batch.begin();
        world.render(batch);
        batch.end();
        world.renderShapes();
        world.renderHitboxes();
        world.renderHud(batch);
    }

    private void renderMultiplayer(float delta) {
        if (multiplayerWorld.isMatchOver()) {
            returnToMainMenu();
            return;
        }

        sendLocalInput();

        multiplayerWorld.update(delta);

        batch.begin();
        multiplayerWorld.render(batch);
        batch.end();

        multiplayerWorld.renderShapes();

        renderMultiplayerHud();
    }

    private void returnToMainMenu() {
        if (multiplayerClient != null) multiplayerClient.disconnect();
        dispose();
        game.setScreen(new MainMenuScreen(game));
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
        input.left = Gdx.input.isKeyPressed(Input.Keys.A);
        input.right = Gdx.input.isKeyPressed(Input.Keys.D);
        input.forward = Gdx.input.isKeyPressed(Input.Keys.W);
        input.backward = Gdx.input.isKeyPressed(Input.Keys.S);
        input.boost = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
        input.shoot = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

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