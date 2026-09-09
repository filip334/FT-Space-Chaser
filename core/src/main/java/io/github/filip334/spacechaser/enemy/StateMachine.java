package io.github.filip334.spacechaser.enemy;

import io.github.filip334.spacechaser.entity.Enemy;

public class StateMachine {

    private EnemyState currentState;

    // ---------------- STATE TRANSITIONS ----------------

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

    // ---------------- UPDATE ----------------

    public void update(Enemy enemy, float delta) {

        if (currentState != null) {
            currentState.update(enemy, delta);
        }
    }
}