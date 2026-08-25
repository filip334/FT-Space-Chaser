package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import io.github.filip334.spacechaser.collision.CompoundHitbox;
import io.github.filip334.spacechaser.component.FuelComponent;
import io.github.filip334.spacechaser.component.HealthComponent;
import io.github.filip334.spacechaser.input.PlayerInput;

public class Player extends Entity{
    
    // INPUT 
    PlayerInput input = new PlayerInput();
    
    //
    FuelComponent fuel;
    
    //
    //private Texture idleAnimation;

    // ---------------- CONSTRUCTOR ----------------
    
    public Player() {
    }

    public Player(float x, float y) {
        this.x = x;
        this.y = y;
        
        this.maxSpeed = 1500f;
        this.acceleration = 550f;
        this.boostAcceleration = 950f;
        this.rotationSpeed = 160f;
        
        this.health = new HealthComponent(100);
        fuel = new FuelComponent(100);
        
        String texturePath = "Original/";
        String idlePath = texturePath + "ship.png";
        
        //idleAnimation = new Texture(idlePath);
        
        width = 128;
        height = 128;
        
        createHitbox();
    }
    
    // INPUT MOVEMENT
    private void readInput() {

        input.left = Gdx.input.isKeyPressed(Input.Keys.A);
        input.right = Gdx.input.isKeyPressed(Input.Keys.D);
        input.forward = Gdx.input.isKeyPressed(Input.Keys.W) && fuel.hasFuel();
        input.backward = Gdx.input.isKeyPressed(Input.Keys.S);
        input.boost = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && fuel.hasFuel();
        input.shoot = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        
    }
    private void handleMovement(float delta) {

        previousX = x;
        previousY = y;
        previousRotation = rotation;
        
        boolean isBoosting = input.boost && input.forward;
        float currentAcceleration = isBoosting ? boostAcceleration : acceleration;

        if (isBoosting) {
            fuel.consume(20f * delta);
        }

        if (input.left) {
            rotation += rotationSpeed * delta;
        }

        if (input.right) {
            rotation -= rotationSpeed * delta;
        }

        float rad = (float) Math.toRadians(rotation);

        float dirX = (float) Math.cos(rad);
        float dirY = (float) Math.sin(rad);

        if (input.forward) {
            velocityX += dirX * currentAcceleration * delta;
            velocityY += dirY * currentAcceleration * delta;
        }

        if (input.backward) {
            velocityX -= dirX * acceleration * 0.6f * delta;
            velocityY -= dirY * acceleration * 0.6f * delta;
        }
        
        x += velocityX * delta;
        y += velocityY * delta;

        velocityX *= Math.pow(drag, delta * 60);
        velocityY *= Math.pow(drag, delta * 60);

        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        if (speed > maxSpeed) {
            float scale = maxSpeed / speed;
            velocityX *= scale;
            velocityY *= scale;
        }
    }
    
    // DEMAGE
    public void takeDamage(float damage){
        health.damage(damage);
        if(health.isDead()){
            this.isDead = true;
        }
    }
    
    // HITBOX
    private void createHitbox() {
        hitbox = new CompoundHitbox();

        // CENTAR
        hitbox.addBox(-10, 0, 75, 25);

        // FRONT
        hitbox.addBox(38, 0, 25, 14);

        // LEFT WING
        hitbox.addBox(-20, 28, 15, 25);

        // RIGHT WING
        hitbox.addBox(-20, -28, 15, 25);

        // LEFT ENGINE
        hitbox.addBox(-30, 18, 40, 10);

        // RIGHT ENGINE
        hitbox.addBox(-30, -18, 40, 10);
    }
    
    // GET / SET
    
        // HEALTH
    public float getMaxHealth(){
        return health.getMaxHealth();
    }
    public float getHealth(){
        return health.getHealth();
    }
    
        // FUEL
    public float getMaxFuel(){
        return fuel.getMaxFuel();
    }
    public float getFuel(){
        return fuel.getFuel();
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
    public void setVelocity(float velocityX, float velocityY) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }
    public Vector2 getPosition() {
        return new Vector2(x, y);
    }
    public void setNetworkHealth(float health) {

        this.health.setHealth(health);

        if (health <= 0f) {
            this.isDead = true;
        } else {
            this.isDead = false;
        }
    }
        // 
    public void restorePreviousX() {
        this.x = previousX;
        updateTransform();
    }
    public void restorePreviousY() {
        this.y = previousY;
        updateTransform();
    }
    public void restorePreviousPosition() {
        this.x = previousX;
        this.y = previousY;
        updateTransform();
    }
    public void restorePreviousRotation() {
        rotation = previousRotation;
        updateTransform();
    }
    public void restorePreviousState(){
        x = previousX;
        y = previousY;
        rotation = previousRotation;

        updateTransform();
    }

        //
    private void updateTransform() {
        hitbox.update(x, y, rotation);
    }
    
    public void savePreviousState() {
        previousX = x;
        previousY = y;
        previousRotation = rotation;
    }
    
        //
    public boolean wantsToShoot(){
        return input.shoot;
    }
    
    
    public void setNetworkState(
    float x,
    float y,
    float rotation
    ) {

        this.x = x;
        this.y = y;
        this.rotation = rotation;

        updateTransform();
    }
    
    // UPDATE / RENDER / DISPOSE
    
    @Override
    public void update(float delta) {
        // UPDATE INPUT
        readInput();
            
        // UPDATE ANIMATION SYSTEM
        //animator.update(delta);

        handleMovement(delta);
        //handleAnimation();

        //ship.setRegion(animator.getFrame());
        updateTransform();
    }
    public void updateHitbox(){
        hitbox.update(x, y, rotation);
    }
    public void debugRender(ShapeRenderer shapeRenderer){
        hitbox.debugRender(shapeRenderer);
    }
    public void applyInput(boolean left, boolean right, boolean forward,
                        boolean backward, boolean boost, boolean shoot) {
        input.left = left;
        input.right = right;
        input.forward = forward && fuel.hasFuel();
        input.backward = backward;
        input.boost = boost && fuel.hasFuel();
        input.shoot = shoot;
    }

    /**
     * Update varijanta za mrezno kontrolisane igrace - NE cita lokalnu tastaturu
     * (Gdx.input ne postoji/ne vazi za druge igrace), samo primenjuje input
     * koji je vec postavljen preko applyInput().
     */
    public void updateNetworked(float delta) {
        handleMovement(delta);
        updateTransform();
    }
    /*@Override
    public void dispose() {
        idleAnimation.dispose();
        moveAnimation.dispose();
        boostAnimation.dispose();
        if (turnLeftAnimation != idleAnimation) {
            turnLeftAnimation.dispose();
        }
        if (turnRightAnimation != idleAnimation && turnRightAnimation != turnLeftAnimation) {
            turnRightAnimation.dispose();
        }
        attackAnimation.dispose();
    }*/
}


/*
hitbox = new CompoundHitbox();

        // CENTAR
        hitbox.addBox(-2.5f, 0, 18.75f, 6.25f);

        // FRONT
        hitbox.addBox(9.5f, 0, 6.25f, 3.5f);

        // LEFT WING
        hitbox.addBox(-5f, 7, 3.75f, 10.5f);

        // RIGHT WING
        hitbox.addBox(-5f, -7, 3.75f, 10.5f);

        // LEFT ENGINE
        hitbox.addBox(-7.5f, 4.5f, 10, 2.5f);

        // RIGHT ENGINE
        hitbox.addBox(-7.5f, -4.5f, 10, 2.5f);

*/
