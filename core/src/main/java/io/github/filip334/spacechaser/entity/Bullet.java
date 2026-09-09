package io.github.filip334.spacechaser.entity;

import io.github.filip334.spacechaser.collision.CompoundHitbox;

public class Bullet extends Entity {
    private float dirX;
    private float dirY;

    public final float DAMAGE = 33f;

    private int ownerId = -1;

    // ---------------- KONSTRUKTORI ----------------

    public Bullet() {
    }

    public Bullet(float x, float y, float dirX, float dirY, float rotation) {
        this.x = x;
        this.y = y;
        
        previousX = x;
        previousY = y;
        
        this.dirX = dirX;
        this.dirY = dirY;
        
        this.width = 36;
        this.height = 36;
        
        this.rotation = rotation;
        
        this.acceleration = 700f;
        
        hitbox = new CompoundHitbox();
        hitbox.addBox(6, 0, 16, 4);
        hitbox.update(this.x, this.y, this.rotation);
    }
    
    // ---------------- UPDATE ----------------

    @Override
    public void update(float delta) {
        previousX = x;
        previousY = y;
        x += dirX * delta * acceleration;
        y += dirY * delta * acceleration;

        hitbox.update(x, y, rotation);
    }

    // ---------------- NETWORK ----------------

    public void setNetworkState(float x, float y, float rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        hitbox.update(x, y, rotation);
    }

    // ---------------- GET / SET ----------------

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }
}
