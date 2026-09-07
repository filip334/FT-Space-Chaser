package io.github.filip334.spacechaser.entity;

public class Coin extends Entity {
    public static final float RADIUS = 5f;

    private final int id;

    public Coin(int id, float x, float y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = RADIUS * 2f;
        this.height = RADIUS * 2f;
    }

    public int getId() {
        return id;
    }

    @Override
    public void update(float delta) {
        
    }
}
