package mario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 關卡地圖（tile-based），共 3 個世界。
 * 圖例: '#'=地面 'B'=磚塊 '?'=問號磚(金幣) 'M'=問號磚(道具) 'U'=已敲過
 *       'X'=石塊 'P'=水管 'o'=金幣 ' '=空
 */
public class Level {
    public static final int TILE = 32;
    public static final int ROWS = 16;

    public record Spawn(int col, Enemy.Type type) { }

    public final int world;
    public final int cols;
    private final char[][] g;
    public final List<Spawn> enemySpawns = new ArrayList<>();
    public int startCol = 3;
    public int flagCol;
    public int castleCol;

    // 主題配色
    public Color skyTop, skyBottom, hill;
    public Color groundFill, groundSpeck, grass, grassDark;
    public Color brickFill, brickLine;
    public boolean underground;

    public Level(int world) {
        this.world = world;
        this.cols = switch (world) {
            case 2 -> 200;
            case 3 -> 230;
            default -> 220;
        };
        g = new char[ROWS][cols];
        for (char[] row : g) Arrays.fill(row, ' ');
        applyTheme();
        switch (world) {
            case 2 -> build2();
            case 3 -> build3();
            default -> build1();
        }
    }

    private void applyTheme() {
        switch (world) {
            case 2 -> { // 地下
                skyTop = new Color(8, 8, 30);
                skyBottom = new Color(28, 28, 72);
                hill = new Color(40, 45, 90);
                groundFill = new Color(62, 78, 152);
                groundSpeck = new Color(46, 58, 115);
                grass = new Color(115, 150, 235);
                grassDark = new Color(85, 112, 195);
                brickFill = new Color(75, 115, 205);
                brickLine = new Color(38, 62, 130);
                underground = true;
            }
            case 3 -> { // 黃昏
                skyTop = new Color(250, 140, 90);
                skyBottom = new Color(255, 215, 150);
                hill = new Color(95, 120, 70);
                groundFill = new Color(150, 90, 40);
                groundSpeck = new Color(120, 70, 30);
                grass = new Color(90, 185, 70);
                grassDark = new Color(60, 150, 50);
                brickFill = new Color(190, 95, 50);
                brickLine = new Color(120, 55, 25);
                underground = false;
            }
            default -> { // 草原
                skyTop = new Color(110, 165, 250);
                skyBottom = new Color(170, 215, 255);
                hill = new Color(120, 195, 110);
                groundFill = new Color(150, 90, 40);
                groundSpeck = new Color(120, 70, 30);
                grass = new Color(90, 185, 70);
                grassDark = new Color(60, 150, 50);
                brickFill = new Color(190, 95, 50);
                brickLine = new Color(120, 55, 25);
                underground = false;
            }
        }
    }

    // ==================== World 1：草原 ====================
    private void build1() {
        ground(0, cols - 1);
        pit(45, 3);
        pit(91, 4);
        pit(140, 3);
        pit(178, 3);

        pipe(25, 2);
        pipe(55, 3);
        pipe(75, 4);
        pipe(130, 3);
        pipe(160, 2);

        set(16, 10, 'M');
        putRow(20, 10, "B?B?B");
        set(22, 6, '?');
        putRow(38, 10, "BBB");
        putRow(62, 10, "?BB?");
        putRow(82, 10, "BBMBB");
        putRow(84, 6, "B?B");
        putRow(100, 10, "BM?B");
        putRow(118, 10, "BBBB");
        putRow(120, 6, "??");
        putRow(146, 10, "BMB");
        putRow(168, 10, "?BB?");

        coinRow(38, 8, 3);
        coinRow(45, 10, 3);
        coinRow(91, 9, 4);
        coinRow(110, 11, 4);
        coinRow(118, 8, 4);
        coinRow(140, 10, 3);
        coinRow(152, 11, 4);
        coinRow(178, 10, 3);

        for (int i = 0; i < 8; i++) stairColumn(185 + i, i + 1);

        flagCol = 198;
        castleCol = 206;

        spawnAll(Enemy.Type.GOOMBA, 22, 30, 42, 66, 85, 100, 104, 135, 150, 172);
        spawnAll(Enemy.Type.KOOPA, 60, 120, 165);
    }

