/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.world.GameWorld;

/**
 *
 * @author Todorovic
 */
public class GameScreen implements Screen{
    Game game;

    //KONSTRUKTOR
    
    public GameScreen(Game game) {
        this.game = game;
        batch = new SpriteBatch();
        world = new GameWorld(game);
    }
   
    private GameWorld world;
    private SpriteBatch batch;

    
    // GDX METODE 
    
    @Override
    public void show() {
    }

    
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
    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        world.dispose();
        batch.dispose();
    }
}
