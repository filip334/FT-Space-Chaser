/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.filip334.spacechaser.collision.CompoundHitbox;

/**
 *
 * @author Todorovic
 */
public abstract class Entity {
    // KOORDINATE
    protected float x,y;
    
    // VELICINA
    protected float width,height;
    
    // HITBOX
    protected CompoundHitbox hitbox;
    
    // STANJE
    protected boolean isDead;
    
    // TEKSTURA
    protected Texture entityTexture;
    
    // GETERI/SETERI
    public boolean isDead(){
        return isDead;
    }
    
    
    // UPDATE / RENDER / DISPOSE
    public abstract void update(float delta);
    public abstract void render(SpriteBatch batch);
    public void dispose(){
        if (entityTexture != null) {
            entityTexture.dispose();
        }
    }
   
}
