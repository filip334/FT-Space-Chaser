package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.world.GameWorld;

public class GameScreen implements Screen{
    
    //
    Game game;

    //
    public GameScreen(Game game) {
        this.game = game;
        batch = new SpriteBatch();
        world = new GameWorld(game);
    }
   
    //
    private GameWorld world;
    private SpriteBatch batch;

    
    // ---------------- RENDER ----------------
    
    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        
        world.update(delta);

        batch.begin();
            world.render(batch);
        batch.end();
            world.renderShapes();
            world.renderHitboxes();
            world.renderHud(batch);
    }
    
    // ---------------- DISPOSE ----------------
    
    @Override
    public void dispose() {
        world.dispose();
        batch.dispose();
    }

    // ---------------- ABSTRAKTNE ----------------
    
    @Override
    public void show() {
    }

    @Override
    public void resize(int i, int i1) {
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
