package io.github.filip334.spacechaser.server;

import java.io.Serializable;

public class EntityState implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public float x;
    public float y;
    public float rotation;

    // koriste ga samo walls (staticne dimenzije)
    public float width;
    public float height;

    // relevantno samo za igrace, za enemy/bullet ostaje 0
    public float health;
    public float maxHealth;
    public float fuel;
    public float maxFuel;

    // da li igrac trenutno pritiska napred (za thrust animaciju na oba klijenta)
    public boolean thrusting;
    public boolean boosting;
}