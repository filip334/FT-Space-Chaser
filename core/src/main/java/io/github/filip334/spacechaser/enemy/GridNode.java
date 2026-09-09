package io.github.filip334.spacechaser.enemy;

public class GridNode {

    public int x;
    public int y;

    public boolean walkable;

    public float gCost;
    public float hCost;

    public GridNode parent;

    // ---------------- KONSTRUKTOR ----------------

    public GridNode(int x, int y, boolean walkable) {
        this.x = x;
        this.y = y;
        this.walkable = walkable;
    }

    public float getFCost() {
        return gCost + hCost;
    }
}