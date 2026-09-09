package io.github.filip334.spacechaser.collision;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class CompoundHitbox {

    //
    private final Array<Polygon> localBoxes = new Array<>();
    private final Array<Polygon> worldBoxes = new Array<>();
    
    //
    private float ownerX;
    private float ownerY;
    private float ownerRotation;

    // ---------------- METHODS ----------------
    public void addBox(float localX,float localY,float width,float height){
        float[] vertices = new float[]{
                -width / 2f, -height / 2f,
                 width / 2f, -height / 2f,
                 width / 2f,  height / 2f,
                -width / 2f,  height / 2f
        };

        Polygon box = new Polygon(vertices);

        // Polygon pozicija predstavlja CENTAR boxa
        box.setPosition(localX, localY);

        localBoxes.add(box);
        worldBoxes.add(new Polygon(vertices));
    }
    
    public boolean overlaps(CompoundHitbox other) {

        for (Polygon a : worldBoxes) {
            for (Polygon b : other.worldBoxes) {

                if (Intersector.overlapConvexPolygons(a, b)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean overlaps(Polygon polygon) {

        for (Polygon box : worldBoxes) {

            if (Intersector.overlapConvexPolygons(box, polygon)) {
                return true;
            }
        }

        return false;
    }

    // ---------------- GET / SET ----------------
    public Array<Polygon> getWorldBoxes() {
        return worldBoxes;
    }

    // ---------------- UPDATE ----------------
    
    public void update(float x,float y,float rotation){
        
        this.ownerX = x;
        this.ownerY = y;
        this.ownerRotation = rotation;

        updateWorldBoxes();
    }
    private void updateWorldBoxes(){
        
        for (int i = 0; i < localBoxes.size; i++) {

            Polygon local = localBoxes.get(i);
            Polygon world = worldBoxes.get(i);

            Vector2 localPosition =
                    new Vector2(local.getX(), local.getY());

            localPosition.rotate(ownerRotation);

            world.setPosition(
                    ownerX + localPosition.x,
                    ownerY + localPosition.y
            );

            world.setRotation(ownerRotation);
        }
    }
    
    // ---------------- RENDER ----------------
    
    public void debugRender(ShapeRenderer shapeRenderer){
        
        shapeRenderer.setColor(Color.RED);

        for (Polygon box : worldBoxes) {

            float[] vertices = box.getTransformedVertices();

            for (int i = 0; i < vertices.length; i += 2) {

                int next = (i + 2) % vertices.length;

                shapeRenderer.line(
                        vertices[i],
                        vertices[i + 1],
                        vertices[next],
                        vertices[next + 1]
                );
            }
        }
    }
}