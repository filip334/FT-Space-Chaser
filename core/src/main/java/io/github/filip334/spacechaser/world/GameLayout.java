package io.github.filip334.spacechaser.world;

/** Fiksni raspored desktop igre: centralna mapa i dva jednaka HUD panela. */
public final class GameLayout {
    public static final int MAP_VIEWPORT_SIZE = 984;
    public static final int SIDE_PANEL_WIDTH = 276;
    public static final int WINDOW_WIDTH = MAP_VIEWPORT_SIZE + SIDE_PANEL_WIDTH * 2;
    public static final int WINDOW_HEIGHT = MAP_VIEWPORT_SIZE;
    public static final float WORLD_OFFSET_X = SIDE_PANEL_WIDTH;

    private GameLayout() {
    }
}
