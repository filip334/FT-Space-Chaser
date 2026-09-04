package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.math.Vector2;
import io.github.filip334.spacechaser.collision.CompoundHitbox;
import io.github.filip334.spacechaser.component.FuelComponent;
import io.github.filip334.spacechaser.component.HealthComponent;
import io.github.filip334.spacechaser.input.InputManager;
import io.github.filip334.spacechaser.world.GameSettings;

public class Player extends Entity{
    
    // INPUT 
    //PlayerInput input = new PlayerInput();
    private final InputManager input;
    
    //
    FuelComponent fuel;
    

    // ---------------- CONSTRUCTOR ----------------
    
    public Player() {
        input = new InputManager(new GameSettings());
    }

    public Player(float x, float y) {
        this(x, y, new GameSettings(), true);
    }

    public Player(float x, float y, GameSettings settings, boolean readsKeyboard) {
        input = new InputManager(settings, readsKeyboard);
        this.x = x;
        this.y = y;
        
        this.maxSpeed = 1500f;
        this.acceleration = 550f;
        this.boostAcceleration = 950f;
        this.rotationSpeed = 160f;
        
        this.health = new HealthComponent(100);
        fuel = new FuelComponent(100);
        
        width = 50;
        height = 50;
        
        createHitbox();
    }
    
    // INPUT MOVEMENT
    /*private void readInput() {

        input.left = Gdx.input.isKeyPressed(Input.Keys.A);
        input.right = Gdx.input.isKeyPressed(Input.Keys.D);
        input.forward = Gdx.input.isKeyPressed(Input.Keys.W) && fuel.hasFuel();
        input.backward = Gdx.input.isKeyPressed(Input.Keys.S);
        input.boost = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && fuel.hasFuel();
        input.shoot = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        
    }*/
    private void handleMovement(float delta) {

        previousX = x;
        previousY = y;
        previousRotation = rotation;
        
        float currentAcceleration = input.boosting() ? boostAcceleration : acceleration;

        if (input.moveForward() && input.boosting()) {
            fuel.consume(20f * delta);
        }

        if (input.moveLeft()) {
            rotation += rotationSpeed * delta;
        }

        if (input.moveRight()) {
            rotation -= rotationSpeed * delta;
        }

        float rad = (float) Math.toRadians(rotation);

        float dirX = (float) Math.cos(rad);
        float dirY = (float) Math.sin(rad);

        if (input.moveForward()) {
            velocityX += dirX * currentAcceleration * delta;
            velocityY += dirY * currentAcceleration * delta;
        }

        if (input.moveBackward()) {
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
    
    // HITBOX
    private void createHitbox() {
        hitbox = new CompoundHitbox();
        // CENTAR
        hitbox.addBox(-8f, 0f, 46f, 19f);
        // FRONT
        hitbox.addBox(23f, 0f, 12f, 17f);
        // LEFT WING
        hitbox.addBox(-6f, 22f, 17f, 27f);
        // RIGHT WING
        hitbox.addBox(-6f, -22f, 17f, 27f);
        // LEFT ENGINE
        hitbox.addBox(-26f, 18f, 17f, 10f);
        // RIGHT ENGINE
        hitbox.addBox(-26f, -18f, 17f, 10f);
    }
    
    // GET / SET
    
        // HEALTH
    public void setNetworkHealth(float health) {

        this.health.setHealth(health);

        if (health <= 0f) {
            this.isDead = true;
        } else {
            this.isDead = false;
        }
    }
        // FUEL
    public float getMaxFuel(){
        return fuel.getMaxFuel();
    }
    public float getFuel(){
        return fuel.getFuel();
    }
    public void setNetworkFuel(float fuel) {
        this.fuel.setFuel(fuel);
    }
    
        // VELOCITY
    public void setVelocity(float velocityX, float velocityY) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }
    
        // POSITION
    public Vector2 getPosition() {
        return new Vector2(x, y);
    }
    
        // RESTORE
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
    
        // INPUT
    public boolean wantsToShoot(){
        return input.shoot();
    }
    public boolean isBoosting() {
        return input.boosting() && input.moveForward();
    }
    public boolean isThrusting() {
        return input.moveForward();
    }
    public void setNetworkBoosting(boolean boosting) {
        input.setBoost(boosting);
    }
    public void setNetworkThrusting(boolean thrusting) {
        input.setForward(thrusting);
    }
    public void setNetworkState(float x,float y,float rotation){
        this.x = x;
        this.y = y;
        this.rotation = rotation;

        updateTransform();
    }
    /**
     * Za mrezno ogledalo (MultiplayerGameWorld) - server salje da li je
     * igrac trenutno pod thrust-om, klijent to primenjuje da bi animacija
     * i za protivnika (i za sopstveni brod u multiplayeru) radila ispravno.
     * @param delta
     */
    
    
    // UPDATE / RENDER / DISPOSE
    
    @Override
    public void update(float delta) {
        if (isDead) {
            return; // eliminisan - vise se ne krece niti reaguje na input
        }

        handleMovement(delta);
        updateTransform();
    }
    
    public void applyInput(boolean left, boolean right, boolean forward, boolean backward, boolean boost, boolean shoot) {
        input.setState(left, right, forward && fuel.hasFuel(), backward,
                boost && fuel.hasFuel(), shoot);
    }
    /**
     * Update varijanta za mrezno kontrolisane igrace - NE cita lokalnu tastaturu
     * (Gdx.input ne postoji/ne vazi za druge igrace), samo primenjuje input
     * koji je vec postavljen preko applyInput(). 
     * @param delta
     */
    public void updateNetworked(float delta) {
        if (isDead) {
            return; // eliminisan - vise se ne krece niti reaguje na input
        }
        handleMovement(delta);
        updateTransform();
    }
    
    
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
