package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.collision.CompoundHitbox;
import io.github.filip334.spacechaser.enemy.PathFinder;
import io.github.filip334.spacechaser.enemy.PatrolState;
import io.github.filip334.spacechaser.enemy.StateMachine;

public class Enemy extends Entity{
    
    private Player player;
    
    private final StateMachine stateMachine;
    private PathFinder pathFinder;

    // ---------------- CONSTRUCTOR ----------------

    public Enemy() {
        stateMachine = new StateMachine();
        
        stateMachine.changeState(
            this,
            createPatrolState()
        );
    }
    
    
    
    public Enemy(float x, float y) {
        //collisionType = CollisionType.ENEMY;

        this.x = x;
        this.y = y;
        
        this.maxSpeed = 120f;
        this.acceleration = 120f;
        
        this.width = 96;
        this.height = 96;
        
        hitbox = new CompoundHitbox();
        hitbox.addBox(0, 0, width/2+10, 20);

        // TEXTURE DEF
        
        stateMachine = new StateMachine();
        
        stateMachine.changeState(
            this,
            createPatrolState()
        );
    }
    
    
    
    public PatrolState createPatrolState() {
        Array<Vector2> points = new Array<>();

        points.add(new Vector2(x, y));
        points.add(new Vector2(x + 200, y));
        points.add(new Vector2(x + 200, y + 200));
        points.add(new Vector2(x, y + 200));

        return new PatrolState(points);
    }
    
    
    // RAKETE SE NE POKLAPAJU
    
    public void applySeparation(Array<Enemy> enemies, float delta) {
        float separationRadius = 60f;
        float pushStrength = 180f;

        float pushX = 0f;
        float pushY = 0f;

        for (Enemy other : enemies) {
            if (other == this || other.isDead()) {
                continue;
            }

            float dx = this.x - other.getX();
            float dy = this.y - other.getY();

            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance > 0 && distance < separationRadius) {
                float force = 1f - (distance / separationRadius);

                pushX += (dx / distance) * force;
                pushY += (dy / distance) * force;
            }
        }

        x += pushX * pushStrength * delta;
        y += pushY * pushStrength * delta;
        updateHitbox();
    }
    
    public void restorePreviousPosition() {
        x = previousX;
        y = previousY;
        updateHitbox();
    }
    
    //
    public StateMachine getStateMachine() {
        return stateMachine;
    }
    public void setPathFinder(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }
    public PathFinder getPathFinder() {
        return pathFinder;
    }
    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
    public Vector2 getPosition() {
        return new Vector2(x, y);
    }
    public Vector2 getPlayerPosition() {
        return player.getPosition();
    }
    public float getSpeed() {
        return acceleration;
    }
    
    public void moveTowards(Vector2 target, float delta) {

        Vector2 direction = new Vector2(
                target.x - x,
                target.y - y
        );

        if (direction.isZero()) {
            velocityX = 0;
            velocityY = 0;
            return;
        }

        direction.nor();

        velocityX = direction.x * acceleration;
        velocityY = direction.y * acceleration;

        previousX = x;
        previousY = y;

        x += velocityX * delta;
        y += velocityY * delta;

        rotation = (float) Math.toDegrees(
                Math.atan2(direction.y, direction.x)
        );

        updateHitbox();
    }
    public boolean canSeePlayer() {

        if (player == null) {
            return false;
        }

        float distance =
                getPosition().dst(player.getPosition());

        return distance < 400f;
    }

    public void setNetworkState(float x, float y, float rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        updateHitbox();
    }
    
    // UPDATE / RENDER / DISPOSE
    
    @Override
    public void update(float delta) {
        stateMachine.update(this, delta);
        hitbox.update(x, y, rotation);
    }

    public void update(float delta,Player player){
        this.player = player;
        update(delta);
    }
    
    
}
/*public void update(float delta, Player player) {
        
        float targetX = player.getX();
        float targetY = player.getY();

        float centerX = x;
        float centerY = y;

        float dirX = targetX - centerX;
        float dirY = targetY - centerY;

        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);

        if (length != 0) {
            dirX /= length;
            dirY /= length;
        }

        previousX = x;
        previousY = y;
        x += dirX * speed * delta;
        y += dirY * speed * delta;

        rotation = (float) Math.toDegrees(Math.atan2(dirY, dirX));

        updateHitbox();
    }*/