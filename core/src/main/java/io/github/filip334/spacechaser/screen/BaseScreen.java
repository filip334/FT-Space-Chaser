package io.github.filip334.spacechaser.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.filip334.spacechaser.ui.Background;
import io.github.filip334.spacechaser.ui.Buttons;
import io.github.filip334.spacechaser.ui.Theme;
import io.github.filip334.spacechaser.ui.UiPanel;
import io.github.filip334.spacechaser.world.GameLayout;

/**
 * Zajednicka osnova za "meni-stil" ekrane (Main Menu, Multiplayer, Host/Join/
 * Client lobby, Settings, Game Over...) - fiksne piksel koordinate ekrana
 * (za razliku od GameScreen-a koji za samu igru koristi FitViewport), sopstveni
 * SpriteBatch/ShapeRenderer, providna gradient pozadina, standardan hover-test
 * i standardan prelaz na drugi ekran.
 * <p>
 * Game.setScreen() u LibGDX-u NIKAD sam ne zove dispose() na ekranu koji
 * napustas - to je razlog zasto je svaka tranzicija ranije curela teksture/
 * fontove/batch. navigateTo() to resava na jednom mestu.
 */
public abstract class BaseScreen implements Screen {

    protected final Game game;
    protected final SpriteBatch batch;
    protected final ShapeRenderer shapeRenderer;

    private Texture backgroundGradient;

    // ---------------- KONSTRUKTOR ----------------

    protected BaseScreen(Game game) {
        this.game = game;
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
    }

    // ---------------- SCREEN METHODS ----------------

    @Override
    public void show() {
        updateScreenProjection(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void resize(int width, int height) {
        updateScreenProjection(width, height);
    }

    // ---------------- RENDER HELPERS ----------------

    /**
     * SpriteBatch i ShapeRenderer ne menjaju automatski projekciju kada se
     * prozor promeni. Bez ovoga se ekran crta u koordinatama prethodne
     * velicine prozora, pa ostane prazan/crn deo dok se ne otvori drugi ekran.
     */
    protected void updateScreenProjection(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0f, 0f, width, height);
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
    }

    /** Resetuje viewport na pun prozor i cisti ekran na crno - prvi poziv u svakom render(). */
    protected void beginFrame() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(Color.BLACK);
    }

    /** Providna gradient pozadina preko celog ekrana - tekstura se pravi lenjo, jednom. */
    protected void drawBackground(float width, float height) {
        if (backgroundGradient == null) {
            backgroundGradient = Background.createGradientTexture();
        }
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(backgroundGradient, 0, 0, width, height);
        batch.end();
    }

    /** Tanka svetleca cyan linija - koristi se ispod naslova. */
    protected void drawGlowLine(float x, float y, float width, float thickness) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Theme.CYAN.r, Theme.CYAN.g, Theme.CYAN.b, 0.15f);
        shapeRenderer.rect(x, y - 4f, width, thickness + 8f);
        shapeRenderer.setColor(Theme.CYAN);
        shapeRenderer.rect(x, y, width, thickness);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Kutija sa imenom pilota i SP/MP rekordima (donji desni ugao) - deljena
     * izmedju Main Menu (klikabilna, editovanje imena) i Multiplayer (samo
     * za citanje) - pozivalac odlucuje da li je "highlighted" (npr. dok se
     * edituje ime) i sam racuna klik na bounds ako mu treba.
     */
    protected void drawPilotBox(Rectangle bounds, BitmapFont font, String pilotName, boolean highlighted,
                                 int spHighScore, int mpHighScore) {
        float boxWidth = 260f;
        float boxHeight = 102f;
        float x = Gdx.graphics.getWidth() - boxWidth - 30f;
        float y = 24f;
        bounds.set(x, y, boxWidth, boxHeight);

        UiPanel.draw(shapeRenderer, x, y, boxWidth, boxHeight, highlighted);

        batch.begin();
        font.setColor(highlighted ? Theme.WHITE : Theme.CYAN);
        font.draw(batch, "Pilot : " + pilotName, x + 16f, y + boxHeight - 16f);
        font.setColor(Theme.WHITE);
        font.draw(batch, "Best (SP) : " + spHighScore, x + 16f, y + boxHeight - 44f);
        font.draw(batch, "Best (MP) : " + mpHighScore, x + 16f, y + boxHeight - 72f);
        batch.end();
    }

    /**
     * Vertikalan stek dugmadi u donjem levom uglu - poslednja stavka u nizu
     * ("izlazna" akcija, npr. EXIT/BACK) je najniza, sa vecim razmakom od
     * ostalih iznad nje. Ista logika je ranije bila zasebno napisana i u
     * MainMenuScreen i MultiplayerScreen.
     */
    protected void drawVerticalMenu(BitmapFont font, String[] items, Rectangle[] bounds,
                                     float startX, float startY, float buttonWidth, float buttonHeight,
                                     float spacing, float lastItemExtraGap) {
        float y = startY;

        for (int i = items.length - 1; i >= 0; i--) {
            bounds[i].set(startX, y, buttonWidth, buttonHeight);
            boolean hovered = isMouseOver(bounds[i]);
            Buttons.draw(batch, shapeRenderer, font, bounds[i], items[i], hovered);

            float gap = (i == items.length - 1) ? lastItemExtraGap : spacing;
            y += buttonHeight + gap;
        }
    }

    // ---------------- UTIL ----------------

    /**
     * Razmera UI elemenata fiksne piksel velicine (dugmad, njihov font) u
     * odnosu na "projektovanu" velicinu prozora (GameLayout.WINDOW_WIDTH/
     * HEIGHT - ista rezolucija za koju je FitViewport mape podesen). Kad se
     * prozor smanji/poveca (min. velicina je ogranicena, ali resizable je),
     * dugmad bi inace ostala ista u pikselima i izgledala nesrazmerno -
     * pomnozi njihove dimenzije/font ovim faktorom da se smanjuju/povecavaju
     * proporcionalno sa prozorom.
     */
    protected float computeUiScale(float width, float height) {
        return Math.min(width / GameLayout.WINDOW_WIDTH, height / GameLayout.WINDOW_HEIGHT);
    }

    protected boolean isMouseOver(Rectangle bounds) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        return bounds.contains(mouseX, mouseY);
    }

    // ---------------- NAVIGATION ----------------

    /** Prelaz na drugi ekran - uvek prvo oslobodi resurse OVOG ekrana (Game.setScreen to sam nikad ne radi). */
    protected void navigateTo(Screen next) {
        dispose();
        game.setScreen(next);
    }

    // ---------------- SCREEN METHODS ----------------

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    // ---------------- DISPOSE ----------------

    /** Podklase sa sopstvenim fontovima/teksturama MORAJU override-ovati i pozvati super.dispose(). */
    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (backgroundGradient != null) {
            backgroundGradient.dispose();
        }
    }
}
