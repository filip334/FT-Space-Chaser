package io.github.filip334.spacechaser.server;

import java.util.ArrayList;
import java.util.List;

public class GameStateSnapshot extends NetworkMessage {
    private static final long serialVersionUID = 1L;

    public List<EntityState> players = new ArrayList<>();
    public List<EntityState> enemies = new ArrayList<>();
    public List<EntityState> bullets = new ArrayList<>();
    public List<EntityState> walls = new ArrayList<>();
}