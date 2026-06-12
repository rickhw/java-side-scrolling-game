package mario;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public class Player {
    public static final int W = 26;
    public static final int SMALL_H = 30;
    public static final int BIG_H = 44;
    public static final double GRAVITY = 0.5;
    // 最大上升高度約 150px（4.7 格），4 格高的水管（128px）才跳得過
    public static final double JUMP_VELOCITY = -12.5;
    public static final double MAX_FALL = 12;

    public double x, y, vx, vy;
    public int h = SMALL_H;
    public int power; // 0=小瑪莉 1=大瑪莉 2=火力瑪莉
    public boolean onGround;
    public int facing = 1;
    public double invincible; // 受傷/重生後的短暫無敵秒數
    private double walkAnim;

    /** groundY = 出生點腳底的 y 座標。 */
    public void reset(double px, double groundY) {
        applyHeight();
        x = px;
        y = groundY - h;
        vx = 0;
        vy = 0;
        onGround = false;
        facing = 1;
        walkAnim = 0;
    }

    public void setPower(int p) {
        double bottom = y + h;
        power = p;
        applyHeight();
        y = bottom - h;
    }

    private void applyHeight() {
        h = power > 0 ? BIG_H : SMALL_H;
    }

    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x, y, W, h);
    }

    public void update(Level lv, boolean left, boolean right, boolean run, double dt, GamePanel game) {
        double accel = onGround ? 0.6 : 0.35;
        double maxSpeed = run ? 6.0 : 3.8;

        if (left) {
            vx -= accel;
            facing = -1;
        }
        if (right) {
            vx += accel;
            facing = 1;
        }
        if (!left && !right) {
            vx *= onGround ? 0.8 : 0.95;
            if (Math.abs(vx) < 0.1) vx = 0;
        }
        vx = Math.max(-maxSpeed, Math.min(maxSpeed, vx));

        moveAndCollide(lv, game);

        if (invincible > 0) invincible -= dt;
        if (Math.abs(vx) > 0.3 && onGround) walkAnim += Math.abs(vx) * dt * 10;
    }

    public void jump() {
        vy = JUMP_VELOCITY;
        onGround = false;
    }

    public void cutJump() {
        if (vy < -4) vy = -4; // 提前放開跳躍鍵 → 跳得較低
    }

    private void moveAndCollide(Level lv, GamePanel game) {
        int t = Level.TILE;

        // 水平移動與碰撞
        x += vx;
        if (x < 0) {
            x = 0;
            vx = 0;
        }
        if (vx > 0) {
            int col = (int) ((x + W) / t);
            if (lv.solidInColumn(col, y, h)) {
                x = col * t - W - 0.001;
                vx = 0;
            }
        } else if (vx < 0) {
            int col = (int) (x / t);
            if (lv.solidInColumn(col, y, h)) {
                x = (col + 1) * t + 0.001;
                vx = 0;
            }
        }

        // 垂直移動與碰撞
        vy += GRAVITY;
        if (vy > MAX_FALL) vy = MAX_FALL;
        y += vy;
        onGround = false;
        if (vy > 0) {
            int row = (int) ((y + h) / t);
            if (lv.solidInRow(row, x, W)) {
                y = row * t - h - 0.001;
                vy = 0;
                onGround = true;
            }
        } else if (vy < 0) {
            int row = (int) (y / t);
            if (lv.solidInRow(row, x, W)) {
                y = (row + 1) * t + 0.001;
                vy = 0;
                bumpHeadTiles(lv, row, game);
            }
        }
    }

    private void bumpHeadTiles(Level lv, int row, GamePanel game) {
        int t = Level.TILE;
        // 優先撞頭部正上方（玩家中心）的磚塊
        int center = (int) ((x + W / 2.0) / t);
        if (lv.isSolid(center, row)) {
            game.bumpBlock(center, row);
            return;
        }
        int c1 = (int) (x / t);
        int c2 = (int) ((x + W - 1) / t);
        for (int c = c1; c <= c2; c++) {
            if (lv.isSolid(c, row)) {
                game.bumpBlock(c, row);
                return;
            }
        }
    }

    public void draw(Graphics2D g2, double camX) {
        // 無敵期間閃爍
        if (invincible > 0 && ((int) (invincible * 10) % 2 == 0)) return;

        int px = (int) (x - camX);
        int py = (int) y;

        // 火力瑪莉：白帽白衣紅吊帶褲；一般：紅衣藍吊帶褲
        Color primary = power == 2 ? new Color(250, 245, 235) : new Color(220, 50, 40);
        Color overalls = power == 2 ? new Color(220, 50, 40) : new Color(50, 80, 200);
        Color skin = new Color(250, 200, 160);
        Color brown = new Color(110, 60, 20);
        Color button = new Color(250, 220, 80);

        boolean flip = facing < 0;
        boolean stepFrame = ((int) walkAnim) % 2 == 0 && Math.abs(vx) > 0.3 && onGround;

        if (power == 0) {
            // ---- 小瑪莉 (26x30) ----
            g2.setColor(primary);
            g2.fillRect(px + 3, py, 20, 5);
            g2.fillRect(flip ? px : px + 6, py + 4, 20, 3);
            g2.setColor(skin);
            g2.fillRect(px + 4, py + 7, 18, 8);
            g2.setColor(Color.BLACK);
            g2.fillRect(flip ? px + 7 : px + 16, py + 8, 3, 4);
            g2.setColor(brown);
            g2.fillRect(flip ? px + 4 : px + 14, py + 12, 8, 3);
            g2.setColor(primary);
            g2.fillRect(px + 3, py + 15, 20, 6);
            g2.setColor(overalls);
            g2.fillRect(px + 5, py + 19, 16, 8);
            g2.fillRect(px + 7, py + 15, 3, 5);
            g2.fillRect(px + 16, py + 15, 3, 5);
            g2.setColor(button);
            g2.fillOval(px + 8, py + 16, 3, 3);
            g2.fillOval(px + 15, py + 16, 3, 3);
            g2.setColor(brown);
            drawShoes(g2, px, py + 26, stepFrame);
        } else {
            // ---- 大瑪莉 / 火力瑪莉 (26x44) ----
            g2.setColor(primary);
            g2.fillRect(px + 3, py, 20, 6);
            g2.fillRect(flip ? px : px + 6, py + 5, 20, 3);
            g2.setColor(skin);
            g2.fillRect(px + 4, py + 8, 18, 10);
            g2.setColor(Color.BLACK);
            g2.fillRect(flip ? px + 7 : px + 16, py + 10, 3, 5);
            g2.setColor(brown);
            g2.fillRect(flip ? px + 4 : px + 14, py + 15, 8, 3);
            // 上衣
            g2.setColor(primary);
            g2.fillRect(px + 2, py + 18, 22, 9);
            // 吊帶褲
            g2.setColor(overalls);
            g2.fillRect(px + 4, py + 25, 18, 13);
            g2.fillRect(px + 7, py + 18, 3, 8);
            g2.fillRect(px + 16, py + 18, 3, 8);
            g2.setColor(button);
            g2.fillOval(px + 8, py + 20, 4, 4);
            g2.fillOval(px + 14, py + 20, 4, 4);
            // 手套
            g2.setColor(skin);
            g2.fillOval(flip ? px - 1 : px + 21, py + 24, 6, 6);
            g2.setColor(brown);
            drawShoes(g2, px, py + 38, stepFrame);
        }
    }

    private void drawShoes(Graphics2D g2, int px, int sy, boolean stepFrame) {
        if (!onGround) {
            g2.fillRect(px + 2, sy, 9, 5);
            g2.fillRect(px + 15, sy, 9, 5);
        } else if (stepFrame) {
            g2.fillRect(px, sy, 10, 5);
            g2.fillRect(px + 16, sy, 8, 5);
        } else {
            g2.fillRect(px + 2, sy, 9, 5);
            g2.fillRect(px + 15, sy, 9, 5);
        }
    }
}
