package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.graphics.Texture;
import io.github.filip334.spacechaser.collision.CompoundHitbox;
import io.github.filip334.spacechaser.component.HealthComponent;

public abstract class Entity {
    
    // POSITION
    protected float x,y;
    protected float previousX,previousY;
    
    // ROTATION
    protected float rotation;
    protected float previousRotation;
    
    // SIZE
    protected float width,height;
    
    // SPEED
    protected float maxSpeed;
    protected float acceleration;
    protected float boostAcceleration;
    protected float rotationSpeed;
    protected final float drag = 0.97f;
    
    protected float velocityX = 0;
    protected float velocityY = 0;
    
    // HEALT
    HealthComponent health;
    
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
    public void setX(float x){
        this.x = x;
    }
    public void setY(float y){
        this.y = y;
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
    public void setPosition(float x,float y){
        this.x = x;
        this.y = y;
    }
    public void setRotation(float rotation){
        this.rotation = rotation;
    }
    
        // HITBOX
    public CompoundHitbox getHitbox(){
        return this.hitbox;
    }
    
    // UPDATE / RENDER / DISPOSE
    public abstract void update(float delta);
}
