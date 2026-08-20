package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.filip334.spacechaser.collision.CompoundHitbox;

public class Bullet extends Entity {
    // 
    private float dirX;
    private float dirY;
    
    private float previousX;
    private float previousY;
    
    // SPEED
    private final float speed = 700f;

    // ---------------- CONSTRUCTOR ----------------
    
    public Bullet(float x, float y, float dirX, float dirY, Texture texture) {
       
        //collisionType = CollisionType.PLAYER_BULLET;
        
        this.x = x;
        this.y = y;
        
        previousX = x;
        previousY = y;
        
        this.dirX = dirX;
        this.dirY = dirY;
        
        this.width = 64;
        this.height = 64;
        
        entityTexture = texture;
        
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
        x += dirX * delta * speed;
        y += dirY * delta * speed;
        
        hitbox.update(x, y, rotation);
    }

    @Override
    public void render(SpriteBatch batch) {
        rotation = (float) Math.toDegrees(Math.atan2(dirY, dirX));
        
        batch.draw(
            entityTexture,
            this.x - this.width/2f,
            this.y - this.height/2f,
            this.width/2f,
            this.height/2f,
            this.width,
            this.height,
            1f,
            1f,
            this.rotation,
            0,
            0,
            entityTexture.getWidth(),
            entityTexture.getHeight(),
            false,
            false
        );
    }
    
    public void debugRender(ShapeRenderer shapeRenderer){
        hitbox.debugRender(shapeRenderer);
    }
}
