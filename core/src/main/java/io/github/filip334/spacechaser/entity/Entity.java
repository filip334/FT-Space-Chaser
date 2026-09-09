package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import io.github.filip334.spacechaser.collision.CompoundHitbox;
import io.github.filip334.spacechaser.component.HealthComponent;

public abstract class Entity {

    protected float x,y;
    protected float previousX,previousY;

    protected float rotation;
    protected float previousRotation;

    protected float width,height;

    protected float maxSpeed;
    protected float acceleration;
    protected float boostAcceleration;
    protected float rotationSpeed;
    protected final float drag = 0.97f;

    protected float velocityX = 0;
    protected float velocityY = 0;

    HealthComponent health;

    protected CompoundHitbox hitbox;

    protected boolean isDead;

    protected Texture entityTexture;

    // ---------------- GET / SET ----------------

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

    public float getHeight(){
        return this.height;
    }
    public float getWidth(){
        return this.width;
    }

    public boolean isDead(){
        return isDead;
    }
    public void isDead(boolean state){
        isDead = state;
    }

    public CompoundHitbox getHitbox(){
        return this.hitbox;
    }

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
    
    
    // ---------------- UPDATE ----------------
    public abstract void update(float delta);
    public void updateHitbox(){
        hitbox.update(x, y, rotation);
    }

    // ---------------- RENDER ----------------
    public void hitBoxDebugRenderer(ShapeRenderer shapeRenderer){
        hitbox.debugRender(shapeRenderer);
    }
}
