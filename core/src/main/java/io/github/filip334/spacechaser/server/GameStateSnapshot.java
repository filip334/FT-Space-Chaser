package io.github.filip334.spacechaser.server;

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
    // <= 0 znaci da odbrojavanje trenutno nije aktivno
    public float waveCountdown;

    // Zidovi/novcici se NE salju svaki tick (zidovi su staticni, novcici se
    // samo skupljaju) - ova dva polja govore klijentu da li su liste u ovom
    // snapshotu stvarno relevantne ili treba da ignorise (prazne) liste.
    public boolean wallsIncluded;
    public boolean coinsChanged;
}
