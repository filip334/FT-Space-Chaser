/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.filip334.spacechaser.enemy;

import io.github.filip334.spacechaser.entity.Enemy;

/**
 *
 * @author Todorovic
 */
public interface EnemyState {
    void enter(Enemy enemy);

    void update(Enemy enemy, float delta);

    void exit(Enemy enemy);
}
