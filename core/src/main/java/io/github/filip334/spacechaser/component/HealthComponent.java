/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.filip334.spacechaser.component;

/**
 *
 * @author Todorovic
 */
public class HealthComponent {
    private float maxHealth;
    private float health;

    // KONSTRUKTOR
    
    public HealthComponent(float maxHealth) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }
    
    
    //
    public void damage(float amount){
        health -= amount;
        
        if(health<0)
            health = 0;
    }
    
    //
    public void heal(int amount){
        health = Math.min(maxHealth, health + amount);
    }

    
    //
    public boolean isDead(){
        return health <= 0;
    }

    //
    public float getHealth(){
        return health;
    }

    //
    public float getMaxHealth(){
        return maxHealth;
    }
}
