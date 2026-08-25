package io.github.filip334.spacechaser.component;

public class HealthComponent {
    private float maxHealth;
    private float health;

    // ---------------- CONSTRUCTOR ----------------
    
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

    public void setHealth(float health) {
        this.health = health;
    }
    
    
}
