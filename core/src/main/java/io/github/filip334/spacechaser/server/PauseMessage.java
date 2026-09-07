package io.github.filip334.spacechaser.server;

/** Klijent -> server: zahtev za pauzu ili nastavak igre. */
public class PauseMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;

    public boolean pause;
}
