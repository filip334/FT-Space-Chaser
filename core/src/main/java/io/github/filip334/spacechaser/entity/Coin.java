package io.github.filip334.spacechaser.entity;

public class Coin extends Entity {
    public static final float RADIUS = 8f;

    private final int id;

    public Coin(int id, float x, float y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = 16f;
        this.height = 16f;
    }

    public int getId() {
        return id;
    }

    @Override
    public void update(float delta) {
        
    }
}
