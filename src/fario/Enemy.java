package fario;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * 敵人：
 *  - GRUB 步行蟲：行走，可踩扁
 *  - BEETLE 甲蟲：踩一下縮進殼，再碰殼會踢出去滑行消滅其他敵人
 *  - SPIKER 尖刺蟲：不能踩（會受傷），只能用火球或殼消滅
 */
public class Enemy {
    public enum Type { GRUB, BEETLE, SPIKER, BOSS }
    public enum Mode { WALK, SHELL, SHELL_MOVING, SQUASHED, FLIPPED }

    public static final int W = 28;
    private static final int SHELL_H = 24;
    private static final int BOSS_HP = 5;

    public final Type type;
    public Mode mode = Mode.WALK;
    public double x, y, vx, vy;
    public int w;            // 實際寬度（一般敵人 = W，Boss 較大）
    public int h;
    public int hp = 1;       // Boss 需要多次攻擊才能擊敗
    public double hitInvuln; // Boss 被擊中後短暫無敵，避免一次扣多滴血
    public boolean active;   // 進入畫面後才開始行動
    public boolean remove;
    public double kickGrace; // 剛踢出的瞬間不會傷到踢的人
    private double squashTimer;
    private double walkAnim;

    public Enemy(Type type, double x, double groundY) {
        this.type = type;
        this.w = type == Type.BOSS ? 60 : W;
        this.h = switch (type) {
            case BEETLE -> 34;
            case BOSS -> 60;
            default -> 26;
        };
        this.hp = type == Type.BOSS ? BOSS_HP : 1;
        this.x = x;
        this.y = groundY - h;
        this.vx = switch (type) {
            case SPIKER -> -1.0;
            case BOSS -> -1.6;
            default -> -1.3;
        };
    }

