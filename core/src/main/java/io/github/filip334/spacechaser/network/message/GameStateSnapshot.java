package io.github.filip334.spacechaser.network.message;

import java.util.ArrayList;
import java.util.List;

public class GameStateSnapshot extends NetworkMessage {
    private static final long serialVersionUID = 1L;

    public List<EntityState> players = new ArrayList<>();
    public List<EntityState> enemies = new ArrayList<>();
    public List<EntityState> bullets = new ArrayList<>();
    public List<EntityState> walls = new ArrayList<>();
    public List<EntityState> coins = new ArrayList<>();

    public int score;
    public float gameTime;
    public boolean matchOver;
    public int wave;
    public float waveCountdown;

    public boolean wallsIncluded;
    public boolean coinsChanged;
}
