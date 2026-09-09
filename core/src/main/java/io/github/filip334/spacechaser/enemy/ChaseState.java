package io.github.filip334.spacechaser.enemy;

import com.badlogic.gdx.math.Vector2;
import io.github.filip334.spacechaser.entity.Enemy;

public class ChaseState implements EnemyState {

    private Vector2 lastKnownPlayerPosition;

    // ---------------- ENTER ----------------

    @Override
    public void enter(Enemy enemy) {
        if (enemy.getPlayer() != null) {
            lastKnownPlayerPosition = enemy.getPlayerPosition().cpy();
        }
    }

    // ---------------- UPDATE ----------------

    @Override
    public void update(Enemy enemy, float delta) {
        if (enemy.canSeePlayer()) {
            lastKnownPlayerPosition = enemy.getPlayerPosition().cpy();
            enemy.moveToPlayer(delta);
        } else {
            Vector2 searchTarget = lastKnownPlayerPosition != null
                    ? lastKnownPlayerPosition
                    : enemy.getPosition();

            enemy.getStateMachine().changeState(
                    enemy,
                    new SearchState(searchTarget)
            );
        }
    }

    // ---------------- EXIT ----------------

    @Override
    public void exit(Enemy enemy) {
    }
}