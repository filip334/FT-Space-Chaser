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
    
    // POSITION
    protected float x,y;
    
    // SIZE
    protected float width,height;
    
    // ROTATION
    protected float rotation;
    
    // HITBOX
    protected CompoundHitbox hitbox;
    
    // STATE
    protected boolean isDead;
    
    // TEXTURE
    protected Texture entityTexture;
    
    // GET/SET
    
        // POSITION
    public float getX(){
        return this.x;
    }
    public float getY(){
        return this.y;
    }
        // SIZE
    public float getHeight(){
        return this.height;
    }
    public float getWidth(){
        return this.width;
    }
        // ROTATION
    public float getRotation(){
        return this.rotation;
    }
        // STATE
    public boolean isDead(){
        return isDead;
    }
    public void isDead(boolean state){
        isDead = state;
    }
    
        // HITBOX
    public CompoundHitbox getHitbox(){
        return this.hitbox;
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
