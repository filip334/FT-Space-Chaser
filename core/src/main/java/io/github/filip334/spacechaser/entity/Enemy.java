package io.github.filip334.spacechaser.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.collision.CompoundHitbox;
import io.github.filip334.spacechaser.component.HealthComponent;
import io.github.filip334.spacechaser.enemy.PathFinder;
import io.github.filip334.spacechaser.enemy.GridNode;
import io.github.filip334.spacechaser.enemy.PatrolState;
import io.github.filip334.spacechaser.enemy.StateMachine;

public class Enemy extends Entity{
    
    private Player player;
    
    private final StateMachine stateMachine;
    private PathFinder pathFinder;
    private Array<Wall> navigationWalls;
    private float navigationMinX;
    private float navigationMaxX;
    private float navigationMinY;
    private float navigationMaxY;
    private boolean movementBlocked;
    private final Array<Vector2> chasePath = new Array<>();
    private int chasePathIndex;
    private float repathTimer;
    private float navigationCellSize;
    private boolean enraged;


    public final float DAMAGE = 33f;
    
    private static final float PATROL_MARGIN = 80f;
    private static final float WALL_AVOIDANCE_RADIUS = 45f;
    private static final float TURN_SPEED = 240f;

    private int scoreValue = 10;

    // ---------------- KONSTRUKTOR ----------------

    public Enemy(float x, float y) {
        this.x = x;
        this.y = y;
        
        this.maxSpeed = 120f;
        this.acceleration = 120f;
        
        this.width = 50;
        this.height = 20;
        health = new HealthComponent(99f);
        
        hitbox = new CompoundHitbox();
        hitbox.addBox(0, 0, width/2+15, 10);

        stateMachine = new StateMachine();
        
        stateMachine.changeState(
            this,
            createPatrolState()
        );
    }
    
    
    
    // ---------------- NAVIGATION / TARGETING ----------------

    public PatrolState createPatrolState() {
        return new PatrolState();
    }

    public void setNavigation(Array<Wall> walls, float fieldX, float fieldY,
                              float fieldWidth, float fieldHeight,
                              PathFinder pathFinder, float cellSize) {
        this.navigationWalls = walls;
        this.pathFinder = pathFinder;
        this.navigationCellSize = cellSize;
        navigationMinX = fieldX + PATROL_MARGIN;
        navigationMaxX = fieldX + fieldWidth - PATROL_MARGIN;
        navigationMinY = fieldY + PATROL_MARGIN;
        navigationMaxY = fieldY + fieldHeight - PATROL_MARGIN;
        stateMachine.changeState(this, createPatrolState());
    }

    public boolean isInNavigablePosition() {
        return navigationWalls == null || !overlapsNavigationWall();
    }

    public boolean wasMovementBlocked() {
        return movementBlocked;
    }

    public void moveToPlayer(float delta) {
        if (player == null) {
            return;
        }
        moveToTarget(player.getPosition(), delta);
    }

    public void moveToTarget(Vector2 target, float delta) {
        if (isPathClear(target)) {
            chasePath.clear();
            moveTowards(target, delta);
            return;
        }

        repathTimer -= delta;
        if (repathTimer <= 0f || chasePath.size == 0 || chasePathIndex >= chasePath.size || movementBlocked) {
            rebuildChasePath(target);
            repathTimer = 0.35f;
        }

        if (chasePathIndex >= chasePath.size) {
            movementBlocked = true;
            return;
        }

        Vector2 waypoint = chasePath.get(chasePathIndex);
        if (getPosition().dst(waypoint) < 16f) {
            chasePathIndex++;
            if (chasePathIndex >= chasePath.size) {
                return;
            }
            waypoint = chasePath.get(chasePathIndex);
        }

        moveToWaypoint(waypoint, delta);
    }

