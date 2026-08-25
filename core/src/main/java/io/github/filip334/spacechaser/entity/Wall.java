package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.filip334.spacechaser.collision.CompoundHitbox;

public class Wall extends Entity {
    
    // ---------------- CONSTRUCTOR ----------------
    public Wall(float x, float y, float width, float height) {

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        hitbox = new CompoundHitbox();

        hitbox.addBox(
            width / 2f,
            height / 2f,
            width,
            height
        );

        hitbox.update(x, y, 0f);
    }
    
    // ---------------- UPDATE ----------------
    
    @Override
    public void update(float delta) {
        // NEMA POMERANJA
    }

    // ---------------- RENDER ----------------
    
    public void render(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, width, height);
    }
}
