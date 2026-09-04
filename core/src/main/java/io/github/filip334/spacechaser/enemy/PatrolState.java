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
            System.out.println("BUG BUG BUG");
            return;
        }

        Vector2 target = patrolPoints.get(currentPoint);
        // Ne cekaj sledeci frejm kada meta bude dostignuta: odmah idi ka narednoj.
        if (enemy.getPosition().dst(target) < 20f) {
            currentPoint++;
            if (currentPoint >= patrolPoints.size) {
                enemy.getStateMachine().changeState(enemy, enemy.createPatrolState());
                return;
            }
            target = patrolPoints.get(currentPoint);
        }

        // FIX: moveTowards() koristi samo lokalni steering (pravo/levo/desno),
        // koji moze geometrijski da se zaglavi u konkavnim uglovima ili
        // tesnim prolazima - tada bi svaki frejm birao novu nasumicnu
        // patrol tacku (kod ispod), sto je izgledalo kao "freeze + trzanje
        // ugla". moveToTarget() prvo probne isti direktan pristup, a ako
        // ne uspe, koristi A* pathfinding da zaobidje prepreku.
        enemy.moveToTarget(target, delta);
        if (enemy.wasMovementBlocked()) {
            // Nova patrola bira tacke sa cistim putem iz trenutne pozicije.
            enemy.getStateMachine().changeState(enemy, enemy.createPatrolState());
            return;
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