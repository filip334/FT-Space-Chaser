/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.filip334.spacechaser.input;

/**
 *
 * @author Todorovic
 */
public class PlayerInput {
    public boolean left;
    public boolean right;
    public boolean forward;
    public boolean backward;
    public boolean boost;
    public boolean shoot;

    public void reset() {
        left = right = forward = backward = boost = shoot = false;
    }
}