    public boolean isBoss() {
        return type == Type.BOSS;
    }

    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x, y, w, h);
    }

    public boolean harmless() {
        return mode == Mode.SQUASHED || mode == Mode.FLIPPED;
    }

    /** Boss 被踩或被火球擊中。回傳是否因此被擊敗。 */
    public boolean hurtBoss(int dir) {
        if (hitInvuln > 0) return false;
        hp--;
        hitInvuln = 0.6;
        if (hp <= 0) {
            flip(dir);
            return true;
        }
        return false;
    }

    public void squash() {
        mode = Mode.SQUASHED;
        squashTimer = 0.4;
        vx = 0;
        vy = 0;
    }

    public void toShell() {
        mode = Mode.SHELL;
        vx = 0;
        y += h - SHELL_H;
        h = SHELL_H;
    }

    public void kick(int dir) {
        mode = Mode.SHELL_MOVING;
        vx = 8 * dir;
        kickGrace = 0.25;
    }

    public void stopShell() {
        mode = Mode.SHELL;
        vx = 0;
    }

    /** 被火球或殼擊中：翻面飛出畫面。 */
    public void flip(int dir) {
        mode = Mode.FLIPPED;
        vx = 1.5 * dir;
        vy = -6;
    }

    public void update(Level lv, double dt) {
        if (kickGrace > 0) kickGrace -= dt;
        if (hitInvuln > 0) hitInvuln -= dt;
        walkAnim += dt * 8;

        switch (mode) {
            case SQUASHED -> {
                squashTimer -= dt;
                if (squashTimer <= 0) remove = true;
            }
            case FLIPPED -> {
                // 無碰撞，直接掉出畫面
                x += vx;
                vy += Player.GRAVITY;
                y += vy;
                if (y > Level.ROWS * Level.TILE + 100) remove = true;
            }
            default -> moveAndCollide(lv);
        }
    }

    private void moveAndCollide(Level lv) {
        int t = Level.TILE;

        x += vx;
        if (vx > 0) {
            int col = (int) ((x + w) / t);
            if (lv.solidInColumn(col, y, h)) {
                x = col * t - w - 0.001;
                vx = -vx;
            }
        } else if (vx < 0) {
            int col = (int) (x / t);
            if (lv.solidInColumn(col, y, h)) {
                x = (col + 1) * t + 0.001;
                vx = -vx;
            }
        }

        vy += Player.GRAVITY;
        if (vy > Player.MAX_FALL) vy = Player.MAX_FALL;
        y += vy;
        if (vy > 0) {
            int row = (int) ((y + h) / t);
            if (lv.solidInRow(row, x, w)) {
                y = row * t - h - 0.001;
                vy = 0;
            }
        }

        if (y > Level.ROWS * Level.TILE + 100) remove = true; // 掉進坑洞
    }

    public void draw(Graphics2D g2, double camX) {
        int px = (int) (x - camX);
        int py = (int) y;

        if (mode == Mode.FLIPPED) {
            AffineTransform old = g2.getTransform();
            g2.translate(px + w / 2.0, py + h / 2.0);
            g2.scale(1, -1);
            g2.translate(-(px + w / 2.0), -(py + h / 2.0));
            drawBody(g2, px, py);
            g2.setTransform(old);
            return;
        }
        drawBody(g2, px, py);
    }

    private void drawBody(Graphics2D g2, int px, int py) {
        switch (type) {
            case GRUB -> drawGrub(g2, px, py);
            case BEETLE -> drawBeetle(g2, px, py);
            case SPIKER -> drawSpiker(g2, px, py);
            case BOSS -> drawBoss(g2, px, py);
        }
    }

    private void drawBoss(Graphics2D g2, int px, int py) {
        // 被擊中後閃爍
        if (hitInvuln > 0 && ((int) (hitInvuln * 12)) % 2 == 0) return;

        boolean step = ((int) walkAnim) % 2 == 0;
        Color shell = new Color(70, 150, 60);
        Color shellDark = new Color(40, 100, 35);
        Color belly = new Color(235, 225, 180);
        Color skin = new Color(225, 200, 110);
        Color spike = new Color(245, 240, 225);

        // 腳
        g2.setColor(skin);
        g2.fillOval(px + (step ? 2 : 6), py + h - 13, 20, 15);
        g2.fillOval(px + w - 26 + (step ? -2 : 2), py + h - 13, 20, 15);
        // 殼 / 身體
        g2.setColor(shell);
        g2.fillOval(px + 4, py + 14, w - 8, h - 18);
        g2.setColor(belly);
        g2.fillOval(px + 15, py + 24, w - 30, h - 30);
        g2.setColor(shellDark);
        g2.drawOval(px + 4, py + 14, w - 9, h - 19);
        // 殼上尖刺
        g2.setColor(spike);
        for (int i = 0; i < 4; i++) {
            int sx = px + 12 + i * 10;
            g2.fillPolygon(new int[]{sx, sx + 5, sx + 10}, new int[]{py + 18, py + 8, py + 18}, 3);
        }
        // 頭（朝移動方向）
        int hx = vx < 0 ? px + 2 : px + w - 30;
        g2.setColor(skin);
        g2.fillOval(hx, py, 28, 22);
        // 角
        g2.setColor(spike);
        g2.fillPolygon(new int[]{hx + 4, hx + 8, hx + 12}, new int[]{py + 2, py - 8, py + 2}, 3);
        g2.fillPolygon(new int[]{hx + 16, hx + 20, hx + 24}, new int[]{py + 2, py - 8, py + 2}, 3);
        // 眼睛 + 怒眉
        g2.setColor(Color.WHITE);
        g2.fillOval(hx + 6, py + 8, 7, 8);
        g2.fillOval(hx + 16, py + 8, 7, 8);
        g2.setColor(Color.BLACK);
        g2.fillOval(hx + 8, py + 11, 3, 4);
        g2.fillOval(hx + 18, py + 11, 3, 4);
        g2.setColor(new Color(180, 40, 30));
        g2.drawLine(hx + 5, py + 6, hx + 13, py + 9);
        g2.drawLine(hx + 24, py + 6, hx + 16, py + 9);
    }

    private void drawGrub(Graphics2D g2, int px, int py) {
        Color body = new Color(160, 90, 40);
        Color feet = new Color(90, 50, 20);

        if (mode == Mode.SQUASHED) {
            g2.setColor(body);
            g2.fillRoundRect(px, py + h - 10, W, 10, 6, 6);
            g2.setColor(feet);
            g2.fillRect(px - 2, py + h - 4, 8, 4);
            g2.fillRect(px + W - 6, py + h - 4, 8, 4);
            return;
        }

        boolean step = ((int) walkAnim) % 2 == 0;
        g2.setColor(body);
        g2.fillOval(px, py, W, h + 4);
        g2.setColor(feet);
        if (step) {
            g2.fillOval(px - 2, py + h - 6, 12, 8);
            g2.fillOval(px + W - 12, py + h - 4, 12, 8);
        } else {
            g2.fillOval(px + 2, py + h - 4, 12, 8);
            g2.fillOval(px + W - 14, py + h - 6, 12, 8);
        }
        g2.setColor(Color.WHITE);
        g2.fillOval(px + 5, py + 8, 7, 9);
        g2.fillOval(px + W - 12, py + 8, 7, 9);
        g2.setColor(Color.BLACK);
        g2.fillOval(px + 8, py + 11, 3, 4);
        g2.fillOval(px + W - 11, py + 11, 3, 4);
        g2.drawLine(px + 4, py + 6, px + 11, py + 9);
        g2.drawLine(px + W - 4, py + 6, px + W - 11, py + 9);
    }

    private void drawBeetle(Graphics2D g2, int px, int py) {
        Color shell = new Color(60, 160, 50);
        Color shellDark = new Color(35, 110, 30);
        Color skin = new Color(245, 215, 130);

        if (mode == Mode.SHELL || mode == Mode.SHELL_MOVING) {
            g2.setColor(shell);
            g2.fillOval(px, py, W, h);
            g2.setColor(new Color(235, 235, 215));
            g2.fillRect(px, py + h - 7, W, 7);
            g2.setColor(shellDark);
            g2.drawOval(px, py, W - 1, h - 1);
            // 殼的紋路（滑行時旋轉感）
            int offset = mode == Mode.SHELL_MOVING ? ((int) (walkAnim * 3)) % 8 : 0;
            g2.drawLine(px + 6 + offset % 6, py + 4, px + 4 + offset % 6, py + h - 8);
            g2.drawLine(px + 14, py + 3, px + 14, py + h - 7);
            g2.drawLine(px + 22 - offset % 6, py + 4, px + 24 - offset % 6, py + h - 8);
            return;
        }

        boolean step = ((int) walkAnim) % 2 == 0;
        int headX = vx < 0 ? px - 2 : px + W - 12;
        // 頭
        g2.setColor(skin);
        g2.fillOval(headX, py, 14, 13);
        g2.setColor(Color.BLACK);
        g2.fillOval(vx < 0 ? headX + 2 : headX + 8, py + 4, 3, 4);
        // 殼
        g2.setColor(shell);
        g2.fillOval(px + 1, py + 8, W - 2, h - 12);
        g2.setColor(shellDark);
        g2.drawOval(px + 1, py + 8, W - 3, h - 13);
        g2.drawLine(px + 9, py + 11, px + 8, py + h - 8);
        g2.drawLine(px + 19, py + 11, px + 20, py + h - 8);
        // 腳
        g2.setColor(skin);
        if (step) {
            g2.fillOval(px + 2, py + h - 6, 10, 7);
            g2.fillOval(px + W - 12, py + h - 4, 10, 6);
        } else {
            g2.fillOval(px + 4, py + h - 4, 10, 6);
            g2.fillOval(px + W - 14, py + h - 6, 10, 7);
        }
    }

    private void drawSpiker(Graphics2D g2, int px, int py) {
        Color body = new Color(210, 70, 40);
        Color spike = new Color(250, 240, 220);
        Color feet = new Color(140, 45, 25);

        // 刺
        g2.setColor(spike);
        for (int i = 0; i < 4; i++) {
            int sx = px + 3 + i * 7;
            g2.fillPolygon(new int[]{sx, sx + 3, sx + 6}, new int[]{py + 8, py - 4, py + 8}, 3);
        }
        // 身體
        g2.setColor(body);
        g2.fillOval(px, py + 4, W, h);
        // 腳
        boolean step = ((int) walkAnim) % 2 == 0;
        g2.setColor(feet);
        if (step) {
            g2.fillOval(px - 1, py + h - 4, 11, 7);
            g2.fillOval(px + W - 10, py + h - 2, 11, 6);
        } else {
            g2.fillOval(px + 1, py + h - 2, 11, 6);
            g2.fillOval(px + W - 12, py + h - 4, 11, 7);
        }
        // 眼睛
        g2.setColor(Color.WHITE);
        g2.fillOval(px + 6, py + 10, 6, 8);
        g2.fillOval(px + W - 12, py + 10, 6, 8);
        g2.setColor(Color.BLACK);
        g2.fillOval(px + 8, py + 13, 3, 4);
        g2.fillOval(px + W - 10, py + 13, 3, 4);
    }
}
