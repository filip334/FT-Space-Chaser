package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import io.github.filip334.spacechaser.SpaceChaserGame;
import io.github.filip334.spacechaser.ui.Buttons;
import io.github.filip334.spacechaser.ui.Fonts;
import io.github.filip334.spacechaser.ui.NameEditor;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.ui.UiPanel;
import io.github.filip334.spacechaser.settings.GameSettings;

public class SettingsScreen extends BaseScreen {

    private final GameSettings settings;

    private final BitmapFont titleFont;
    private final BitmapFont labelFont;
    private final BitmapFont rowFont;

    private final Rectangle nameBox = new Rectangle();
    private final NameEditor nameEditor;

    private enum Action { MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, SHOOT, BOOST }

    private final Action[] actions = Action.values();
    private final String[] actionLabels = {
            "Move Up", "Move Down", "Move Left", "Move Right", "Shoot", "Boost"
    };
    private final Rectangle[] actionBounds = new Rectangle[actions.length];
    private Action rebindingAction;

    private final Rectangle defaultButton = new Rectangle();
    private final Rectangle backButton = new Rectangle();

    // ---------------- KONSTRUKTOR ----------------

    public SettingsScreen(Game game) {
        super(game);
        this.settings = ((SpaceChaserGame) game).getSettings();

        titleFont = Fonts.generate(34, Theme.WHITE);
        labelFont = Fonts.generate(16, Theme.CYAN_DIM);
        rowFont = Fonts.generate(20, Theme.WHITE);

        nameEditor = new NameEditor(settings);

        for (int i = 0; i < actionBounds.length; i++) {
            actionBounds[i] = new Rectangle();
        }
    }

    // ---------------- RENDER ----------------

    @Override
    public void render(float delta) {
        beginFrame();

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        drawBackground(width, height);

        float startX = width * 0.075f;
        float cursorY = height - 60f;

        batch.begin();
        titleFont.setColor(Theme.WHITE);
        titleFont.draw(batch, "SETTINGS", startX, cursorY);
        batch.end();

        cursorY -= 70f;
        cursorY = drawNameSection(startX, cursorY);

        cursorY -= 40f;
        cursorY = drawControlsSection(startX, cursorY);

        cursorY -= 90f;
        drawBottomButtons(startX, cursorY);

        handleInput();
    }

    // ---------------- IME ----------------

    private float drawNameSection(float startX, float cursorY) {
        batch.begin();
        labelFont.draw(batch, "Player Name", startX, cursorY);
        batch.end();

        float labelGap = 34f;
        float boxHeight = 46f;
        float boxWidth = 320f;
        float boxTop = cursorY - labelFont.getCapHeight() - labelGap;
        float boxY = boxTop - boxHeight;
        nameBox.set(startX, boxY, boxWidth, boxHeight);

        UiPanel.draw(shapeRenderer, nameBox.x, nameBox.y, nameBox.width, nameBox.height, nameEditor.isEditing());

        batch.begin();
        rowFont.setColor(nameEditor.isEditing() ? Theme.WHITE : Theme.CYAN);
        float textY = boxY + boxHeight / 2f + rowFont.getCapHeight() / 2f;
        rowFont.draw(batch, nameEditor.getDisplayText(), startX + 14f, textY);
        batch.end();

        return boxY;
    }

    // ---------------- KONTROLE ----------------

    private static final float ROW_SPACING = 50f;
    private static final float ROW_HEIGHT = 38f;
    private static final float ROW_WIDTH = 320f;

