package mario;

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
    private static final int WORLDS = 3;

    private enum State { PLAYING, DYING, LEVEL_CLEAR, GAME_OVER, WIN, PAUSED }

    private Thread thread;
    private volatile boolean running;

    private Level level;
    private final Player player = new Player();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<Fireball> fireballs = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private State state = State.PLAYING;
    private double stateTimer;
    private double animTime;

    private boolean left, right, run, jumpHeld, jumpWasHeld, fireHeld, fireWasHeld;
    private double fireCooldown;
    private boolean godMode; // 無敵模式：不會受傷、按住跳躍鍵飛行、N 鍵跳關、時間暫停

    private int world = 1;
    private int score, coins, lives;
    private double timeLeft;
    private double camX;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        newGame();
    }

    public void start() {
        running = true;
        thread = new Thread(this, "game-loop");
        thread.start();
    }

    private void newGame() {
        world = 1;
        score = 0;
        coins = 0;
        lives = 3;
        player.power = 0;
        resetLevel(0);
    }

    private void resetLevel(double invincibleTime) {
        level = new Level(world);
        timeLeft = 300;
        enemies.clear();
        items.clear();
        fireballs.clear();
        particles.clear();
        for (Level.Spawn s : level.enemySpawns) {
            enemies.add(new Enemy(s.type(), s.col() * Level.TILE + 2, 14 * Level.TILE));
        }
        player.reset(level.startCol * Level.TILE, 14 * Level.TILE);
        player.invincible = invincibleTime;
        camX = 0;
        state = State.PLAYING;
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
                if (stateTimer > 2.5) {
                    world++;
                    resetLevel(0);
                }
            }
            case PAUSED, GAME_OVER, WIN -> { }
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
                if (f.bounds().intersects(e.bounds())) {
                    e.flip(f.vx > 0 ? 1 : -1);
                    addScore(200, e.x, e.y);
                    Sound.KICK.play();
                    f.remove = true;
                    break;
                }
            }
        }
        fireballs.removeIf(f -> f.remove);

        // 滑行龜殼 vs 其他敵人
        for (Enemy shell : enemies) {
            if (shell.mode != Enemy.Mode.SHELL_MOVING) continue;
            for (Enemy e : enemies) {
                if (e == shell || !e.active || e.harmless() || e.mode == Enemy.Mode.SHELL_MOVING) continue;
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
            int dir = player.x + Player.W / 2.0 < e.x + Enemy.W / 2.0 ? 1 : -1;
            if (stomp) {
                switch (e.mode) {
                    case WALK -> {
                        if (e.type == Enemy.Type.SPINY) {
                            hurtPlayer(); // 刺龜不能踩
                        } else if (e.type == Enemy.Type.KOOPA) {
                            e.toShell();
                            player.vy = -8;
                            Sound.STOMP.play();
                            addScore(100, e.x + Enemy.W / 2.0, e.y);
                        } else {
                            e.squash();
                            player.vy = -8;
                            Sound.STOMP.play();
                            addScore(100, e.x + Enemy.W / 2.0, e.y);
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

        // 抵達旗桿 → 過關
        if (player.x + Player.W / 2.0 >= level.flagCol * Level.TILE + Level.TILE / 2.0) {
            int bonus = (int) timeLeft * 10;
            score += bonus;
            particles.add(Particle.score(player.x, player.y - 20, "+" + bonus));
            Sound.CLEAR.play();
            stateTimer = 0;
            state = world < WORLDS ? State.LEVEL_CLEAR : State.WIN;
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
                    // 大瑪莉撞碎磚塊
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

        // 遠景山丘（視差 0.4）
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

        // 草叢
        g2.setColor(level.grassDark);
        for (int i = 0; i < level.cols; i += 17) {
            int bx = (int) (i * Level.TILE - camX);
            if (bx < -150 || bx > WIDTH + 150) continue;
            int by = 14 * Level.TILE;
            g2.fillArc(bx, by - 24, 50, 48, 0, 180);
            g2.fillArc(bx + 30, by - 32, 60, 64, 0, 180);
            g2.fillArc(bx + 64, by - 22, 46, 44, 0, 180);
        }
    }

    private void drawHud(Graphics2D g2) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRoundRect(8, 8, WIDTH - 16, 30, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString(String.format("SCORE %06d", score), 24, 30);
        g2.drawString(String.format("COINS x%02d", coins), 240, 30);
        g2.drawString(String.format("WORLD %d/%d", world, WORLDS), 430, 30);
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
    }

    private void drawOverlay(Graphics2D g2) {
        String title = null;
        String sub = null;
        switch (state) {
            case GAME_OVER -> {
                title = "GAME OVER";
                sub = "按 ENTER 重新開始";
            }
            case LEVEL_CLEAR -> {
                title = "WORLD " + world + " CLEAR!";
                sub = "進入下一個世界…";
            }
            case WIN -> {
                title = "ALL CLEAR!";
                sub = "總分 " + score + " — 按 ENTER 再玩一次";
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

    // ---- 鍵盤輸入 ----

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SPACE, KeyEvent.VK_UP, KeyEvent.VK_W -> jumpHeld = true;
            case KeyEvent.VK_SHIFT -> run = true;
            case KeyEvent.VK_X, KeyEvent.VK_F -> fireHeld = true;
            case KeyEvent.VK_M -> Sound.muted = !Sound.muted;
            case KeyEvent.VK_G -> {
                godMode = !godMode;
                particles.add(Particle.score(player.x - 30, player.y - 30,
                        godMode ? "GOD MODE ON" : "GOD MODE OFF"));
                if (godMode) Sound.POWERUP.play();
                else Sound.POWERDOWN.play();
            }
            case KeyEvent.VK_N -> {
                // 無敵模式專用：跳到下一個世界（循環）
                if (godMode && (state == State.PLAYING || state == State.PAUSED)) {
                    world = world % WORLDS + 1;
                    resetLevel(0);
                    Sound.CLEAR.play();
                }
            }
            case KeyEvent.VK_P -> {
                if (state == State.PLAYING) state = State.PAUSED;
                else if (state == State.PAUSED) state = State.PLAYING;
            }
            case KeyEvent.VK_ENTER -> {
                if (state == State.GAME_OVER || state == State.WIN) newGame();
            }
            case KeyEvent.VK_R -> newGame();
            default -> { }
        }
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
    public void keyTyped(KeyEvent e) { }
}
