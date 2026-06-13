package fario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

/**
 * 以 World 為單位的地圖（類似經典平台遊戲的世界地圖）：玩家在節點之間移動，
 * 走到關卡節點後進入該關。每個 World 一張，外觀由 {@link Theme} 決定。
 *
 * 節點 1..N（N = {@link Stages#STAGES}），最後一個是 Boss 關。
 * 通關某關後解鎖下一關；只能在已解鎖的節點之間移動。
 */
public class WorldMap {
    public final int world;
    public final Theme theme;
    public final int count;

    private final int[] nx;
    private final int[] ny;
    private final boolean[] cleared;

    public int unlocked = 1; // 已解鎖到第幾關（1..count）
    public int cursor = 1;    // 目前所在節點

    public WorldMap(int world, Theme theme) {
        this.world = world;
        this.theme = theme;
        this.count = Stages.STAGES;
        this.cleared = new boolean[count + 1];
        // 節點座標（index 1..count）；最後一關較高，靠近終點。
        this.nx = new int[]{0, 130, 305, 470, 645, 835};
        this.ny = new int[]{0, 372, 300, 372, 296, 224};
    }

    public boolean isBoss(int s) { return s == count; }
    public boolean isCleared(int s) { return s >= 1 && s <= count && cleared[s]; }
    public void markCleared(int s) { if (s >= 1 && s <= count) cleared[s] = true; }
    public void unlock(int s) { if (s > unlocked) unlocked = Math.min(count, s); }
    public void unlockAll() { unlocked = count; }

    public void move(int dir) {
        int target = cursor + dir;
        if (target >= 1 && target <= Math.min(count, unlocked)) cursor = target;
    }

    // ---- 繪製 ----

    public void draw(Graphics2D g2, double animTime, int lives, int score, boolean god) {
        int W = GamePanel.WIDTH;
        int H = GamePanel.HEIGHT;

        g2.setPaint(new GradientPaint(0, 0, theme.skyTop, 0, H, theme.skyBottom));
        g2.fillRect(0, 0, W, H);

        // 遠景山丘 / 沙丘
        g2.setColor(theme.hill);
        for (int i = 0; i < 4; i++) g2.fillArc(i * 320 - 60, H - 150, 320, 230, 0, 180);

        // 地面帶
        g2.setColor(theme.groundFill);
        g2.fillRect(0, H - 64, W, 64);
        g2.setColor(theme.grass);
        g2.fillRect(0, H - 64, W, 10);

        // 路徑（已解鎖段亮、未解鎖段暗）
        g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int s = 1; s < count; s++) {
            boolean lit = s < unlocked || cleared[s];
            g2.setColor(lit ? new Color(255, 240, 160) : new Color(255, 255, 255, 70));
            g2.drawLine(nx[s], ny[s], nx[s + 1], ny[s + 1]);
        }
        g2.setStroke(new BasicStroke(1));

        for (int s = 1; s <= count; s++) drawNode(g2, s);
        drawAvatar(g2, nx[cursor], ny[cursor] - 36, animTime);

        // 抬頭資訊
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(8, 8, W - 16, 34, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        g2.drawString("WORLD " + world + " — " + theme.name, 22, 33);
        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2.drawString(String.format("LIVES x%d   SCORE %06d", Math.max(0, lives), score), W - 330, 31);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        String hint = "← → 移動關卡    ↑ / ENTER 進入" + (god ? "    [GOD] N=全解鎖" : "");
        int hw = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, (W - hw) / 2, H - 22);
    }

    private void drawNode(Graphics2D g2, int s) {
        int x = nx[s];
        int y = ny[s];
        int r = 22;
        boolean unlockedN = s <= unlocked;
        boolean clr = cleared[s];

        Color fill;
        if (!unlockedN) fill = new Color(120, 120, 120);
        else if (clr) fill = new Color(70, 165, 85);
        else if (isBoss(s)) fill = new Color(170, 60, 60);
        else fill = new Color(70, 120, 205);

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(x - r + 3, y - r + 4, r * 2, r * 2);
        g2.setColor(fill);
        g2.fillOval(x - r, y - r, r * 2, r * 2);
        g2.setColor(s == cursor ? new Color(255, 230, 80) : Color.WHITE);
        g2.setStroke(new BasicStroke(s == cursor ? 4 : 3));
        g2.drawOval(x - r, y - r, r * 2, r * 2);
        g2.setStroke(new BasicStroke(1));

        g2.setColor(Color.WHITE);
        if (isBoss(s)) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 15));
            g2.drawString("BOSS", x - 16, y + 5);
        } else {
            g2.setFont(new Font("Monospaced", Font.BOLD, 22));
            g2.drawString(String.valueOf(s), x - 6, y + 8);
        }
        if (clr) {
            g2.setColor(new Color(255, 225, 70));
            g2.fillOval(x + r - 12, y - r - 4, 15, 15);
            g2.setColor(new Color(150, 110, 20));
            g2.drawOval(x + r - 12, y - r - 4, 15, 15);
        }
    }

    private void drawAvatar(Graphics2D g2, int x, int y, double t) {
        y += (int) (Math.sin(t * 4) * 3); // 上下晃動
        g2.setColor(new Color(220, 50, 40)); // 帽子
        g2.fillRoundRect(x - 9, y - 14, 18, 8, 4, 4);
        g2.fillRect(x - 9, y - 8, 18, 4);
        g2.setColor(new Color(250, 200, 160)); // 臉
        g2.fillRect(x - 7, y - 6, 14, 8);
        g2.setColor(Color.BLACK);
        g2.fillRect(x + 1, y - 4, 2, 3); // 眼睛
        g2.setColor(new Color(50, 80, 200)); // 身體
        g2.fillRect(x - 8, y + 2, 16, 10);
        g2.setColor(new Color(110, 60, 20)); // 鞋
        g2.fillRect(x - 8, y + 12, 7, 3);
        g2.fillRect(x + 1, y + 12, 7, 3);
    }
}
