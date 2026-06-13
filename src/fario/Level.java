package fario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 關卡地圖（tile-based）。本身不知道「第幾關」——只是一張可以被任意填充的格子地圖，
 * 配上一個 {@link Theme} 決定外觀。實際的關卡內容由 {@link Stages} 用可重用的
 * 區塊（segment）組裝而成。
 *
 * 圖例: '#'=地面 'B'=磚塊 '?'=問號磚(金幣) 'M'=問號磚(道具) 'U'=已敲過
 *       'X'=石塊 'P'=水管 'o'=金幣 ' '=空
 */
public class Level {
    public static final int TILE = 32;
    public static final int ROWS = 16;
    public static final int GROUND_ROW = 14; // 地面頂端所在的列

    public record Spawn(int col, Enemy.Type type) { }

    public final Theme theme;
    public final int cols;
    private final char[][] g;
    public final List<Spawn> enemySpawns = new ArrayList<>();

    public int startCol = 3;
    public int flagCol = -1;   // <0 表示沒有旗桿（例如 Boss 關）
    public int castleCol = -1;
    public boolean boss;

    // 從主題複製出來，繪圖時直接取用（GamePanel 背景也會讀）。
    public final Color skyTop, skyBottom, hill;
    public final Color groundFill, groundSpeck, grass, grassDark;
    public final Color brickFill, brickLine;
    public final boolean underground;
    public final boolean desert;

    public Level(Theme theme, int cols) {
        this.theme = theme;
        this.cols = cols;
        this.g = new char[ROWS][cols];
        for (char[] row : g) java.util.Arrays.fill(row, ' ');

        this.skyTop = theme.skyTop;
        this.skyBottom = theme.skyBottom;
        this.hill = theme.hill;
        this.groundFill = theme.groundFill;
        this.groundSpeck = theme.groundSpeck;
        this.grass = theme.grass;
        this.grassDark = theme.grassDark;
        this.brickFill = theme.brickFill;
        this.brickLine = theme.brickLine;
        this.underground = theme.underground;
        this.desert = theme.desert;
    }

    // ---- 可重用的建構積木（供 Stages 組裝關卡）----

    public void ground(int c1, int c2) {
        for (int c = c1; c <= c2; c++) {
            g[GROUND_ROW][c] = '#';
            g[GROUND_ROW + 1][c] = '#';
        }
    }

    public void ceiling(int c1, int c2) {
        for (int c = c1; c <= c2; c++) {
            g[0][c] = '#';
            g[1][c] = '#';
        }
    }

    public void pit(int col, int width) {
        for (int c = col; c < col + width; c++) {
            g[GROUND_ROW][c] = ' ';
            g[GROUND_ROW + 1][c] = ' ';
        }
    }

    public void pipe(int col, int height) {
        for (int r = GROUND_ROW - height; r <= GROUND_ROW - 1; r++) {
            set(col, r, 'P');
            set(col + 1, r, 'P');
        }
    }

    public void putRow(int col, int row, String tiles) {
        for (int i = 0; i < tiles.length(); i++) {
            char t = tiles.charAt(i);
            if (t != ' ') set(col + i, row, t);
        }
    }

    public void coinRow(int col, int row, int count) {
        for (int i = 0; i < count; i++) set(col + i, row, 'o');
    }

    public void stairColumn(int col, int height) {
        for (int r = GROUND_ROW - 1; r > GROUND_ROW - 1 - height; r--) set(col, r, 'X');
    }

    public void spawn(Enemy.Type type, int col) {
        enemySpawns.add(new Spawn(col, type));
    }

    // ---- 查詢與修改 ----

