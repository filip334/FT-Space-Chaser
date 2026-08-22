package io.github.filip334.spacechaser.enemy;

import com.badlogic.gdx.math.Vector2;
import io.github.filip334.spacechaser.entity.Enemy;

public class SearchState implements EnemyState {

    private final Vector2 searchPosition;

    private float timer;

    private static final float SEARCH_TIME = 3f;

    public SearchState(Vector2 searchPosition) {
        this.searchPosition = searchPosition;
    }

    @Override
    public void enter(Enemy enemy) {

        timer = 0f;
    }

    @Override
    public void update(Enemy enemy, float delta) {

        if (enemy.canSeePlayer()) {

            enemy.getStateMachine().changeState(
                    enemy,
                    new ChaseState()
            );

            return;
        }

        enemy.moveTowards(searchPosition, delta);

        timer += delta;

        if (timer >= SEARCH_TIME) {

            enemy.getStateMachine().changeState(
                    enemy,
                    enemy.createPatrolState()
            );
        }
    }

    @Override
    public void exit(Enemy enemy) {
    }
}