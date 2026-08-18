/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.filip334.spacechaser.collision.CompoundHitbox;
import io.github.filip334.spacechaser.input.PlayerInput;
/**
 *
 * @author Todorovic
 */
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
    private float attemptedX;

    private final float maxSpeed = 500f;
    private final float acceleration = 350f;
    private float rotation = 0f;
    private final float rotationSpeed = 110f;
    private final float drag = 0.97f;

    //
    private Texture idleAnimation;
    
    // COMBAT
    private float attackTimer = 0f;

    
    float originX;
    float originY;
    
    
    public Player(float x,float y) {
        this.x = x;
        this.y = y;
        
        String texturePath = "Fighter/";
        String idlePath = texturePath + "Idle.png";
        
        idleAnimation = new Texture(idlePath);
        ship = new Sprite(idleAnimation);
        
        width = 192;
        height = 192;
        ship.setSize(width, height);
        ship.setOriginCenter();
        
        originX = ship.getWidth() / 2f;
        originY = ship.getHeight() / 2f;
        
        hitbox = new CompoundHitbox();
    }
    
    
    
    
    private void readInput() {

        input.left = Gdx.input.isKeyPressed(Input.Keys.A);
        input.right = Gdx.input.isKeyPressed(Input.Keys.D);
        input.forward = Gdx.input.isKeyPressed(Input.Keys.W)/*&& fuel.hasFuel()*/;
        input.backward = Gdx.input.isKeyPressed(Input.Keys.S);
        input.boost = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)/*&& fuel.hasFuel()*/;
        input.shoot = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        if (input.shoot) {
            //attackTimer = 0.18f;
        }
    }
    private void handleMovement(float delta) {

        boolean isBoosting = input.boost && input.forward;
        float currentAcceleration = isBoosting ? 550f : acceleration;

        /*if (isBoosting) {
            fuel.consume(20f * delta);
        }*/

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
        attemptedX = x;

        velocityX *= Math.pow(drag, delta * 60);
        velocityY *= Math.pow(drag, delta * 60);

        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        if (speed > maxSpeed) {
            float scale = maxSpeed / speed;
            velocityX *= scale;
            velocityY *= scale;
        }
    }
    
    @Override
    public void update(float delta) {
        // UPDATE INPUT
        readInput();

        // UPDATE ANIMATION SYSTEM
        //animator.update(delta);

        if (attackTimer > 0f) {
            attackTimer -= delta;
        }

        handleMovement(delta);
        //handleAnimation();

        ship.setRotation(rotation);
        ship.setPosition(x, y);

        //ship.setRegion(animator.getFrame());
        //updateHitbox();
    }

    @Override
    public void render(SpriteBatch batch) {
        ship.draw(batch);
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