    public char get(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= ROWS) return ' ';
        return g[row][col];
    }

    public void set(int col, int row, char t) {
        if (col < 0 || col >= cols || row < 0 || row >= ROWS) return;
        g[row][col] = t;
    }

    public boolean isSolid(int col, int row) {
        if (col < 0 || col >= cols) return true; // 關卡左右邊界視為牆
        char t = get(col, row);
        return t == '#' || t == 'B' || t == '?' || t == 'M' || t == 'U' || t == 'X' || t == 'P';
    }

    public boolean solidInColumn(int col, double y, double h) {
        int r1 = (int) (y / TILE);
        int r2 = (int) ((y + h - 1) / TILE);
        for (int r = r1; r <= r2; r++) {
            if (isSolid(col, r)) return true;
        }
        return false;
    }

    public boolean solidInRow(int row, double x, double w) {
        int c1 = (int) (x / TILE);
        int c2 = (int) ((x + w - 1) / TILE);
        for (int c = c1; c <= c2; c++) {
            if (isSolid(c, row)) return true;
        }
        return false;
    }

    public int pixelWidth() {
        return cols * TILE;
    }

    // ---- 繪製 ----

    public void draw(Graphics2D g2, double camX, double animTime) {
        int firstCol = Math.max(0, (int) (camX / TILE) - 1);
        int lastCol = Math.min(cols - 1, (int) ((camX + GamePanel.WIDTH) / TILE) + 1);

        for (int r = 0; r < ROWS; r++) {
            for (int c = firstCol; c <= lastCol; c++) {
                int x = (int) (c * TILE - camX);
                int y = r * TILE;
                switch (g[r][c]) {
                    case '#' -> drawGround(g2, x, y, c, r);
                    case 'B' -> drawBrick(g2, x, y);
                    case '?', 'M' -> drawQuestion(g2, x, y, animTime);
                    case 'U' -> drawUsed(g2, x, y);
                    case 'X' -> drawStone(g2, x, y);
                    case 'P' -> drawPipe(g2, x, y, c, r);
                    case 'o' -> drawCoin(g2, x, y, animTime);
                    default -> { }
                }
            }
        }

        drawFlag(g2, camX);
        drawCastle(g2, camX);
    }

    private void drawGround(Graphics2D g2, int x, int y, int c, int r) {
        boolean topExposed = !isSolid(c, r - 1) && r > 1;
        g2.setColor(groundFill);
        g2.fillRect(x, y, TILE, TILE);
        g2.setColor(groundSpeck);
        g2.fillRect(x + 2, y + 18, 12, 10);
        g2.fillRect(x + 18, y + 6, 11, 9);
        if (topExposed) {
            g2.setColor(grass);
            g2.fillRect(x, y, TILE, 8);
            g2.setColor(grassDark);
            g2.fillRect(x, y + 6, TILE, 2);
        }
        g2.setColor(groundSpeck.darker());
        g2.drawRect(x, y, TILE - 1, TILE - 1);
    }

    private void drawBrick(Graphics2D g2, int x, int y) {
        g2.setColor(brickFill);
        g2.fillRect(x, y, TILE, TILE);
        g2.setColor(brickLine);
        g2.drawLine(x, y + 15, x + TILE - 1, y + 15);
        g2.drawLine(x + 15, y, x + 15, y + 15);
        g2.drawLine(x + 7, y + 16, x + 7, y + 31);
        g2.drawLine(x + 23, y + 16, x + 23, y + 31);
        g2.drawRect(x, y, TILE - 1, TILE - 1);
    }

    private void drawQuestion(Graphics2D g2, int x, int y, double t) {
        int bounce = (int) (Math.sin(t * 5) * 1.5);
        g2.setColor(new Color(235, 170, 40));
        g2.fillRect(x, y, TILE, TILE);
        g2.setColor(new Color(255, 215, 110));
        g2.fillRect(x + 2, y + 2, TILE - 4, 4);
        g2.fillRect(x + 2, y + 2, 4, TILE - 4);
        g2.setColor(new Color(150, 95, 15));
        g2.fillRect(x + 3, y + 3, 3, 3);
        g2.fillRect(x + TILE - 6, y + 3, 3, 3);
        g2.fillRect(x + 3, y + TILE - 6, 3, 3);
        g2.fillRect(x + TILE - 6, y + TILE - 6, 3, 3);
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        g2.drawString("?", x + 11, y + 24 + bounce);
        g2.setColor(new Color(150, 95, 15));
        g2.drawRect(x, y, TILE - 1, TILE - 1);
    }

    private void drawUsed(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(140, 90, 50));
        g2.fillRect(x, y, TILE, TILE);
        g2.setColor(new Color(100, 60, 30));
        g2.fillRect(x + 3, y + 3, 3, 3);
        g2.fillRect(x + TILE - 6, y + 3, 3, 3);
        g2.fillRect(x + 3, y + TILE - 6, 3, 3);
        g2.fillRect(x + TILE - 6, y + TILE - 6, 3, 3);
        g2.drawRect(x, y, TILE - 1, TILE - 1);
    }

    private void drawStone(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(170, 170, 160));
        g2.fillRect(x, y, TILE, TILE);
        g2.setColor(new Color(210, 210, 200));
        g2.fillRect(x + 2, y + 2, TILE - 4, 4);
        g2.setColor(new Color(110, 110, 100));
        g2.drawRect(x, y, TILE - 1, TILE - 1);
        g2.drawLine(x + 4, y + TILE - 5, x + TILE - 5, y + TILE - 5);
    }

    private void drawPipe(Graphics2D g2, int x, int y, int c, int r) {
        boolean isTop = get(c, r - 1) != 'P';
        boolean isLeft = get(c - 1, r) != 'P';
        g2.setColor(new Color(60, 170, 60));
        g2.fillRect(x, y, TILE, TILE);
        g2.setColor(new Color(120, 220, 120));
        g2.fillRect(x + (isLeft ? 4 : 0), y, 8, TILE);
        g2.setColor(new Color(25, 110, 25));
        if (isTop) {
            g2.fillRect(x - (isLeft ? 2 : 0), y, TILE + 2, 4);
            g2.drawRect(x - (isLeft ? 2 : 0), y, TILE + 1, 12);
        }
        if (isLeft) g2.drawLine(x, y, x, y + TILE);
        else g2.drawLine(x + TILE - 1, y, x + TILE - 1, y + TILE);
    }

    private void drawCoin(Graphics2D g2, int x, int y, double t) {
        double squeeze = Math.abs(Math.sin(t * 4)); // 旋轉感
        int w = (int) (16 * Math.max(0.25, squeeze));
        int cx = x + TILE / 2;
        g2.setColor(new Color(255, 200, 40));
        g2.fillOval(cx - w / 2, y + 6, w, 20);
        g2.setColor(new Color(255, 240, 150));
        g2.fillOval(cx - w / 4, y + 9, w / 2, 14);
        g2.setColor(new Color(180, 130, 10));
        g2.drawOval(cx - w / 2, y + 6, w, 20);
    }

    private void drawFlag(Graphics2D g2, double camX) {
        if (flagCol < 0) return;
        int px = (int) (flagCol * TILE + TILE / 2 - camX);
        if (px < -100 || px > GamePanel.WIDTH + 100) return;
        int top = 4 * TILE;
        int bottom = GROUND_ROW * TILE;
        g2.setColor(new Color(170, 170, 160));
        g2.fillRect(px - 14, bottom - TILE, 28, TILE);
        g2.setColor(new Color(110, 110, 100));
        g2.drawRect(px - 14, bottom - TILE, 28, TILE);
        g2.setColor(new Color(60, 180, 70));
        g2.setStroke(new BasicStroke(5));
        g2.drawLine(px, top, px, bottom - TILE);
        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(240, 210, 60));
        g2.fillOval(px - 7, top - 12, 14, 14);
        g2.setColor(new Color(230, 60, 50));
        g2.fillPolygon(new int[]{px - 2, px - 2, px - 40}, new int[]{top + 4, top + 36, top + 20}, 3);
    }

    private void drawCastle(Graphics2D g2, double camX) {
        if (castleCol < 0) return;
        int x = (int) (castleCol * TILE - camX);
        if (x < -300 || x > GamePanel.WIDTH + 300) return;
        int groundY = GROUND_ROW * TILE;
        Color wall = new Color(200, 200, 195);
        Color dark = new Color(140, 140, 135);
        g2.setColor(wall);
        g2.fillRect(x, groundY - 128, 160, 128);
        g2.fillRect(x + 40, groundY - 192, 80, 64);
        for (int i = 0; i < 5; i++) g2.fillRect(x + i * 34, groundY - 144, 22, 16);
        for (int i = 0; i < 3; i++) g2.fillRect(x + 42 + i * 30, groundY - 208, 18, 16);
        g2.setColor(dark);
        g2.fillArc(x + 60, groundY - 72, 40, 80, 0, 180);
        g2.fillRect(x + 60, groundY - 32, 40, 32);
        g2.fillRect(x + 16, groundY - 110, 18, 24);
        g2.fillRect(x + 126, groundY - 110, 18, 24);
        g2.fillRect(x + 70, groundY - 180, 20, 26);
        g2.setColor(new Color(110, 110, 105));
        g2.drawRect(x, groundY - 128, 160, 128);
        g2.drawRect(x + 40, groundY - 192, 80, 64);
    }
}
