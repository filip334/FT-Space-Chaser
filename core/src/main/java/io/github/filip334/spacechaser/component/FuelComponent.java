package io.github.filip334.spacechaser.component;

public class FuelComponent {
    //
    private float maxFuel;
    private float fuel;

    // ---------------- CONSTRUCTOR ----------------
    
    public FuelComponent(float maxFuel) {
        this.maxFuel = maxFuel;
        this.fuel = maxFuel;
    }
    
    
    public void consume(float amount){
        fuel -= amount;

        if (fuel < 0) {
            fuel = 0;
        }
    }
    
    public void refill(float amount){
        fuel += amount;
        
        if(fuel > maxFuel)
            fuel = maxFuel;
    }

    // GETERI / SETERI
    
    public float getMaxFuel() {
        return maxFuel;
    }

    public float getFuel() {
        return fuel;
    }
    
    public boolean hasFuel(){
        return fuel>0;
    }
}
