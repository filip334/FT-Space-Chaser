package io.github.filip334.spacechaser.entity;

import io.github.filip334.spacechaser.collision.CompoundHitbox;
import io.github.filip334.spacechaser.component.FuelComponent;
import io.github.filip334.spacechaser.component.HealthComponent;
import io.github.filip334.spacechaser.input.InputManager;
import io.github.filip334.spacechaser.world.GameSettings;

public class Player extends Entity{

    // INPUT
    private final InputManager input;

    //
    FuelComponent fuel;

    // Sopstveni skor ovog igraca (multiplayer - svaki igrac vodi svoj).
    private int score = 0;


    // ---------------- CONSTRUCTOR ----------------

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
        this.rotationSpeed = 110f;
        
        this.health = new HealthComponent(100);
        fuel = new FuelComponent(100);
        
        width = 50;
        height = 50;
        
        createHitbox();
    }
    
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
        hitbox.addBox(0f, 0f, 50f, 20f);
        // WINGS
        hitbox.addBox(-5f, 0f, 15f, 50f);
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

        // SCORE
    public int getScore() {
        return score;
    }
    public void addScore(int amount) {
        score += amount;
    }
    public void setNetworkScore(int score) {
        this.score = score;
    }

        // VELOCITY
    public void setVelocity(float velocityX, float velocityY) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }
    
    private void updateTransform() {
        hitbox.update(x, y, rotation);
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
    // ---------------- MULTIPLAYER INTERPOLACIJA ----------------
    // Server salje snapshotove preko mreze i do 60x/sec, ali mogu da stignu
    // neravnomerno (jitter, TCP zastoji) - narocito primetno kod igraca koji
    // se pridruzio (guest), za razliku od hosta koji sa svojim serverom
    // "razgovara" preko loopback-a. Umesto da svaki primljeni paket odmah
    // trzajno postavi poziciju, cuvamo ga kao cilj (net*) i svakog render
    // frejma se samo priblizavamo tom cilju - vidljivo kretanje ostaje glatko
    // cak i kad paketi kasne ili stignu u grupi.
    private boolean netInitialized;
    private float netTargetX, netTargetY, netTargetRotation;
    private static final float NET_SMOOTHING_RATE = 18f;

    public void setNetworkState(float x, float y, float rotation) {
        netTargetX = x;
        netTargetY = y;
        netTargetRotation = rotation;

        if (!netInitialized) {
            // Prvi paket za ovog igraca - nema smisla interpolirati od (0,0),
            // odmah skoci na stvarnu pocetnu poziciju/rotaciju.
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            netInitialized = true;
            updateTransform();
        }
    }

    /** Poziva se svakog render frejma iz MultiplayerGameWorld.update(delta). */
    public void interpolateNetworkState(float delta) {
        if (!netInitialized) return;

        float t = 1f - (float) Math.exp(-NET_SMOOTHING_RATE * delta);
        x += (netTargetX - x) * t;
        y += (netTargetY - y) * t;
        rotation += shortestAngleDelta(rotation, netTargetRotation) * t;
        updateTransform();
    }

    private static float shortestAngleDelta(float from, float to) {
        float diff = (to - from) % 360f;
        if (diff < -180f) diff += 360f;
        if (diff > 180f) diff -= 360f;
        return diff;
    }

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

