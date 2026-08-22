/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.filip334.spacechaser.collision;

import com.badlogic.gdx.math.Vector2;

/**
 *
 * @author Todorovic
 */
public class CollisionResult {
    public boolean collided;
    public Vector2 normal = new Vector2();

    public CollisionResult() {
        collided = false;
    }

    public void set(Vector2 normal) {
        this.collided = true;
        this.normal.set(normal);
    }

    public void reset() {
        collided = false;
        normal.setZero();
    }
}
