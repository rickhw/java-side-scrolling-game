package fario;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/** 簡單的視覺效果：彈出的金幣、磚塊碎片、得分文字。 */
public class Particle {
    public enum Kind { COIN, DEBRIS, TEXT }

    public final Kind kind;
    public double x, y, vx, vy;
    public double life;
    public String text;

    private Particle(Kind kind, double x, double y, double vx, double vy, double life) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
    }

    public static Particle coinPop(double x, double y) {
        return new Particle(Kind.COIN, x, y, 0, -9, 0.6);
    }

    public static Particle debris(double x, double y, double vx, double vy) {
        return new Particle(Kind.DEBRIS, x, y, vx, vy, 1.0);
    }

    public static Particle score(double x, double y, String text) {
        Particle p = new Particle(Kind.TEXT, x, y, 0, -1.2, 0.9);
        p.text = text;
        return p;
    }

    public boolean update(double dt) {
        life -= dt;
        if (kind != Kind.TEXT) vy += 0.4;
        x += vx;
        y += vy;
        return life > 0;
    }

    public void draw(Graphics2D g2, double camX) {
        int px = (int) (x - camX);
        int py = (int) y;
        switch (kind) {
            case COIN -> {
                g2.setColor(new Color(255, 200, 40));
                g2.fillOval(px - 7, py - 10, 14, 20);
                g2.setColor(new Color(255, 240, 150));
                g2.fillOval(px - 3, py - 7, 7, 14);
            }
            case DEBRIS -> {
                g2.setColor(new Color(190, 95, 50));
                g2.fillRect(px - 5, py - 5, 10, 10);
                g2.setColor(new Color(120, 55, 25));
                g2.drawRect(px - 5, py - 5, 10, 10);
            }
            case TEXT -> {
                g2.setColor(new Color(255, 255, 255, (int) Math.min(255, life * 400)));
                g2.setFont(new Font("Monospaced", Font.BOLD, 14));
                g2.drawString(text, px, py);
            }
        }
    }
}
