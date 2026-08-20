package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.collision.CompoundHitbox;

public class Enemy extends Entity{
    //
    
    protected float velocityX, velocityY;
    protected float speed = 120f;
    
    private float previousX;
    private float previousY;

    // ---------------- CONSTRUCTOR ----------------
    
    public Enemy(float x, float y) {
        //collisionType = CollisionType.ENEMY;

        this.x = x;
        this.y = y;
        
        this.width = 96;
        this.height = 96;
        
        hitbox = new CompoundHitbox();
        hitbox.addBox(0, 0, width/2+10, 20);

        // TEXTURE DEF
        entityTexture = new Texture("Original/projectile.png");
    }
    
    // RAKETE SE NE POKLAPAJU
    
    public void applySeparation(Array<Enemy> enemies, float delta) {
        float separationRadius = 60f;
        float pushStrength = 180f;

        float pushX = 0f;
        float pushY = 0f;

        for (Enemy other : enemies) {
            if (other == this || other.isDead()) {
                continue;
            }

            float dx = this.x - other.getX();
            float dy = this.y - other.getY();

            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance > 0 && distance < separationRadius) {
                float force = 1f - (distance / separationRadius);

                pushX += (dx / distance) * force;
                pushY += (dy / distance) * force;
            }
        }

        x += pushX * pushStrength * delta;
        y += pushY * pushStrength * delta;
        updateHitbox();
    }
    
    public void restorePreviousPosition() {
        x = previousX;
        y = previousY;
        updateHitbox();
    }

    private void updateHitbox() {
        hitbox.update(x, y, rotation);
    }
    
    // UPDATE / RENDER / DISPOSE
    
    @Override
    public void update(float delta) {
        hitbox.update(x, y, rotation);
    }

    public void update(float delta, Player player) {
        float targetX = player.getX();
        float targetY = player.getY();

        float centerX = x;
        float centerY = y;

        float dirX = targetX - centerX;
        float dirY = targetY - centerY;

        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);

        if (length != 0) {
            dirX /= length;
            dirY /= length;
        }

        previousX = x;
        previousY = y;
        x += dirX * speed * delta;
        y += dirY * speed * delta;

        rotation = (float) Math.toDegrees(Math.atan2(dirY, dirX));

        updateHitbox();
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.draw(
            entityTexture,
            x - width / 2f,
            y - height / 2f,
            width / 2f,
            height / 2f,
            width,
            height,
            1f,
            1f,
            rotation,
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