    private void rebuildChasePath(Vector2 target) {
        chasePath.clear();
        chasePathIndex = 0;

        if (pathFinder == null || navigationCellSize <= 0f) {
            return;
        }

        int startX = worldToGridX(x);
        int startY = worldToGridY(y);
        int targetX = worldToGridX(target.x);
        int targetY = worldToGridY(target.y);
        Array<GridNode> nodes = pathFinder.findPath(startX, startY, targetX, targetY);

        for (int i = 1; i < nodes.size; i++) {
            GridNode node = nodes.get(i);
            chasePath.add(new Vector2(
                    navigationMinX - PATROL_MARGIN + (node.x + 0.5f) * navigationCellSize,
                    navigationMinY - PATROL_MARGIN + (node.y + 0.5f) * navigationCellSize));
        }

        if (chasePath.size > 0) {
            chasePath.add(target.cpy());
        }
    }

    private int worldToGridX(float worldX) {
        return MathUtils.clamp((int) ((worldX - (navigationMinX - PATROL_MARGIN)) / navigationCellSize),
                0, (int) ((navigationMaxX - navigationMinX + PATROL_MARGIN * 2f) / navigationCellSize) - 1);
    }

    private int worldToGridY(float worldY) {
        return MathUtils.clamp((int) ((worldY - (navigationMinY - PATROL_MARGIN)) / navigationCellSize),
                0, (int) ((navigationMaxY - navigationMinY + PATROL_MARGIN * 2f) / navigationCellSize) - 1);
    }

    public Vector2 pickPatrolPoint() {
        if (navigationWalls == null || pathFinder == null) {
            return new Vector2(x, y);
        }

        int startGridX = worldToGridX(x);
        int startGridY = worldToGridY(y);

        for (int attempt = 0; attempt < 30; attempt++) {
            Vector2 candidate = new Vector2(
                    MathUtils.random(navigationMinX, navigationMaxX),
                    MathUtils.random(navigationMinY, navigationMaxY));

            if (isPathClear(candidate)) {
                return candidate;
            }

            int targetGridX = worldToGridX(candidate.x);
            int targetGridY = worldToGridY(candidate.y);
            if (pathFinder.findPath(startGridX, startGridY, targetGridX, targetGridY).size > 0) {
                return candidate;
            }
        }

        return new Vector2(x, y);
    }
    
    
    // ---------------- SEPARATION ----------------

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

        if (pushX == 0f && pushY == 0f) {
            return;
        }

        float savedX = x;
        float savedY = y;

        x += pushX * pushStrength * delta;
        y += pushY * pushStrength * delta;
        updateHitbox();

