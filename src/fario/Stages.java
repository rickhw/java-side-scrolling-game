package fario;

import java.util.Random;

/**
 * 關卡工廠：用可重用的「區塊（segment）」組裝關卡，達到關卡 / 敵人的高重用性。
 *
 * 結構：{@link #WORLDS} 個世界，每個世界 {@link #STAGES} 關，
 * 每個世界的最後一關（第 {@link #STAGES} 關）是 Boss 關。
 *   World 1：草原（{@link Theme#GRASSLAND}）
 *   World 2：沙漠（{@link Theme#DESERT}）
 *
 * 一般關卡用固定種子（world, stage）隨機組裝，因此每次產生的地圖都一致；
 * 難度（坑洞寬度、敵人密度、尖刺蟲比例）隨世界與關數提升。
 */
public final class Stages {
    public static final int WORLDS = 2;
    public static final int STAGES = 5;

    private Stages() { }

    public static Theme themeOf(int world) {
        return world == 2 ? Theme.DESERT : Theme.GRASSLAND;
    }

    public static Level build(int world, int stage) {
        Theme theme = themeOf(world);
        return stage == STAGES ? buildBoss(world, theme) : buildField(world, stage, theme);
    }

    // ==================== 一般關卡 ====================

    private static Level buildField(int world, int stage, Theme theme) {
        Random rnd = new Random(world * 131L + stage * 17L);
        int cols = 170 + stage * 14;
        Level lv = new Level(theme, cols);
        lv.ground(0, cols - 1);
        lv.startCol = 3;

        double diff = (world - 1) * 0.6 + (stage - 1) * 0.18; // 0 ~ ~1.3
        int col = 10; // 開頭留一段平地
        int endZone = cols - 18;
        while (col < endZone) {
            col = switch (rnd.nextInt(6)) {
                case 0 -> segPipe(lv, col, rnd);
                case 1 -> segPit(lv, col, rnd, diff);
                case 2 -> segBricks(lv, col, rnd);
                case 3 -> segStairs(lv, col, rnd);
                case 4 -> segCoins(lv, col, rnd);
                default -> segEnemies(lv, col, rnd, world, diff);
            };
            col += 4 + rnd.nextInt(4); // 區塊之間留間隔
        }

        // 收尾：上樓梯 + 旗桿 + 城堡
        int baseCol = cols - 16;
        for (int i = 0; i < 6; i++) lv.stairColumn(baseCol + i, i + 1);
        lv.flagCol = cols - 8;
        lv.castleCol = cols - 4;
        return lv;
    }

    private static int segPipe(Level lv, int col, Random rnd) {
        lv.pipe(col, 2 + rnd.nextInt(3));
        return col + 2;
    }

    private static int segPit(Level lv, int col, Random rnd, double diff) {
        int w = 2 + (int) Math.min(2, Math.round(diff * 2)); // 寬度 2~4
        lv.pit(col, w);
        if (w >= 3) lv.set(col + w / 2, Level.GROUND_ROW - 1, 'X'); // 寬坑放一塊踏腳石
        return col + w;
    }

    private static int segBricks(Level lv, int col, Random rnd) {
        String pat = switch (rnd.nextInt(4)) {
            case 0 -> "B?B";
            case 1 -> "BMB";
            case 2 -> "?B?";
            default -> "BB?BB";
        };
        lv.putRow(col, 10, pat);
        lv.coinRow(col, 8, Math.min(pat.length(), 4));
        return col + pat.length();
    }

    private static int segStairs(Level lv, int col, Random rnd) {
        int n = 2 + rnd.nextInt(4);
        for (int i = 0; i < n; i++) lv.stairColumn(col + i, i + 1);
        return col + n;
    }

    private static int segCoins(Level lv, int col, Random rnd) {
        int n = 3 + rnd.nextInt(4);
        lv.coinRow(col, 9, n);
        return col + n;
    }

    private static int segEnemies(Level lv, int col, Random rnd, int world, double diff) {
        int n = 1 + (rnd.nextDouble() < diff ? 1 : 0);
        for (int i = 0; i < n; i++) lv.spawn(pickEnemy(rnd, world), col + i * 3);
        return col + n * 3 + 2;
    }

    /** 依世界挑敵人：越後面的世界尖刺蟲越多。 */
    private static Enemy.Type pickEnemy(Random rnd, int world) {
        double r = rnd.nextDouble();
        if (world >= 2) {
            if (r < 0.35) return Enemy.Type.GRUB;
            if (r < 0.65) return Enemy.Type.BEETLE;
            return Enemy.Type.SPIKER;
        }
        if (r < 0.6) return Enemy.Type.GRUB;
        if (r < 0.9) return Enemy.Type.BEETLE;
        return Enemy.Type.SPIKER;
    }

    // ==================== Boss 關 ====================

    private static Level buildBoss(int world, Theme theme) {
        int cols = 42;
        Level lv = new Level(theme, cols);
        lv.ground(0, cols - 1);
        lv.boss = true;
        lv.startCol = 3;
        lv.flagCol = -1;
        lv.castleCol = cols - 6;

        // 城堡前的高牆，把 Boss 圍在競技場內。
        int wallCol = cols - 9;
        for (int r = 8; r <= Level.GROUND_ROW - 1; r++) lv.set(wallCol, r, 'X');

        // 幾個掩體 / 火球練習磚 + 金幣。
        lv.putRow(13, 8, "B?B");
        lv.putRow(27, 8, "B?B");
        lv.coinRow(10, 6, 3);
        lv.coinRow(30, 6, 3);

        lv.spawn(Enemy.Type.BOSS, 22);
        return lv;
    }
}
