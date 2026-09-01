package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.filip334.spacechaser.collision.CompoundHitbox;

public class Bullet extends Entity {
    // 
    private float dirX;
    private float dirY;

    // ---------------- CONSTRUCTOR ----------------
    
    public Bullet() {
    }

    public Bullet(float x, float y, float dirX, float dirY, float rotation) {
        //collisionType = CollisionType.PLAYER_BULLET;
        
        this.x = x;
        this.y = y;
        
        previousX = x;
        previousY = y;
        
        this.dirX = dirX;
        this.dirY = dirY;
        
        this.width = 64;
        this.height = 64;
        
        this.rotation = rotation;
        
        this.acceleration = 700f;
        
        hitbox = new CompoundHitbox();
        hitbox.addBox(6, 0, 16, 4);
        hitbox.update(this.x, this.y, this.rotation);
    }
    
    // GET / SET

    public float getPreviousX() {
        return previousX;
    }

    public float getPreviousY() {
        return previousY;
    }
    
    // UPDATE / RENDER
    
    @Override
    public void update(float delta) {
        previousX = x;
        previousY = y;
        x += dirX * delta * acceleration;
        y += dirY * delta * acceleration;
        
        hitbox.update(x, y, rotation);
    }

    public void setNetworkState(float x, float y, float rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        hitbox.update(x, y, rotation);
    }
}
