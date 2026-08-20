package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Wall;

public class EncounterField {
    
    //
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 9;

    private static final float WALL_THICKNESS = 6f;
    private static final float MAP_PADDING = 12f;

    // 
    private Array<Wall> walls;

    //
    private float cellWidth;
    private float cellHeight;
    private float fieldX;
    private float fieldY;
    private float fieldWidth;
    private float fieldHeight;
    private int screenWidth;
    private int screenHeight;

    public EncounterField() {
        walls = new Array<>();
        generateStaticMap();
    }

    private void generateStaticMap() {
        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();
        fieldX = MAP_PADDING;
        fieldY = MAP_PADDING;
        fieldWidth = screenWidth - MAP_PADDING * 2f;
        fieldHeight = screenHeight - MAP_PADDING * 2f;

        cellWidth = fieldWidth / GRID_COLS;
        cellHeight = fieldHeight / GRID_ROWS;

        walls.clear();

        addHorizontalWall(0, 9, 0);
        addHorizontalWall(0, 9, 9);
        addVerticalWall(0, 0, 9);
        addVerticalWall(9, 0, 9);

        // Svaki poziv dodaje i odraz oko vertikalne i horizontalne ose mape.
        // Cetiri kratka, simetricna ostrva. Izmedju njih je sirok centralni
        // prolaz, a svaki spoljasnji prolaz je dovoljno sirok za rotaciju broda.
        addMirroredHorizontal(1.3f, 2.6f, 2.2f);
        addMirroredVertical(2.2f, 1.3f, 2.6f);
    }

    private void addHorizontalWall(float startCol, float endCol, float row) {
        float x = fieldX + startCol * cellWidth;
        float y = fieldY + row * cellHeight - WALL_THICKNESS / 2f;
        float width = (endCol - startCol) * cellWidth;

        walls.add(new Wall(x, y, width, WALL_THICKNESS));
    }

    private void addVerticalWall(float col, float startRow, float endRow) {
        float x = fieldX + col * cellWidth - WALL_THICKNESS / 2f;
        float y = fieldY + startRow * cellHeight;
        float height = (endRow - startRow) * cellHeight;

        walls.add(new Wall(x, y, WALL_THICKNESS, height));
    }

    private void addMirroredHorizontal(float startCol, float endCol, float row) {
        addHorizontalWall(startCol, endCol, row);
        addHorizontalWall(GRID_COLS - endCol, GRID_COLS - startCol, row);
        addHorizontalWall(startCol, endCol, GRID_ROWS - row);
        addHorizontalWall(GRID_COLS - endCol, GRID_COLS - startCol, GRID_ROWS - row);
    }

    private void addMirroredVertical(float col, float startRow, float endRow) {
        addVerticalWall(col, startRow, endRow);
        addVerticalWall(GRID_COLS - col, startRow, endRow);
        addVerticalWall(col, GRID_ROWS - endRow, GRID_ROWS - startRow);
        addVerticalWall(GRID_COLS - col, GRID_ROWS - endRow, GRID_ROWS - startRow);
    }

    // ---------------- GET/SET ----------------
    
    public Array<Wall> getWalls() {
        return walls;
    }

    // ---------------- UPDATE ----------------
    
    public void update() {
        if (screenWidth != Gdx.graphics.getWidth() || screenHeight != Gdx.graphics.getHeight()) {
            generateStaticMap();
        }
    }

    // ---------------- RENDER ----------------
    
    public void render(ShapeRenderer shapeRenderer) {
        for (Wall wall : walls) {
            wall.render(shapeRenderer);
        }
    }
}
