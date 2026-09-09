package io.github.filip334.spacechaser.enemy;

import io.github.filip334.spacechaser.entity.Enemy;

public interface EnemyState {
    void enter(Enemy enemy);

    void update(Enemy enemy, float delta);

    void exit(Enemy enemy);
}
