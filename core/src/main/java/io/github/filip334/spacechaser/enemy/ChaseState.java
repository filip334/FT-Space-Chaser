package io.github.filip334.spacechaser.enemy;

import io.github.filip334.spacechaser.entity.Enemy;

public class ChaseState implements EnemyState {

    private float lostPlayerTimer;

    private static final float MAX_LOST_TIME = 2f;

    @Override
    public void enter(Enemy enemy) {

        lostPlayerTimer = 0f;
    }

    @Override
    public void update(Enemy enemy, float delta) {

        if (enemy.canSeePlayer()) {

            lostPlayerTimer = 0f;

            enemy.moveTowards(
                    enemy.getPlayerPosition(),
                    delta
            );

        } else {

            lostPlayerTimer += delta;

            if (lostPlayerTimer >= MAX_LOST_TIME) {

                enemy.getStateMachine().changeState(
                        enemy,
                        new SearchState(
                                enemy.getPosition().cpy()
                        )
                );
            }
        }
    }

    @Override
    public void exit(Enemy enemy) {
    }
}