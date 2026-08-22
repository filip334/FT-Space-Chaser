package io.github.filip334.spacechaser.enemy;

import io.github.filip334.spacechaser.entity.Enemy;

public class StateMachine {

    private EnemyState currentState;

    public void changeState(Enemy enemy, EnemyState newState) {

        if (newState == null) {
            return;
        }

        if (currentState != null) {
            currentState.exit(enemy);
        }

        currentState = newState;
        currentState.enter(enemy);
    }

    public void update(Enemy enemy, float delta) {

        if (currentState != null) {
            currentState.update(enemy, delta);
        }
    }

    public EnemyState getCurrentState() {
        return currentState;
    }
}