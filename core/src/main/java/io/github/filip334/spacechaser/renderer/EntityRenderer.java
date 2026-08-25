package io.github.filip334.spacechaser.renderer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.filip334.spacechaser.entity.*;

public class EntityRenderer {
    
    private Texture playerTexture;
    private Texture enemyTexture;
    private Texture bulletTexture;

    public EntityRenderer(Texture playerTexture, Texture enemyTexture, Texture bulletTexture) {
        this.playerTexture = playerTexture;
        this.enemyTexture = enemyTexture;
        this.bulletTexture = bulletTexture;
    }
    
    public void render(SpriteBatch batch, Entity entity){
        if (entity instanceof Player) {
            Player player = (Player)entity;
            renderPlayer(batch, player);
        }

        if (entity instanceof Enemy) {
            Enemy enemy = (Enemy)entity;
            renderEnemy(batch, enemy);
        }

        if (entity instanceof Bullet) {
            Bullet bullet = (Bullet)entity;
            renderBullet(batch, bullet);
        }
    }
    
    
    private void renderPlayer(SpriteBatch batch,Player player){
        batch.draw(
            playerTexture, 
            player.getX() - player.getWidth() / 2f,
            player.getY() - player.getHeight() / 2f,
            player.getWidth() / 2f,
            player.getHeight() / 2f,
            player.getWidth(),
            player.getHeight(),
            1f,
            1f,
            player.getRotation(),
            0,
            0,
            playerTexture.getWidth(),
            playerTexture.getHeight(),
            false,
            false
        );
    }
    
    private void renderEnemy(SpriteBatch batch,Enemy enemy){
        batch.draw(
            enemyTexture,
            enemy.getX() - enemy.getWidth() / 2f,
            enemy.getY() - enemy.getHeight() / 2f,
            enemy.getWidth() / 2f,
            enemy.getHeight() / 2f,
            enemy.getWidth(),
            enemy.getHeight(),
            1f,
            1f,
            enemy.getRotation(),
            0,
            0,
            enemyTexture.getWidth(),
            enemyTexture.getHeight(),
            false,
            false
        );
    }
    
    private void renderBullet(SpriteBatch batch,Bullet bullet){
        batch.draw(
            bulletTexture,
            bullet.getX() - bullet.getWidth() / 2f,
            bullet.getY() - bullet.getHeight() / 2f,
            bullet.getWidth() / 2f,
            bullet.getHeight() / 2f,
            bullet.getWidth(),
            bullet.getHeight(),
            1f,
            1f,
            bullet.getRotation(),
            0,
            0,
            bulletTexture.getWidth(),
            bulletTexture.getHeight(),
            false,
            false
        );
    }
    
    public void dispose() {
        playerTexture.dispose();
        enemyTexture.dispose();
        bulletTexture.dispose();
    }
    
    
}
