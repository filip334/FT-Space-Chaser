package io.github.filip334.spacechaser.enemy;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.filip334.spacechaser.entity.Enemy;

public class PatrolState implements EnemyState {

    private static final float ARRIVE_DISTANCE = 20f;
    private static final float IDLE_CHANCE = 0.3f;
    private static final float IDLE_MIN = 0.4f;
    private static final float IDLE_MAX = 1.4f;

    private Vector2 target;
    private float idleTimer;

    // ---------------- ENTER ----------------

    @Override
    public void enter(Enemy enemy) {
        target = enemy.pickPatrolPoint();
        idleTimer = 0f;
    }

    // ---------------- UPDATE ----------------

    @Override
    public void update(Enemy enemy, float delta) {
        if (enemy.canSeePlayer()) {
            enemy.getStateMachine().changeState(enemy, new ChaseState());
            return;
        }

        if (idleTimer > 0f) {
            idleTimer -= delta;
            return;
        }

        if (target == null || enemy.getPosition().dst(target) < ARRIVE_DISTANCE) {
            pickNextTarget(enemy);
            return;
        }

        enemy.moveToTarget(target, delta);
        if (enemy.wasMovementBlocked()) {
            pickNextTarget(enemy);
        }
    }

    private void pickNextTarget(Enemy enemy) {
        target = enemy.pickPatrolPoint();
        if (MathUtils.randomBoolean(IDLE_CHANCE)) {
            idleTimer = MathUtils.random(IDLE_MIN, IDLE_MAX);
        }
    }

    // ---------------- EXIT ----------------

    @Override
    public void exit(Enemy enemy) {
    }
}
