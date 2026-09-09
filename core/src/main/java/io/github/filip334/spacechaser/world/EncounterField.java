package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Wall;

public class EncounterField {

    private static final int GRID_COLS = 10;
    private static final int GRID_ROWS = 10;

    private static final float WALL_THICKNESS = 6f;
    private static final float MAP_PADDING = 8f;
    private static final float CELL_SIZE = 97f;
    private static final float POST_SIZE = 13f;

    private Array<Wall> walls;

    private float cellWidth;
    private float cellHeight;
    private float fieldX;
    private float fieldY;
    private float fieldWidth;
    private float fieldHeight;

    // ---------------- KONSTRUKTOR ----------------

    public EncounterField() {
        walls = new Array<>();
        generateStaticMap();
    }

    // ---------------- MAP GENERATION ----------------

    private void generateStaticMap() {
        fieldX = MAP_PADDING;
        fieldY = MAP_PADDING;
        fieldWidth = GRID_COLS * CELL_SIZE;
        fieldHeight = GRID_ROWS * CELL_SIZE;

        cellWidth = CELL_SIZE;
        cellHeight = CELL_SIZE;

        walls.clear();

        addHorizontalWall(0, GRID_COLS, 0);
        addHorizontalWall(0, GRID_COLS, GRID_ROWS);
        addVerticalWall(0, GRID_ROWS, 0);
        addVerticalWall(0, GRID_ROWS, GRID_COLS);

        buildRingWithGaps(1, GRID_COLS - 1, 1, GRID_ROWS - 1, 2f);

        buildRingWithGaps(2, GRID_COLS - 2, 2, GRID_ROWS - 2, 2f);

        addVerticalWall(3,7,3);
        addVerticalWall(3,7,7);

        addVerticalWall(3,4,4);
        addVerticalWall(3,4,6);
        addVerticalWall(6,7,4);
        addVerticalWall(6,7,6);

        addPost(4, 3);
        addPost(4, 4);


        addPost(6, 3);
        addPost(6, 4);

        addPost(4, 6);
        addPost(4, 7);

        addPost(6, 6);
        addPost(6, 7);

        addPost(3, 3);
        addPost(3, 7);

        addPost(7, 3);
        addPost(7, 7);

        addPost(1, 4);
        addPost(1, 6);
        addPost(9, 4);
        addPost(9, 6);

        addPost(2, 4);
        addPost(2, 6);
        addPost(8, 4);
        addPost(8, 6);


        addPost(4, 9);
        addPost(6, 9);
        addPost(4, 1);
        addPost(6, 1);

        addPost(4, 2);
        addPost(6, 2);
        addPost(4, 8);
        addPost(6, 8);

        addCornerPosts(1, GRID_COLS - 1, 1, GRID_ROWS - 1);
        addCornerPosts(2, GRID_COLS - 2, 2, GRID_ROWS - 2);
    }

    private void buildRingWithGaps(float minCol, float maxCol, float minRow, float maxRow, float gapSize) {
        float midCol = (minCol + maxCol) / 2f;
        float midRow = (minRow + maxRow) / 2f;

        float gapStartCol = midCol - gapSize / 2f;
        float gapEndCol = midCol + gapSize / 2f;
        float gapStartRow = midRow - gapSize / 2f;
        float gapEndRow = midRow + gapSize / 2f;

        addHorizontalWall(minCol, gapStartCol, minRow);
        addHorizontalWall(gapEndCol, maxCol, minRow);
        addHorizontalWall(minCol, gapStartCol, maxRow);
        addHorizontalWall(gapEndCol, maxCol, maxRow);

        addVerticalWall(minRow, gapStartRow, minCol);
        addVerticalWall(gapEndRow, maxRow, minCol);
        addVerticalWall(minRow, gapStartRow, maxCol);
        addVerticalWall(gapEndRow, maxRow, maxCol);
    }

    private void addCornerPosts(float minCol, float maxCol, float minRow, float maxRow) {
        addPost(minCol, minRow);
        addPost(maxCol, minRow);
        addPost(minCol, maxRow);
        addPost(maxCol, maxRow);
    }

    private void addPost(float col, float row) {
        float x = fieldX + col * cellWidth - POST_SIZE / 2f;
        float y = fieldY + row * cellHeight - POST_SIZE / 2f;
        walls.add(new Wall(x, y, POST_SIZE, POST_SIZE));
    }

    private void addHorizontalWall(float startCol, float endCol, float row) {
        float x = fieldX + startCol * cellWidth;
        float y = fieldY + row * cellHeight - WALL_THICKNESS / 2f;
        float width = (endCol - startCol) * cellWidth;

        walls.add(new Wall(x, y, width, WALL_THICKNESS));
    }

    private void addVerticalWall(float startRow, float endRow, float col) {
        float x = fieldX + col * cellWidth - WALL_THICKNESS / 2f;
        float y = fieldY + startRow * cellHeight;
        float height = (endRow - startRow) * cellHeight;

        walls.add(new Wall(x, y, WALL_THICKNESS, height));
    }

    // ---------------- GET / SET ----------------

    public Array<Wall> getWalls() {
        return walls;
    }

    public float getFieldX() {
        return fieldX;
    }

    public float getFieldY() {
        return fieldY;
    }

    public float getFieldWidth() {
        return fieldWidth;
    }

    public float getFieldHeight() {
        return fieldHeight;
    }

    public float getCellSize() {
        return cellWidth;
    }

    // ---------------- UPDATE ----------------

    public void update() {
    }

    // ---------------- RENDER ----------------

    public void render(ShapeRenderer shapeRenderer) {
        for (Wall wall : walls) {
            wall.render(shapeRenderer);
        }
    }
}
