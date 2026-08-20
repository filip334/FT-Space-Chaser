package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.filip334.spacechaser.collision.CompoundHitbox;
import io.github.filip334.spacechaser.component.FuelComponent;
import io.github.filip334.spacechaser.component.HealthComponent;
import io.github.filip334.spacechaser.input.PlayerInput;

public class Player extends Entity{
    
    // INPUT 
    PlayerInput input = new PlayerInput();
    
    // SPRITE
    private Sprite ship;
    
    // PHYSICS
    private float velocityX = 0;
    private float velocityY = 0;
    private float previousX;
    private float previousY;

    private final float maxSpeed = 1500f;
    private final float acceleration = 550f;
    private final float boostAcceleration = 950f;
    private final float rotationSpeed = 160f;
    private final float drag = 0.97f;

    //
    HealthComponent health;
    FuelComponent fuel;
    
    
    //
    private Texture idleAnimation;

    // ---------------- CONSTRUCTOR ----------------
    
    public Player(float x,float y) {
        this.x = x;
        this.y = y;
        
        health = new HealthComponent(100);
        fuel = new FuelComponent(100);
        
        String texturePath = "Original/";
        String idlePath = texturePath + "ship.png";
        
        idleAnimation = new Texture(idlePath);
        ship = new Sprite(idleAnimation);
        
        width = 128;
        height = 128;
        ship.setSize(width, height);
        ship.setOriginCenter();
        
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

        previousX = x;
        previousY = y;
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
        hitbox.addBox(-20, 28, 15, 42);

        // RIGHT WING
        hitbox.addBox(-20, -28, 15, 42);

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

        //
    private void updateTransform() {
        ship.setCenter(x, y);
        ship.setRotation(rotation);
        hitbox.update(x, y, rotation);
    }
    
        //
    public boolean wantsToShoot(){
        return input.shoot;
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

        /*ship.setRotation(rotation);
        ship.setCenter(x, y);

        //ship.setRegion(animator.getFrame());
        hitbox.update(this.x, this.y, rotation);*/
        updateTransform();
    }

    @Override
    public void render(SpriteBatch batch) {
        ship.draw(batch);
    }
    public void debugRender(ShapeRenderer shapeRenderer){
        hitbox.debugRender(shapeRenderer);
    }
    
    @Override
    public void dispose() {
        
        /*idleAnimation.dispose();
        moveAnimation.dispose();
        boostAnimation.dispose();
        if (turnLeftAnimation != idleAnimation) {
            turnLeftAnimation.dispose();
        }
        if (turnRightAnimation != idleAnimation && turnRightAnimation != turnLeftAnimation) {
            turnRightAnimation.dispose();
        }
        attackAnimation.dispose();*/
    }
}