    // ==================== World 2：地下 ====================
    private void build2() {
        ground(0, cols - 1);
        // 天花板
        for (int c = 0; c <= 178; c++) {
            g[0][c] = '#';
            g[1][c] = '#';
        }

        pit(40, 3);
        pit(78, 4);
        pit(118, 3);
        pit(155, 3);

        pipe(60, 3);
        pipe(100, 4);
        pipe(145, 2);

        set(14, 10, 'M');
        putRow(18, 10, "B?B");
        putRow(26, 7, "BBB");
        coinRow(26, 5, 3);
        putRow(33, 10, "BB?BB");
        coinRow(40, 10, 3);
        putRow(48, 10, "?B?");
        putRow(69, 10, "BMB");
        coinRow(78, 9, 4);
        putRow(86, 10, "BBBB");
        putRow(88, 6, "??");
        putRow(108, 10, "?BB?");
        coinRow(109, 8, 2);
        coinRow(118, 10, 3);
        set(130, 10, 'M');
        putRow(136, 10, "B?B");
        coinRow(150, 11, 4);
        coinRow(155, 9, 3);
        putRow(162, 10, "BB");

        for (int i = 0; i < 7; i++) stairColumn(170 + i, i + 1);

        flagCol = 184;
        castleCol = 189;

        spawnAll(Enemy.Type.GOOMBA, 25, 45, 72, 92, 112, 150);
        spawnAll(Enemy.Type.KOOPA, 35, 65, 96, 140);
        spawnAll(Enemy.Type.SPINY, 52, 125, 165);
    }

    // ==================== World 3：黃昏（高難度） ====================
    private void build3() {
        ground(0, cols - 1);
        pit(30, 4);
        pit(60, 5);
        pit(95, 4);
        pit(130, 5);
        pit(170, 4);

        pipe(45, 2);
        pipe(85, 3);
        pipe(120, 4);
        pipe(155, 3);

        set(20, 10, 'M');
        putRow(24, 10, "?B");
        putRow(30, 11, "XXXX");   // 坑洞上的踏板
        coinRow(30, 9, 4);
        putRow(52, 9, "B?B");
        putRow(61, 11, "XXX");
        coinRow(61, 9, 3);
        putRow(70, 10, "BB?BB");
        set(72, 6, 'M');
        coinRow(95, 10, 4);
        putRow(104, 10, "B??B");
        set(110, 6, '?');
        putRow(131, 11, "XXX");
        coinRow(131, 9, 3);
        putRow(142, 10, "?BB?");
        set(144, 6, 'M');
        coinRow(160, 11, 4);
        coinRow(170, 9, 4);
        putRow(178, 10, "BB?BB");

        for (int i = 0; i < 4; i++) stairColumn(186 + i, i + 1);
        for (int i = 0; i < 8; i++) stairColumn(195 + i, i + 1);

        flagCol = 210;
        castleCol = 216;

        spawnAll(Enemy.Type.GOOMBA, 26, 50, 78, 105, 145, 175, 182);
        spawnAll(Enemy.Type.KOOPA, 40, 90, 115, 150, 165);
        spawnAll(Enemy.Type.SPINY, 55, 70, 108, 140, 160);
    }

    // ---- 建構輔助 ----

    private void spawnAll(Enemy.Type type, int... colsAt) {
        for (int c : colsAt) enemySpawns.add(new Spawn(c, type));
    }

    private void ground(int c1, int c2) {
        for (int c = c1; c <= c2; c++) {
            g[14][c] = '#';
            g[15][c] = '#';
        }
    }

    private void pit(int col, int width) {
        for (int c = col; c < col + width; c++) {
            g[14][c] = ' ';
            g[15][c] = ' ';
        }
    }

    private void pipe(int col, int height) {
        for (int r = 14 - height; r <= 13; r++) {
            g[r][col] = 'P';
            g[r][col + 1] = 'P';
        }
    }

    private void putRow(int col, int row, String tiles) {
        for (int i = 0; i < tiles.length(); i++) {
            char t = tiles.charAt(i);
            if (t != ' ') set(col + i, row, t);
        }
    }

    private void coinRow(int col, int row, int count) {
        for (int i = 0; i < count; i++) set(col + i, row, 'o');
    }

    private void stairColumn(int col, int height) {
        for (int r = 13; r > 13 - height; r--) g[r][col] = 'X';
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
        int px = (int) (flagCol * TILE + TILE / 2 - camX);
        if (px < -100 || px > GamePanel.WIDTH + 100) return;
        int top = 4 * TILE;
        int bottom = 14 * TILE;
        // 底座
        g2.setColor(new Color(170, 170, 160));
        g2.fillRect(px - 14, bottom - TILE, 28, TILE);
        g2.setColor(new Color(110, 110, 100));
        g2.drawRect(px - 14, bottom - TILE, 28, TILE);
        // 旗桿
        g2.setColor(new Color(60, 180, 70));
        g2.setStroke(new BasicStroke(5));
        g2.drawLine(px, top, px, bottom - TILE);
        g2.setStroke(new BasicStroke(1));
        // 頂端圓球
        g2.setColor(new Color(240, 210, 60));
        g2.fillOval(px - 7, top - 12, 14, 14);
        // 旗子
        g2.setColor(new Color(230, 60, 50));
        g2.fillPolygon(new int[]{px - 2, px - 2, px - 40}, new int[]{top + 4, top + 36, top + 20}, 3);
    }

    private void drawCastle(Graphics2D g2, double camX) {
        int x = (int) (castleCol * TILE - camX);
        if (x < -300 || x > GamePanel.WIDTH + 300) return;
        int groundY = 14 * TILE;
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
