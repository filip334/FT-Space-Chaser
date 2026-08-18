/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.filip334.spacechaser.animation;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Todorovic
 */
public class AnimationController {
     public enum PlayMode {
        NORMAL,
        LOOP,
        REVERSED,
        LOOP_REVERSED
    }

    private static class AnimState {
        Animation<TextureRegion> animation;
        float time = 0f;
    }

    private final Map<String, AnimState> animations = new HashMap<>();

    private String currentState;
    private PlayMode playMode = PlayMode.NORMAL;

    // ---------------- SPRITE SHEET ADD ----------------

    public void add(String name,
                    Texture sheet,
                    int frameCount,
                    int frameWidth,
                    int frameHeight,
                    float frameDuration) {

        TextureRegion[][] grid = TextureRegion.split(
                sheet,
                frameWidth,
                frameHeight
        );

        TextureRegion[] frames = new TextureRegion[frameCount];

        int index = 0;

        for (TextureRegion[] row : grid) {
            for (TextureRegion region : row) {

                if (index >= frameCount) break;

                frames[index++] = region;
            }
        }

        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames);

        AnimState state = new AnimState();
        state.animation = animation;

        animations.put(name, state);

        if (currentState == null) {
            currentState = name;
        }
    }

    // ---------------- STATE ----------------

    public void setState(String state) {
        if (!animations.containsKey(state)) return;

        if (!state.equals(currentState)) {
            currentState = state;
            animations.get(state).time = 0f;
        }
    }

    public void setPlayMode(PlayMode mode) {
        this.playMode = mode;
    }

    // ---------------- UPDATE ----------------

    public void update(float delta) {

        if (currentState == null) return;

        AnimState anim = animations.get(currentState);
        if (anim == null) return;

        float duration = anim.animation.getAnimationDuration();

        switch (playMode) {

            case LOOP:
                anim.time += delta;
                if (anim.time >= duration) anim.time = 0f;
                break;

            case NORMAL:
                anim.time += delta;
                if (anim.time >= duration) anim.time = duration;
                break;

            case REVERSED:
                anim.time -= delta;
                if (anim.time <= 0f) anim.time = 0f;
                break;

            case LOOP_REVERSED:
                anim.time -= delta;
                if (anim.time <= 0f) anim.time = duration;
                break;
        }
    }

    // ---------------- FRAME ----------------

    public TextureRegion getFrame() {

        if (currentState == null) return null;

        AnimState anim = animations.get(currentState);
        if (anim == null) return null;

        boolean looping =
                playMode == PlayMode.LOOP ||
                playMode == PlayMode.LOOP_REVERSED;

        return anim.animation.getKeyFrame(anim.time, looping);
    }

    // ---------------- CONTROL ----------------

    public void playForward(String state) {
        setState(state);
        setPlayMode(PlayMode.NORMAL);
    }

    public void playLoop(String state) {
        setState(state);
        setPlayMode(PlayMode.LOOP);
    }

    public void playReverse(String state) {
        setState(state);
        setPlayMode(PlayMode.REVERSED);
    }

    public void playLoopReverse(String state) {
        setState(state);
        setPlayMode(PlayMode.LOOP_REVERSED);
    }

    public void reset() {
        if (currentState == null) return;
        animations.get(currentState).time = 0f;
    }

    public String getCurrentState() {
        return currentState;
    }
}
