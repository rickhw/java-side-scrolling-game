package mario;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/** 火力瑪莉發射的火球：沿地面彈跳，撞牆消失，可消滅敵人。 */
public class Fireball {
    public static final int S = 12;

    public double x, y, vx, vy;
    public boolean remove;
    private double life = 3.0;

    public Fireball(double x, double y, int dir) {
        this.x = x;
        this.y = y;
        this.vx = 7.5 * dir;
    }

    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x, y, S, S);
    }

    public void update(Level lv, double dt) {
        life -= dt;
        if (life <= 0) {
            remove = true;
            return;
        }

        int t = Level.TILE;
        x += vx;
        if (vx > 0) {
            int col = (int) ((x + S) / t);
            if (lv.solidInColumn(col, y, S)) {
                remove = true;
                return;
            }
        } else {
            int col = (int) (x / t);
            if (lv.solidInColumn(col, y, S)) {
                remove = true;
                return;
            }
        }

        vy += 0.45;
        if (vy > 10) vy = 10;
        y += vy;
        if (vy > 0) {
            int row = (int) ((y + S) / t);
            if (lv.solidInRow(row, x, S)) {
                y = row * t - S - 0.001;
                vy = -5.2; // 彈跳
            }
        } else if (vy < 0) {
            int row = (int) (y / t);
            if (lv.solidInRow(row, x, S)) {
                remove = true;
                return;
            }
        }

        if (y > Level.ROWS * Level.TILE + 50) remove = true;
    }

    public void draw(Graphics2D g2, double camX, double animTime) {
        int px = (int) (x - camX);
        int py = (int) y;
        g2.setColor(new Color(240, 110, 20));
        g2.fillOval(px, py, S, S);
        g2.setColor(new Color(255, 220, 80));
        // 旋轉的內焰
        double a = animTime * 16;
        int ox = (int) (Math.cos(a) * 2);
        int oy = (int) (Math.sin(a) * 2);
        g2.fillOval(px + 3 + ox, py + 3 + oy, 6, 6);
    }
}