    private float drawControlsSection(float startX, float cursorY) {
        batch.begin();
        labelFont.draw(batch, "Controls", startX, cursorY);
        batch.end();

        cursorY -= 40f;

        for (int i = 0; i < actions.length; i++) {
            float y = cursorY - i * ROW_SPACING;
            actionBounds[i].set(startX, y - ROW_HEIGHT + 12f, ROW_WIDTH, ROW_HEIGHT);

            boolean waiting = rebindingAction == actions[i];
            boolean hovered = !waiting && isMouseOver(actionBounds[i]);

            UiPanel.draw(shapeRenderer, actionBounds[i].x, actionBounds[i].y,
                    actionBounds[i].width, actionBounds[i].height, waiting || hovered);

            String keyName = waiting ? "Press a key..." : Input.Keys.toString(getKeyFor(actions[i]));

            batch.begin();
            rowFont.setColor(waiting || hovered ? Theme.WHITE : Theme.CYAN);
            rowFont.draw(batch, actionLabels[i] + " :  " + keyName, startX + 14f, y);
            batch.end();
        }

        return cursorY - (actions.length - 1) * ROW_SPACING;
    }

    // ---------------- GET / SET ----------------

    private int getKeyFor(Action action) {
        switch (action) {
            case MOVE_UP: return settings.getMoveUp();
            case MOVE_DOWN: return settings.getMoveDown();
            case MOVE_LEFT: return settings.getMoveLeft();
            case MOVE_RIGHT: return settings.getMoveRight();
            case SHOOT: return settings.getShoot();
            case BOOST: return settings.getIsBoosting();
            default: throw new IllegalStateException();
        }
    }

    private void setKeyFor(Action action, int keycode) {
        switch (action) {
            case MOVE_UP: settings.setMoveUp(keycode); break;
            case MOVE_DOWN: settings.setMoveDown(keycode); break;
            case MOVE_LEFT: settings.setMoveLeft(keycode); break;
            case MOVE_RIGHT: settings.setMoveRight(keycode); break;
            case SHOOT: settings.setShoot(keycode); break;
            case BOOST: settings.setBoost(keycode); break;
        }
    }

    private void beginRebind(Action action) {
        nameEditor.finish();
        rebindingAction = action;
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    cancelRebind();
                    return true;
                }
                setKeyFor(action, keycode);
                rebindingAction = null;
                Gdx.input.setInputProcessor(null);
                return true;
            }
        });
    }

    private void cancelRebind() {
        if (rebindingAction == null) return;
        rebindingAction = null;
        Gdx.input.setInputProcessor(null);
    }

    // ---------------- DEFAULT / BACK ----------------

    private static final float DEFAULT_BUTTON_WIDTH = 150f;
    private static final float BACK_EXTRA_GAP = 70f;

    private void drawBottomButtons(float startX, float y) {
        defaultButton.set(startX, y - 10f, DEFAULT_BUTTON_WIDTH, 44f);
        backButton.set(startX + DEFAULT_BUTTON_WIDTH + BACK_EXTRA_GAP, y - 10f, 130f, 44f);

        Buttons.draw(batch, shapeRenderer, rowFont, defaultButton, "Default", isMouseOver(defaultButton));
        Buttons.draw(batch, shapeRenderer, rowFont, backButton, "Back", isMouseOver(backButton));
    }

    // ---------------- INPUT ----------------

    private void handleInput() {
        if (!Gdx.input.justTouched()) {
            return;
        }

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        if (nameBox.contains(mouseX, mouseY)) {
            cancelRebind();
            nameEditor.begin();
            return;
        }

        for (int i = 0; i < actionBounds.length; i++) {
            if (actionBounds[i].contains(mouseX, mouseY)) {
                nameEditor.finish();
                beginRebind(actions[i]);
                return;
            }
        }

        if (defaultButton.contains(mouseX, mouseY)) {
            nameEditor.finish();
            cancelRebind();
            settings.resetControlsToDefault();
            return;
        }

        if (backButton.contains(mouseX, mouseY)) {
            nameEditor.finish();
            cancelRebind();
            navigateTo(new MainMenuScreen(game));
            return;
        }

        nameEditor.finish();
    }

    // ---------------- SCREEN METHODS ----------------

    @Override
    public void hide() {
        nameEditor.finish();
        cancelRebind();
    }

    // ---------------- DISPOSE ----------------

    @Override
    public void dispose() {
        super.dispose();
        titleFont.dispose();
        labelFont.dispose();
        rowFont.dispose();
    }
}
