package mario;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/** 道具：蘑菇（變大）與火焰花（火球能力）。從問號磚中升起。 */
public class Item {
    public enum Type { MUSHROOM, FLOWER }

    public static final int W = 26;
    public static final int H = 26;

    public final Type type;
    public double x, y, vx, vy;
    public boolean remove;
    private double rise; // 從磚塊中升起的剩餘像素

    /** finalY = 升起後停在磚塊頂端的 y 座標。 */
    public Item(Type type, double x, double finalY) {
        this.type = type;
        this.x = x;
        this.y = finalY + H;
        this.rise = H;
        if (type == Type.MUSHROOM) vx = 1.6;
    }

    public boolean emerging() {
        return rise > 0;
    }

    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x, y, W, H);
    }

    public void update(Level lv, double dt) {
        if (rise > 0) {
            y -= 0.8;
            rise -= 0.8;
            return;
        }
        if (type == Type.FLOWER) return; // 火焰花原地不動

        int t = Level.TILE;
        x += vx;
        if (vx > 0) {
            int col = (int) ((x + W) / t);
            if (lv.solidInColumn(col, y, H)) {
                x = col * t - W - 0.001;
                vx = -vx;
            }
        } else if (vx < 0) {
            int col = (int) (x / t);
            if (lv.solidInColumn(col, y, H)) {
                x = (col + 1) * t + 0.001;
                vx = -vx;
            }
        }

        vy += Player.GRAVITY;
        if (vy > Player.MAX_FALL) vy = Player.MAX_FALL;
        y += vy;
        if (vy > 0) {
            int row = (int) ((y + H) / t);
            if (lv.solidInRow(row, x, W)) {
                y = row * t - H - 0.001;
                vy = 0;
            }
        }

        if (y > Level.ROWS * Level.TILE + 100) remove = true;
    }

    public void draw(Graphics2D g2, double camX) {
        int px = (int) (x - camX);
        int py = (int) y;

        if (type == Type.MUSHROOM) {
            // 蘑菇頭
            g2.setColor(new Color(225, 60, 50));
            g2.fillArc(px, py, W, 28, 0, 180);
            g2.setColor(Color.WHITE);
            g2.fillOval(px + 4, py + 3, 6, 6);
            g2.fillOval(px + 16, py + 3, 6, 6);
            g2.fillOval(px + 10, py + 9, 6, 5);
            // 臉
            g2.setColor(new Color(250, 220, 170));
            g2.fillRect(px + 5, py + 14, 16, 12);
            g2.setColor(Color.BLACK);
            g2.fillRect(px + 8, py + 17, 3, 5);
            g2.fillRect(px + 15, py + 17, 3, 5);
        } else {
            // 火焰花：莖
            g2.setColor(new Color(50, 160, 50));
            g2.fillRect(px + 11, py + 14, 4, 12);
            g2.fillRect(px + 4, py + 20, 8, 3);
            g2.fillRect(px + 14, py + 20, 8, 3);
            // 花瓣
            g2.setColor(new Color(240, 120, 30));
            g2.fillOval(px + 3, py, 20, 16);
            g2.setColor(new Color(230, 60, 40));
            g2.fillOval(px + 6, py + 2, 14, 12);
            g2.setColor(Color.WHITE);
            g2.fillOval(px + 9, py + 4, 8, 8);
        }
    }
}
