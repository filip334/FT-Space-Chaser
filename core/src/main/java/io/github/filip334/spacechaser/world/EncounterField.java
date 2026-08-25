package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Wall;

public class EncounterField {
    
    //
    private static final int GRID_COLS = 10;
    private static final int GRID_ROWS = 10;

    private static final float WALL_THICKNESS = 6f;
    private static final float MAP_PADDING = 12f;
    private static final float CELL_SIZE = 70f;
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
        fieldWidth = GRID_COLS * CELL_SIZE;
        fieldHeight = GRID_ROWS * CELL_SIZE;

        cellWidth = CELL_SIZE;
        cellHeight = CELL_SIZE;

        walls.clear();

        addHorizontalWall(0, 10, 0);
        addHorizontalWall(0, 10, 10);
        addVerticalWall(0, 10, 0);
        addVerticalWall(0, 10, 10);

        // Svaki poziv dodaje i odraz oko vertikalne i horizontalne ose mape.
        // Cetiri kratka, simetricna ostrva. Izmedju njih je sirok centralni
        // prolaz, a svaki spoljasnji prolaz je dovoljno sirok za rotaciju broda.
        
        /*//dole levo
        addHorizontalWall(1, 5, 1);
        addVerticalWall(1, 5, 1);
        
        //gore levo
        addVerticalWall(6, 9, 1);
        addHorizontalWall(1, 5, 9);
        
        //dole levo
        addHorizontalWall(3, 5, 3);
        addVerticalWall(3, 5, 3);
        
        //gore levo
        addVerticalWall(6, 8, 3);
        addHorizontalWall(3, 5, 8);
        
        //centar levo
        addVerticalWall(4, 7, 4);
        
        //centar desno
        addVerticalWall(4, 5, 5);
        addVerticalWall(6, 7, 5);
        
        //DESNA
        
        //dole desno
        addHorizontalWall(6, 9, 1);
        addVerticalWall(1, 5, 9);
        
        //gore desno
        addVerticalWall(6, 9, 9);
        addHorizontalWall(6, 9, 9);
        
        //dole desno
        addHorizontalWall(6, 8, 3);
        addVerticalWall(3, 5, 8);
        
        //gore desno
        addVerticalWall(6, 8, 8);
        addHorizontalWall(6, 8, 8);
        
        //centar desno
        addVerticalWall(4, 7, 7);
        
        //centar levo
        addVerticalWall(4, 5, 6);
        addVerticalWall(6, 7, 6);*/
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
