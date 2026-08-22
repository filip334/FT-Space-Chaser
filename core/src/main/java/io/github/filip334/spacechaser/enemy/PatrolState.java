package io.github.filip334.spacechaser.enemy;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.filip334.spacechaser.entity.Enemy;

public class PatrolState implements EnemyState {

    private final Array<Vector2> patrolPoints;

    private int currentPoint;

    private float decisionTimer;

    public PatrolState(Array<Vector2> patrolPoints) {
        this.patrolPoints = patrolPoints;
    }

    @Override
    public void enter(Enemy enemy) {

        currentPoint = 0;
        decisionTimer = 0;
    }

    @Override
    public void update(Enemy enemy, float delta) {

        if (patrolPoints.size == 0) {
            return;
        }

        Vector2 target = patrolPoints.get(currentPoint);

        enemy.moveTowards(target, delta);

        if (enemy.getPosition().dst(target) < 20f) {

            currentPoint++;

            if (currentPoint >= patrolPoints.size) {
                currentPoint = 0;
            }
        }

        decisionTimer += delta;

        if (enemy.canSeePlayer()) {

            enemy.getStateMachine().changeState(
                    enemy,
                    new ChaseState()
            );
        }
    }

    @Override
    public void exit(Enemy enemy) {
    }
}