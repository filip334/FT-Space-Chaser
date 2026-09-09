package io.github.filip334.spacechaser.network.message;

import java.io.Serializable;

public class EntityState implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public float x;
    public float y;
    public float rotation;

    public float width;
    public float height;

    public float health;
    public float maxHealth;
    public float fuel;
    public float maxFuel;
    public int score;

    public boolean thrusting;
    public boolean boosting;
}