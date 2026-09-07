package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
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
    public void setPosition(float x,float y){
        this.x = x;
        this.y = y;
    }
    public Vector2 getPosition() {
        return new Vector2(x, y);
    }
    public float getPreviousX() {
        return previousX;
    }
    public float getPreviousY() {
        return previousY;
    }

        // VELOCITY
    public float getVelocityX() {
        return velocityX;
    }
    public float getVelocityY() {
        return velocityY;
    }
    public void setVelocityX(float velocityX) {
        this.velocityX = velocityX;
    }
    public void setVelocityY(float velocityY) {
        this.velocityY = velocityY;
    }

        // ROTATION
    public float getRotation(){
        return this.rotation;
    }
    public void setRotation(float rotation){
        this.rotation = rotation;
    }
    public float getPreviousRotation() {
        return previousRotation;
    }
    public void setPreviousRotation(float previousRotation) {
        this.previousRotation = previousRotation;
    }
    
        // SIZE
    public float getHeight(){
        return this.height;
    }
    public float getWidth(){
        return this.width;
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
    
        // HEALTH
    public float getHealth() {
        return health.getHealth();
    }
    public float getMaxHealth(){
        return health.getMaxHealth();
    }
    public void setHealth(float health) {
        this.health.setHealth(health);
    }
    public void takeDamage(float damage){
        health.damage(damage);
        if(health.isDead()){
            this.isDead = true;
        }
    }
    
    
    // UPDATE / RENDER / DISPOSE
    public abstract void update(float delta);
    public void updateHitbox(){
        hitbox.update(x, y, rotation);
    }
    public void hitBoxDebugRenderer(ShapeRenderer shapeRenderer){
        hitbox.debugRender(shapeRenderer);
    }
}
