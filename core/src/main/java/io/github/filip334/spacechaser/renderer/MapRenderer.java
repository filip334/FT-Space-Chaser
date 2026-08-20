package io.github.filip334.spacechaser.renderer;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.filip334.spacechaser.entity.Wall;
import io.github.filip334.spacechaser.world.EncounterField;

public class MapRenderer {
     private final EncounterField encounterField;

    public MapRenderer(EncounterField encounterField) {
        this.encounterField = encounterField;
    }

    public void render(ShapeRenderer shapeRenderer) {
        for (Wall wall : encounterField.getWalls()) {
            wall.render(shapeRenderer);
        }
    }
}
