package io.github.filip334.spacechaser.entity;

public class Coin extends Entity {
    public static final float RADIUS = 14f;
    
    private final int id;

    public Coin(int id, float x, float y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = 28f;
        this.height = 28f;
    }

    public int getId() {
        return id;
    }

    @Override
    public void update(float delta) {
        
    }
}