        if (overlapsNavigationWall()) {
            x = savedX;
            y = savedY;
            updateHitbox();
        }
    }
    
    public void restorePreviousPosition() {
        x = previousX;
        y = previousY;
        updateHitbox();
    }

    // ---------------- GET / SET ----------------

    public void setMaxHealth(float maxHealth) {
        this.health = new HealthComponent(maxHealth);
    }

    public int getScoreValue() {
        return scoreValue;
    }

    public void setScoreValue(int scoreValue) {
        this.scoreValue = scoreValue;
    }

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
    public Vector2 getPlayerPosition() {
        return player.getPosition();
    }
    public float getSpeed() {
        return acceleration;
    }
    
    // ---------------- MOVEMENT ----------------

    public void moveTowards(Vector2 target, float delta) {
        movementBlocked = false;
        previousRotation = rotation;
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
        previousX = x;
        previousY = y;

        Vector2 steering = direction.cpy().add(calculateWallAvoidance().scl(1.6f));
        if (!steering.isZero()) {
            steering.nor();
        } else {
            steering.set(direction);
        }

        attemptMove(direction, steering, 1.25f, delta);
    }

    private void moveToWaypoint(Vector2 target, float delta) {
        movementBlocked = false;
        previousRotation = rotation;

        Vector2 direction = new Vector2(target.x - x, target.y - y);
        if (direction.isZero()) {
            velocityX = 0f;
            velocityY = 0f;
            return;
        }

        direction.nor();
        previousX = x;
        previousY = y;

        attemptMove(direction, direction, 0.6f, delta);
    }

    private void attemptMove(Vector2 direction, Vector2 primaryDirection, float sideAngleScale, float delta) {
        if (tryMove(primaryDirection, delta)) {
            return;
        }

        Vector2 left = new Vector2(-direction.y, direction.x).scl(sideAngleScale).add(direction).nor();
        if (tryMove(left, delta)) {
            return;
        }

        Vector2 right = new Vector2(direction.y, -direction.x).scl(sideAngleScale).add(direction).nor();
        if (tryMove(right, delta)) {
            return;
        }

        velocityX = 0f;
        velocityY = 0f;
        rotation = previousRotation;
        movementBlocked = true;
        updateHitbox();
    }

    private boolean tryMove(Vector2 direction, float delta) {
        float baseRotation = rotation;

        float nextX = x + direction.x * acceleration * delta;
        float nextY = y + direction.y * acceleration * delta;
        float desiredRotation = (float) Math.toDegrees(Math.atan2(direction.y, direction.x));
        float nextRotation = rotateTowards(desiredRotation, delta);

        x = nextX;
        y = nextY;
        rotation = nextRotation;
        updateHitbox();

        if (overlapsNavigationWall()) {
            x = previousX;
            y = previousY;
            rotation = baseRotation;
            updateHitbox();
            return false;
        }

        velocityX = direction.x * acceleration;
        velocityY = direction.y * acceleration;
        return true;
    }

    private float rotateTowards(float desiredRotation, float delta) {
        float difference = (desiredRotation - rotation + 540f) % 360f - 180f;
        float maxTurn = TURN_SPEED * delta;
        return rotation + MathUtils.clamp(difference, -maxTurn, maxTurn);
    }

    private Vector2 calculateWallAvoidance() {
        Vector2 avoidance = new Vector2();
        if (navigationWalls == null) {
            return avoidance;
        }

        for (Wall wall : navigationWalls) {
            float closestX = MathUtils.clamp(x, wall.getX(), wall.getX() + wall.getWidth());
            float closestY = MathUtils.clamp(y, wall.getY(), wall.getY() + wall.getHeight());
            float dx = x - closestX;
            float dy = y - closestY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance >= WALL_AVOIDANCE_RADIUS) {
                continue;
            }

            if (distance < 0.001f) {
                dx = x - (wall.getX() + wall.getWidth() / 2f);
                dy = y - (wall.getY() + wall.getHeight() / 2f);
                distance = (float) Math.sqrt(dx * dx + dy * dy);
            }

            if (distance > 0.001f) {
                float strength = 1f - distance / WALL_AVOIDANCE_RADIUS;
                avoidance.add(dx / distance * strength, dy / distance * strength);
            }
        }

        return avoidance;
    }

    private boolean isPathClear(Vector2 target) {
        float savedX = x;
        float savedY = y;
        float savedRotation = rotation;
        float distance = savedX == target.x && savedY == target.y
                ? 0f : new Vector2(target.x - savedX, target.y - savedY).len();
        int steps = Math.max(1, (int) Math.ceil(distance / 24f));
        float pathRotation = (float) Math.toDegrees(Math.atan2(target.y - savedY, target.x - savedX));

        for (int step = 1; step <= steps; step++) {
            float progress = step / (float) steps;
            x = MathUtils.lerp(savedX, target.x, progress);
            y = MathUtils.lerp(savedY, target.y, progress);
            rotation = pathRotation;
            updateHitbox();

            if (overlapsNavigationWall()) {
                x = savedX;
                y = savedY;
                rotation = savedRotation;
                updateHitbox();
                return false;
            }
        }

        x = savedX;
        y = savedY;
        rotation = savedRotation;
        updateHitbox();
        return true;
    }

    private boolean overlapsNavigationWall() {
        if (navigationWalls == null) {
            return false;
        }

        for (Wall wall : navigationWalls) {
            if (hitbox.overlaps(wall.getHitbox())) {
                return true;
            }
        }
        return false;
    }

    // ---------------- PERCEPTION ----------------

    public boolean canSeePlayer() {

        if (player == null) {
            return false;
        }

        if (enraged) {
            return true;
        }

        float distance =
                getPosition().dst(player.getPosition());

        return distance < 400f;
    }

    public boolean isEnraged() {
        return enraged;
    }

    public void setEnraged(boolean enraged) {
        this.enraged = enraged;
    }

    // ---------------- NETWORK ----------------

    public void setNetworkState(float x, float y, float rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        updateHitbox();
    }

    // ---------------- UPDATE ----------------

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