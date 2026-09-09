package io.github.filip334.spacechaser.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import io.github.filip334.spacechaser.settings.GameSettings;

public class NameEditor {

    private static final int MAX_LENGTH = 16;

    private final GameSettings settings;
    private boolean editing;
    private String draft;

    public NameEditor(GameSettings settings) {
        this.settings = settings;
        this.draft = settings.getPlayerName();
    }

    public boolean isEditing() {
        return editing;
    }

    public String getDisplayText() {
        return editing ? draft + "|" : draft;
    }

    public void begin() {
        if (editing) return;
        editing = true;
        draft = settings.getPlayerName();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.BACKSPACE) {
                    if (!draft.isEmpty()) {
                        draft = draft.substring(0, draft.length() - 1);
                    }
                    return true;
                }
                if (keycode == Input.Keys.ENTER) {
                    finish();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    editing = false;
                    Gdx.input.setInputProcessor(null);
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                if (character >= 32 && character != 127 && draft.length() < MAX_LENGTH) {
                    draft += character;
                    return true;
                }
                return false;
            }
        });
    }

    public void finish() {
        if (!editing) return;
        settings.setPlayerName(draft);
        draft = settings.getPlayerName();
        editing = false;
        Gdx.input.setInputProcessor(null);
    }
}
