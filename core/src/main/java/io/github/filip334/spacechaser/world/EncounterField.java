package io.github.filip334.spacechaser.world;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Wall;

public class EncounterField {

    //
    private static final int GRID_COLS = 16;
    private static final int GRID_ROWS = 16;

    private static final float WALL_THICKNESS = 6f;
    private static final float MAP_PADDING = 12f;
    private static final float CELL_SIZE = 60f;
    private static final float POST_SIZE = 18f;
    //
    private Array<Wall> walls;

    //
    private float cellWidth;
    private float cellHeight;
    private float fieldX;
    private float fieldY;
    private float fieldWidth;
    private float fieldHeight;

    public EncounterField() {
        walls = new Array<>();
        generateStaticMap();
    }

    private void generateStaticMap() {
        // Mapa je fiksne velicine, NE zavisi od velicine prozora -
        // isto na svakom klijentu i na serveru, bez obzira na rezoluciju.
        fieldX = MAP_PADDING;
        fieldY = MAP_PADDING;
        fieldWidth = GRID_COLS * CELL_SIZE;
        fieldHeight = GRID_ROWS * CELL_SIZE;

        cellWidth = CELL_SIZE;
        cellHeight = CELL_SIZE;

        walls.clear();

        // Spoljna granica cele mape
        addHorizontalWall(0, GRID_COLS, 0);
        addHorizontalWall(0, GRID_COLS, GRID_ROWS);
        addVerticalWall(0, GRID_ROWS, 0);
        addVerticalWall(0, GRID_ROWS, GRID_COLS);

        // Spoljni prsten - prolaz na sredini svake strane (kao original)
        buildRingWithGaps(3, GRID_COLS - 3, 3, GRID_ROWS - 3, 2f);

        // Unutrasnji prsten - manji, blize centru
        buildRingWithGaps(6, GRID_COLS - 6, 6, GRID_ROWS - 6, 1.5f);

        // Mali "postovi" na uglovima oba prstena - stilski akcenat kao na originalu
        addCornerPosts(3, GRID_COLS - 3, 3, GRID_ROWS - 3);
        addCornerPosts(6, GRID_COLS - 6, 6, GRID_ROWS - 6);
    }

    /**
     * Gradi kvadratni prsten od (minCol,minRow) do (maxCol,maxRow) sa prolazom
     * sirine gapSize na sredini svake od 4 strane, da brod moze da prelazi
     * izmedju prstenova - isto kao original.
     */
    private void buildRingWithGaps(float minCol, float maxCol, float minRow, float maxRow, float gapSize) {
        float midCol = (minCol + maxCol) / 2f;
        float midRow = (minRow + maxRow) / 2f;

        float gapStartCol = midCol - gapSize / 2f;
        float gapEndCol = midCol + gapSize / 2f;
        float gapStartRow = midRow - gapSize / 2f;
        float gapEndRow = midRow + gapSize / 2f;

        // Gornja i donja strana (sa prolazom u sredini)
        addHorizontalWall(minCol, gapStartCol, minRow);
        addHorizontalWall(gapEndCol, maxCol, minRow);
        addHorizontalWall(minCol, gapStartCol, maxRow);
        addHorizontalWall(gapEndCol, maxCol, maxRow);

        // Leva i desna strana (sa prolazom u sredini)
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

    // ---------------- GET/SET ----------------

    public Array<Wall> getWalls() {
        return walls;
    }

    /**
     * Granice igrivog polja mape - koristi OVO za spawn pozicije
     * (enemy, player), nikad Gdx.graphics.getWidth()/getHeight().
     */
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

    // ---------------- UPDATE ----------------

    public void update() {
        // Mapa je staticna i ne zavisi od velicine prozora - nema potrebe
        // da se regenerise. Metoda ostaje zbog poziva iz GameWorld.update().
    }

    // ---------------- RENDER ----------------

    public void render(ShapeRenderer shapeRenderer) {
        for (Wall wall : walls) {
            wall.render(shapeRenderer);
        }
    }
}