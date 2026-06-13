package fario;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel implements Runnable, KeyListener {
    public static final int WIDTH = 960;
    public static final int HEIGHT = Level.ROWS * Level.TILE; // 512
    private static final double DT = 1.0 / 60.0;

    private enum State { TITLE, MAP, PLAYING, DYING, LEVEL_CLEAR, GAME_OVER, WIN, PAUSED }

    // 開頭畫面隱藏指令：注音「很好玩」(cp3cl3j06) → 進入遊戲擁有 30 條命
    private static final String CHEAT_CODE = "cp3cl3j06";
    private static final int CHEAT_LIVES = 30;
    private final StringBuilder cheatBuffer = new StringBuilder();
    private boolean cheatActive;

    private Thread thread;
    private volatile boolean running;

    private Level level;
    private WorldMap map;
    private final Player player = new Player();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<Fireball> fireballs = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private Enemy boss; // Boss 關的頭目（沒有則為 null）

    private State state = State.PLAYING;
    private double stateTimer;
    private double animTime;

    private boolean left, right, run, jumpHeld, jumpWasHeld, fireHeld, fireWasHeld;
    private double fireCooldown;
    private boolean godMode; // 無敵模式：不會受傷、按住跳躍鍵飛行、N 鍵跳關、時間暫停

    private int world = 1, stage = 1;
    private int score, coins, lives;
    private double timeLeft;
    private double camX;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        gotoTitle(); // 啟動先停在開頭畫面
    }

    public void start() {
        running = true;
        thread = new Thread(this, "game-loop");
        thread.start();
    }

    private void gotoTitle() {
        cheatActive = false;
        cheatBuffer.setLength(0);
        world = 1;
        stage = 1;
        score = 0;
        coins = 0;
        lives = 3;
        player.power = 0;
        // 開頭畫面背後顯示一張關卡當背景
        level = Stages.build(1, 1);
        player.reset(level.startCol * Level.TILE, Level.GROUND_ROW * Level.TILE);
        enemies.clear();
        items.clear();
        fireballs.clear();
        particles.clear();
        boss = null;
        camX = 0;
        state = State.TITLE;
        Music.play(Music.TITLE);
    }

    /** 從開頭畫面開始新遊戲（進入第一個世界地圖）。 */
    private void startGame() {
        world = 1;
        stage = 1;
        score = 0;
        coins = 0;
        lives = cheatActive ? CHEAT_LIVES : 3;
        player.power = 0;
        enterWorldMap(1);
    }

    private void enterWorldMap(int w) {
        world = w;
        map = new WorldMap(w, Stages.themeOf(w));
        state = State.MAP;
        Music.play(Music.MAP);
    }

    private void enterStage(int s) {
        stage = s;
        resetLevel(0);
    }

    private void resetLevel(double invincibleTime) {
        level = Stages.build(world, stage);
        timeLeft = level.boss ? 200 : 300;
        enemies.clear();
        items.clear();
        fireballs.clear();
        particles.clear();
        boss = null;
        for (Level.Spawn s : level.enemySpawns) {
            Enemy e = new Enemy(s.type(), s.col() * Level.TILE + 2, Level.GROUND_ROW * Level.TILE);
            enemies.add(e);
            if (e.isBoss()) boss = e;
        }
        player.reset(level.startCol * Level.TILE, Level.GROUND_ROW * Level.TILE);
        player.invincible = invincibleTime;
        camX = 0;
        state = State.PLAYING;
        Music.play(level.boss ? Music.BOSS : Music.STAGE);
    }

    /** 過關（一般關抵達旗桿或 Boss 關擊敗頭目時呼叫）。 */
    private void clearStage() {
        int bonus = (int) timeLeft * 10;
        score += bonus;
        particles.add(Particle.score(player.x, player.y - 20, "+" + bonus));
        Sound.CLEAR.play();
        Music.stop();
        stateTimer = 0;
        state = State.LEVEL_CLEAR;
    }

    /** LEVEL_CLEAR 動畫結束後：回到地圖、前往下一個世界、或全破。 */
    private void afterStageClear() {
        map.markCleared(stage);
        if (stage < Stages.STAGES) {
            map.unlock(stage + 1);
            map.cursor = stage + 1;
            state = State.MAP;
            Music.play(Music.MAP);
        } else if (world < Stages.WORLDS) {
            enterWorldMap(world + 1);
        } else {
            state = State.WIN;
            Music.stop();
        }
    }

    private void bossDown() {
        double bx = boss != null ? boss.x : player.x;
        double by = boss != null ? boss.y : player.y;
        addScore(3000, bx, by);
        particles.add(Particle.score(player.x, player.y - 40, "BOSS DOWN!"));
        clearStage();
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        double acc = 0;
        while (running) {
            long now = System.nanoTime();
            acc += (now - last) / 1e9;
            last = now;
            if (acc > 0.25) acc = 0.25; // 避免螺旋追趕
            boolean updated = false;
            while (acc >= DT) {
                update();
                acc -= DT;
                updated = true;
            }
            if (updated) repaint();
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void update() {
        animTime += DT;

        switch (state) {
            case PLAYING -> updatePlaying();
            case DYING -> updateDying();
            case LEVEL_CLEAR -> {
                stateTimer += DT;
                if (stateTimer > 2.5) afterStageClear();
            }
            case TITLE, MAP, PAUSED, GAME_OVER, WIN -> { }
        }

        particles.removeIf(p -> !p.update(DT));
    }

    private void updatePlaying() {
        // 跳躍（按下邊緣觸發 + 放開縮短跳躍）
        if (jumpHeld && !jumpWasHeld && player.onGround) {
            player.jump();
            Sound.JUMP.play();
        }
        if (!jumpHeld && jumpWasHeld) player.cutJump();
        jumpWasHeld = jumpHeld;

        // 發射火球
        if (fireCooldown > 0) fireCooldown -= DT;
        if (fireHeld && !fireWasHeld && player.power == 2 && fireballs.size() < 2 && fireCooldown <= 0) {
            double fx = player.facing > 0 ? player.x + Player.W : player.x - Fireball.S;
            fireballs.add(new Fireball(fx, player.y + 10, player.facing));
            fireCooldown = 0.3;
            Sound.FIREBALL.play();
        }
        fireWasHeld = fireHeld;

        // 無敵模式：按住跳躍鍵飛行
        if (godMode && jumpHeld) {
            player.vy = Math.min(player.vy, -6);
            player.onGround = false;
        }

        player.update(level, left, right, run || godMode, DT, this);
        if (godMode && player.y < -100) player.y = -100; // 別飛出畫面太遠

        collectCoins();

        // 道具
        for (Item it : items) it.update(level, DT);
        Rectangle2D.Double pb = player.bounds();
        for (Item it : items) {
            if (it.remove || it.emerging() || !pb.intersects(it.bounds())) continue;
            it.remove = true;
            if (it.type == Item.Type.MUSHROOM) {
                if (player.power == 0) player.setPower(1);
            } else {
                player.setPower(player.power == 0 ? 1 : 2);
            }
            Sound.POWERUP.play();
            addScore(1000, it.x, it.y);
        }
        items.removeIf(i -> i.remove);

        // 敵人
        for (Enemy e : enemies) {
            if (!e.active && e.x < camX + WIDTH + 64) e.active = true;
            if (e.active) e.update(level, DT);
        }

        // 火球 vs 敵人
        for (Fireball f : fireballs) {
            f.update(level, DT);
            if (f.remove) continue;
            for (Enemy e : enemies) {
                if (!e.active || e.harmless() || e.remove) continue;
                if (!f.bounds().intersects(e.bounds())) continue;
                if (e.isBoss()) {
                    boolean dead = e.hurtBoss(f.vx > 0 ? 1 : -1);
                    Sound.KICK.play();
                    addScore(200, e.x, e.y);
                    if (dead) bossDown();
                } else {
                    e.flip(f.vx > 0 ? 1 : -1);
                    addScore(200, e.x, e.y);
                    Sound.KICK.play();
                }
                f.remove = true;
                break;
            }
        }
        fireballs.removeIf(f -> f.remove);

        // 滑行龜殼 vs 其他敵人
        for (Enemy shell : enemies) {
            if (shell.mode != Enemy.Mode.SHELL_MOVING) continue;
            for (Enemy e : enemies) {
                if (e == shell || !e.active || e.harmless() || e.mode == Enemy.Mode.SHELL_MOVING || e.isBoss()) continue;
                if (shell.bounds().intersects(e.bounds())) {
                    e.flip(shell.vx > 0 ? 1 : -1);
                    addScore(200, e.x, e.y);
                    Sound.KICK.play();
                }
            }
        }
        enemies.removeIf(e -> e.remove);

        // 玩家 vs 敵人
        pb = player.bounds();
        for (Enemy e : enemies) {
            if (!e.active || e.harmless()) continue;
            if (!pb.intersects(e.bounds())) continue;
            boolean stomp = player.vy > 0 && (player.y + player.h) - e.y < 16;
            int dir = player.x + Player.W / 2.0 < e.x + e.w / 2.0 ? 1 : -1;

            if (e.isBoss()) {
                if (stomp) {
                    boolean dead = e.hurtBoss(dir);
                    player.vy = -9;
                    Sound.STOMP.play();
                    addScore(300, e.x, e.y);
                    if (dead) bossDown();
                } else {
                    hurtPlayer();
                }
                if (state != State.PLAYING) return;
                continue;
            }

            if (stomp) {
                switch (e.mode) {
                    case WALK -> {
                        if (e.type == Enemy.Type.SPINY) {
                            hurtPlayer(); // 刺龜不能踩
                        } else if (e.type == Enemy.Type.KOOPA) {
                            e.toShell();
                            player.vy = -8;
                            Sound.STOMP.play();
                            addScore(100, e.x + e.w / 2.0, e.y);
                        } else {
                            e.squash();
                            player.vy = -8;
                            Sound.STOMP.play();
                            addScore(100, e.x + e.w / 2.0, e.y);
                        }
                    }
                    case SHELL -> {
                        e.kick(dir);
                        player.vy = -8;
                        Sound.KICK.play();
                    }
                    case SHELL_MOVING -> {
                        e.stopShell();
                        player.vy = -8;
                        Sound.STOMP.play();
                    }
                    default -> { }
                }
            } else {
                switch (e.mode) {
                    case SHELL -> {
                        e.kick(dir);
                        Sound.KICK.play();
                    }
                    case SHELL_MOVING -> {
                        if (e.kickGrace <= 0) hurtPlayer();
                    }
                    default -> hurtPlayer();
                }
            }
            if (state != State.PLAYING) return;
        }

        // 掉進坑洞
        if (player.y > HEIGHT + 50) {
            if (godMode) {
                // 無敵模式不會摔死，送回畫面上方飛回來
                player.y = -80;
                player.vy = 0;
            } else {
                die();
                return;
            }
        }

        // 倒數計時（無敵模式時間暫停，方便慢慢逛關卡）
        if (!godMode) {
            timeLeft -= DT;
            if (timeLeft <= 0) {
                timeLeft = 0;
                die();
                return;
            }
        }

        // 抵達旗桿 → 過關（Boss 關沒有旗桿，靠擊敗頭目過關）
        if (!level.boss && player.x + Player.W / 2.0 >= level.flagCol * Level.TILE + Level.TILE / 2.0) {
            clearStage();
        }

        // 攝影機跟隨
        camX = player.x - WIDTH / 2.0 + Player.W / 2.0;
        camX = Math.max(0, Math.min(camX, level.pixelWidth() - WIDTH));
    }

    private void updateDying() {
        player.vy += Player.GRAVITY;
        player.y += player.vy;
        stateTimer += DT;
        if (stateTimer > 2.0) {
            if (lives > 0) {
                player.power = 0; // 死亡後失去能力
                resetLevel(2.0);
            } else {
                state = State.GAME_OVER;
                Music.stop();
            }
        }
    }

    private void hurtPlayer() {
        if (godMode || player.invincible > 0) return;
        if (player.power > 0) {
            player.setPower(0);
            player.invincible = 2;
            Sound.POWERDOWN.play();
        } else {
            die();
        }
    }

    private void die() {
        lives--;
        state = State.DYING;
        stateTimer = 0;
        player.vx = 0;
        player.vy = -11;
        Sound.DIE.play();
        Music.stop();
    }

    private void collectCoins() {
        int t = Level.TILE;
        int c1 = (int) (player.x / t);
        int c2 = (int) ((player.x + Player.W - 1) / t);
        int r1 = (int) (player.y / t);
        int r2 = (int) ((player.y + player.h - 1) / t);
        for (int r = r1; r <= r2; r++) {
            for (int c = c1; c <= c2; c++) {
                if (level.get(c, r) == 'o') {
                    level.set(c, r, ' ');
                    addCoin();
                    addScore(100, c * t + t / 2.0, r * t);
                    Sound.COIN.play();
                }
            }
        }
    }

    private void addCoin() {
        coins++;
        if (coins >= 100) {
            coins -= 100;
            lives++;
            Sound.ONE_UP.play();
            particles.add(Particle.score(player.x, player.y - 30, "1UP!"));
        }
    }

    /** 玩家從下方撞到磚塊時由 Player 呼叫。 */
    public void bumpBlock(int col, int row) {
        char tile = level.get(col, row);
        double cx = col * Level.TILE + Level.TILE / 2.0;
        double cy = row * Level.TILE;
        switch (tile) {
            case '?' -> {
                level.set(col, row, 'U');
                addCoin();
                particles.add(Particle.coinPop(cx, cy - 6));
                addScore(200, cx, cy);
                Sound.COIN.play();
            }
            case 'M' -> {
                level.set(col, row, 'U');
                Item.Type type = player.power == 0 ? Item.Type.MUSHROOM : Item.Type.FLOWER;
                items.add(new Item(type, col * Level.TILE + 3, (row - 1) * Level.TILE + (Level.TILE - Item.H)));
                Sound.APPEAR.play();
            }
            case 'B' -> {
                if (player.power > 0) {
                    // 大 Fario撞碎磚塊
                    level.set(col, row, ' ');
                    particles.add(Particle.debris(cx - 8, cy + 8, -2.5, -7));
                    particles.add(Particle.debris(cx + 8, cy + 8, 2.5, -7));
                    particles.add(Particle.debris(cx - 6, cy + 20, -1.5, -4));
                    particles.add(Particle.debris(cx + 6, cy + 20, 1.5, -4));
                    addScore(50, cx, cy);
                    Sound.BREAK.play();
                } else {
                    Sound.BUMP.play();
                }
            }
            default -> Sound.BUMP.play();
        }
    }

    private void addScore(int amount, double x, double y) {
        score += amount;
        particles.add(Particle.score(x - 10, y - 6, "+" + amount));
    }

    // ---- 繪製 ----

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == State.MAP) {
            map.draw(g2, animTime, lives, score, godMode);
            return;
        }

        drawBackground(g2);
        // 升起中的道具畫在磚塊後面
        for (Item it : items) {
            if (it.emerging()) it.draw(g2, camX);
        }
        level.draw(g2, camX, animTime);
        for (Item it : items) {
            if (!it.emerging()) it.draw(g2, camX);
        }
        for (Enemy e : enemies) e.draw(g2, camX);
        for (Fireball f : fireballs) f.draw(g2, camX, animTime);
        player.draw(g2, camX);
        for (Particle p : particles) p.draw(g2, camX);
        drawHud(g2);
        drawOverlay(g2);
    }

    private void drawBackground(Graphics2D g2) {
        g2.setPaint(new GradientPaint(0, 0, level.skyTop, 0, HEIGHT, level.skyBottom));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        if (level.underground) return; // 地下關只用深色背景

        // 遠景山丘 / 沙丘（視差 0.4）
        g2.setColor(level.hill);
        for (int i = 0; i < level.pixelWidth() / 500 + 2; i++) {
            int hx = (int) (i * 500 - camX * 0.4);
            if (hx < -300 || hx > WIDTH + 300) continue;
            g2.fillArc(hx, HEIGHT - 64 - 90, 260, 180, 0, 180);
        }

        // 雲（視差 0.6）
        g2.setColor(new Color(255, 255, 255, 230));
        for (int i = 0; i < level.pixelWidth() / 340 + 2; i++) {
            int cx = (int) (i * 340 + (i % 3) * 60 - camX * 0.6);
            int cy = 50 + (i % 4) * 28;
            if (cx < -200 || cx > WIDTH + 200) continue;
            g2.fillOval(cx, cy, 70, 32);
            g2.fillOval(cx + 24, cy - 14, 56, 36);
            g2.fillOval(cx + 50, cy, 60, 30);
        }

        // 地面裝飾：草原畫草叢，沙漠畫仙人掌
        for (int i = 0; i < level.cols; i += 17) {
            int bx = (int) (i * Level.TILE - camX);
            if (bx < -150 || bx > WIDTH + 150) continue;
            int by = Level.GROUND_ROW * Level.TILE;
            if (level.desert) drawCactus(g2, bx, by);
            else drawBush(g2, bx, by);
        }
    }

    private void drawBush(Graphics2D g2, int bx, int by) {
        g2.setColor(level.grassDark);
        g2.fillArc(bx, by - 24, 50, 48, 0, 180);
        g2.fillArc(bx + 30, by - 32, 60, 64, 0, 180);
        g2.fillArc(bx + 64, by - 22, 46, 44, 0, 180);
    }

    private void drawCactus(Graphics2D g2, int bx, int by) {
        Color body = new Color(70, 150, 80);
        g2.setColor(body);
        g2.fillRoundRect(bx + 20, by - 62, 14, 62, 8, 8);   // 主幹
        g2.fillRoundRect(bx + 6, by - 40, 12, 26, 6, 6);    // 左臂（直）
        g2.fillRoundRect(bx + 6, by - 40, 16, 10, 6, 6);    // 左臂（橫）
        g2.fillRoundRect(bx + 36, by - 50, 12, 32, 6, 6);   // 右臂（直）
        g2.fillRoundRect(bx + 30, by - 50, 18, 10, 6, 6);   // 右臂（橫）
        g2.setColor(new Color(50, 120, 60));
        g2.drawRoundRect(bx + 20, by - 62, 14, 62, 8, 8);
    }

    private void drawHud(Graphics2D g2) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRoundRect(8, 8, WIDTH - 16, 30, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString(String.format("SCORE %06d", score), 24, 30);
        g2.drawString(String.format("COINS x%02d", coins), 240, 30);
        g2.drawString(String.format("WORLD %d-%d", world, stage), 430, 30);
        g2.drawString(String.format("LIVES x%d", Math.max(0, lives)), 610, 30);
        g2.drawString(String.format("TIME %3d", (int) Math.ceil(timeLeft)), 790, 30);
        int chipX = 24;
        if (Sound.muted) {
            g2.setColor(new Color(255, 255, 255, 150));
            g2.drawString("[MUTE]", chipX, 58);
            chipX += 80;
        }
        if (godMode) {
            g2.setColor(new Color(255, 230, 80));
            g2.drawString("[GOD] 按住跳躍=飛行  N=跳關", chipX, 58);
        }

        // Boss 血條
        if (level.boss && boss != null && !boss.remove && boss.hp > 0) {
            int n = boss.hp;
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRoundRect(WIDTH / 2 - 96, 44, 192, 24, 8, 8);
            g2.setColor(new Color(255, 90, 80));
            g2.setFont(new Font("Monospaced", Font.BOLD, 16));
            g2.drawString("BOSS", WIDTH / 2 - 86, 61);
            for (int i = 0; i < n; i++) {
                g2.fillRect(WIDTH / 2 - 34 + i * 26, 50, 20, 11);
            }
        }
    }

    private void drawOverlay(Graphics2D g2) {
        if (state == State.TITLE) {
            drawTitleScreen(g2);
            return;
        }

        String title = null;
        String sub = null;
        switch (state) {
            case GAME_OVER -> {
                title = "GAME OVER";
                sub = "總分 " + score + " — 按 ENTER 回到開頭畫面";
            }
            case LEVEL_CLEAR -> {
                if (level.boss) {
                    title = "BOSS DEFEATED!";
                    sub = world < Stages.WORLDS ? "前往下一個世界…" : "全部通關！";
                } else {
                    title = "WORLD " + world + "-" + stage + " CLEAR!";
                    sub = "回到地圖…";
                }
            }
            case WIN -> {
                title = "ALL CLEAR!";
                sub = "恭喜破關！總分 " + score + " — 按 ENTER 回到開頭畫面";
            }
            case PAUSED -> {
                title = "PAUSED";
                sub = "按 P 繼續";
            }
            default -> { }
        }
        if (title == null) return;

        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 52));
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (WIDTH - tw) / 2, HEIGHT / 2 - 20);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
        int sw = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, (WIDTH - sw) / 2, HEIGHT / 2 + 28);
        g2.setStroke(new BasicStroke(1));
    }

    private void drawTitleScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // 標題（黃字 + 黑色陰影）
        String title = "FARIO";
        g2.setFont(new Font("Monospaced", Font.BOLD, 96));
        int tw = g2.getFontMetrics().stringWidth(title);
        int tx = (WIDTH - tw) / 2;
        int ty = 190;
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, tx + 5, ty + 5);
        g2.setColor(new Color(255, 210, 60));
        g2.drawString(title, tx, ty);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
        drawCentered(g2, "8-bit 橫向捲軸冒險", 232);
        g2.setColor(new Color(200, 220, 255));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        drawCentered(g2, "v" + Main.version() + "   " + Stages.WORLDS + " 個世界 × " + Stages.STAGES + " 關（含 Boss）", 258);

        // 閃爍的開始提示
        if (((int) (animTime * 2)) % 2 == 0) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            drawCentered(g2, "按 ENTER 開始遊戲", 330);
        }

        g2.setColor(new Color(220, 220, 220, 200));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        drawCentered(g2, "← → / A D 移動    SPACE 跳躍    SHIFT 奔跑    X 火球    P 暫停    M 靜音", 392);

        if (cheatActive) {
            g2.setColor(new Color(255, 215, 0));
            g2.setFont(new Font("Monospaced", Font.BOLD, 22));
            drawCentered(g2, "★ 隱藏指令啟動：" + CHEAT_LIVES + " 條命 ★", 440);
        }
        g2.setStroke(new BasicStroke(1));
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        int w = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (WIDTH - w) / 2, y);
    }

    // ---- 鍵盤輸入 ----

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // M 靜音：SFX 與 BGM 一起
        if (code == KeyEvent.VK_M) {
            Sound.muted = !Sound.muted;
            Music.setMuted(Sound.muted);
            return;
        }

        if (state == State.MAP) {
            handleMapKeys(code);
            return;
        }

        switch (code) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SPACE, KeyEvent.VK_UP, KeyEvent.VK_W -> jumpHeld = true;
            case KeyEvent.VK_SHIFT -> run = true;
            case KeyEvent.VK_X, KeyEvent.VK_F -> fireHeld = true;
            case KeyEvent.VK_G -> toggleGod();
            case KeyEvent.VK_N -> {
                // 無敵模式專用：直接過關
                if (godMode && state == State.PLAYING) clearStage();
            }
            case KeyEvent.VK_P -> {
                if (state == State.PLAYING) {
                    state = State.PAUSED;
                    Music.stop();
                } else if (state == State.PAUSED) {
                    state = State.PLAYING;
                    Music.play(level.boss ? Music.BOSS : Music.STAGE);
                }
            }
            case KeyEvent.VK_ENTER -> {
                if (state == State.TITLE) startGame();
                else if (state == State.GAME_OVER || state == State.WIN) gotoTitle();
            }
            case KeyEvent.VK_R -> {
                if (state != State.TITLE) startGame();
            }
            default -> { }
        }
    }

    private void handleMapKeys(int code) {
        switch (code) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> map.move(-1);
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> map.move(1);
            case KeyEvent.VK_UP, KeyEvent.VK_W, KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> enterStage(map.cursor);
            case KeyEvent.VK_G -> toggleGod();
            case KeyEvent.VK_N -> {
                if (godMode) map.unlockAll();
            }
            case KeyEvent.VK_R -> startGame();
            default -> { }
        }
    }

    private void toggleGod() {
        godMode = !godMode;
        if (state == State.PLAYING) {
            particles.add(Particle.score(player.x - 30, player.y - 30,
                    godMode ? "GOD MODE ON" : "GOD MODE OFF"));
        }
        if (godMode) Sound.POWERUP.play();
        else Sound.POWERDOWN.play();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = false;
            case KeyEvent.VK_SPACE, KeyEvent.VK_UP, KeyEvent.VK_W -> jumpHeld = false;
            case KeyEvent.VK_SHIFT -> run = false;
            case KeyEvent.VK_X, KeyEvent.VK_F -> fireHeld = false;
            default -> { }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (state != State.TITLE) return;
        char c = Character.toLowerCase(e.getKeyChar());
        if (c < 0x20 || c == 0xFFFF) return; // 略過控制鍵
        cheatBuffer.append(c);
        if (cheatBuffer.length() > CHEAT_CODE.length()) {
            cheatBuffer.delete(0, cheatBuffer.length() - CHEAT_CODE.length());
        }
        if (!cheatActive && cheatBuffer.toString().equals(CHEAT_CODE)) {
            cheatActive = true;
            Sound.ONE_UP.play();
        }
    }
}
