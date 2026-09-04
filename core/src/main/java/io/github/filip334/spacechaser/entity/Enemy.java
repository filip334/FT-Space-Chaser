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

    
    public final float DAMAGE = 33f;
    
    private static final float PATROL_MARGIN = 80f;
    private static final float WALL_AVOIDANCE_RADIUS = 110f;
    private static final float TURN_SPEED = 240f;

    // ---------------- CONSTRUCTOR ----------------

    public Enemy() {
        health = new HealthComponent(3f);
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
        
        this.width = 50;
        this.height = 20;
        health = new HealthComponent(99f);
        
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

        if (navigationWalls == null) {
            // Fallback za klijentsko ogledalo, koje ne pokrece AI.
            points.add(new Vector2(x, y));
            points.add(new Vector2(x + 200, y));
            points.add(new Vector2(x + 200, y + 200));
            points.add(new Vector2(x, y + 200));
        } else {
            for (int i = 0; i < 4; i++) {
                points.add(findRandomReachablePatrolPoint());
            }
        }

        return new PatrolState(points);
    }

    /** Postavlja granice mape i zidove koje AI mora da izbegava. */
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

    /**
     * Generalizovano kretanje ka bilo kojoj meti (igrac ili patrol tacka).
     *
     * FIX: ranije je PatrolState zvao samo moveTowards() (cist lokalni
     * steering + izbegavanje zida). U konkavnim uglovima / tesnim
     * prolazima moze da se desi da NIJEDAN od tri lokalna pokusaja
     * (pravo, levo, desno) ne uspe da prodje - raketa ostaje fizicki
     * zaglavljena na istoj poziciji. Kako je PatrolState svaki frejm
     * u tom slucaju birao NOVU nasumicnu patrol tacku (movementBlocked),
     * pravac ka novoj (nasumicnoj) meti se menjao svaki frejm dok je
     * pozicija ostajala ista - to je izgledalo kao "freeze + trzanje
     * ugla". moveToPlayer() je vec imao A* fallback za tacno ovaj
     * slucaj (zato je chase odmah "provalio" kroz prepreku) - sada
     * isti fallback koristi i patrola, preko ove generalizovane metode.
     */
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

    private Vector2 findRandomReachablePatrolPoint() {
        for (int attempt = 0; attempt < 30; attempt++) {
            Vector2 candidate = new Vector2(
                    MathUtils.random(navigationMinX, navigationMaxX),
                    MathUtils.random(navigationMinY, navigationMaxY));

            if (isPathClear(candidate)) {
                return candidate;
            }
        }

        return new Vector2(x, y);
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

        if (pushX == 0f && pushY == 0f) {
            return;
        }

        // FIX: pre ove izmene se pomeraj primenjivao bez provere zida,
        // pa su dve rakete koje se guraju blizu zida mogle da se "zaglave"
        // unutar njega (svaki naredni tryMove() bi detektovao overlap i
        // vratio poziciju na tu vec zaglavljenu tacku).
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

        if (tryMove(steering, delta)) {
            return;
        }

        // Kada je put pravo ispred zatvoren, probaj da zaobidjes zid s leve ili desne strane.
        Vector2 left = new Vector2(-direction.y, direction.x).scl(1.25f).add(direction).nor();
        if (tryMove(left, delta)) {
            return;
        }

        Vector2 right = new Vector2(direction.y, -direction.x).scl(1.25f).add(direction).nor();
        if (tryMove(right, delta)) {
            return;
        }

        velocityX = 0f;
        velocityY = 0f;
        // Svi pokusaji su udarili u zid. Vrati poslednji stabilan ugao da
        // raketa ne bi vizuelno skakala levo-desno pri svakom frejmu.
        rotation = previousRotation;
        movementBlocked = true;
        updateHitbox();
    }

    /**
     * Pracenje A* waypoint-a ne koristi lokalno odbijanje od zida: waypoint je
     * vec izabran kao bezbedan, pa bi dodatno skretanje izazvalo osciliranje
     * levo-desno uz uglove prepreka.
     */
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

        if (tryMove(direction, delta)) {
            return;
        }

        velocityX = 0f;
        velocityY = 0f;
        rotation = previousRotation;
        movementBlocked = true;
        updateHitbox();
    }

    private boolean tryMove(Vector2 direction, float delta) {
        // FIX: sacuvaj ugao PRE pokusaja, ne samo poziciju. Pre ove izmene,
        // kada bi ovaj pokusaj udario u zid, x/y su se vracali na
        // previousX/Y ali je "rotation" ostajao promenjen ka neuspelom
        // smeru. moveTowards() zove tryMove() do 3 puta po frejmu
        // (steering -> left -> right), pa se rotacija akumulirala kroz
        // sva tri neuspela pokusaja u istom frejmu -> raketa je izgledala
        // "zamrznuto" (pozicija se nije menjala) uz brzo skakanje ugla
        // levo-desno (rotacija se menjala 3x ka razlicitim smerovima).
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

    /** Ogranicava okretanje, tako da promena waypointa ne izazove vizuelni skok od 180 stepeni. */
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