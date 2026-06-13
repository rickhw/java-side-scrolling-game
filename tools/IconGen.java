import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates the Fario app icon (the in-game hero on a sky/ground scene) and writes,
 * with no external tools:
 *   assets/icon.png   512x512  (Linux installer + window/dock icon resource)
 *   assets/icon.ico            (Windows installer; PNG-embedded multi-size)
 *   assets/icon.icns           (macOS installer; PNG-embedded multi-size)
 *
 * Run from the project root:  javac -d build-tools tools/IconGen.java && java -cp build-tools IconGen
 */
public class IconGen {
    // Palette matches mario.Player so the icon looks like the game.
    static final Color RED = new Color(220, 50, 40);
    static final Color BLUE = new Color(50, 80, 200);
    static final Color SKIN = new Color(250, 200, 160);
    static final Color BROWN = new Color(110, 60, 20);
    static final Color BUTTON = new Color(250, 220, 80);

    public static void main(String[] args) throws Exception {
        Path assets = Path.of("assets");
        Files.createDirectories(assets);

        ImageIO.write(render(512), "png", assets.resolve("icon.png").toFile());
        writeIco(assets.resolve("icon.ico"), new int[]{16, 32, 48, 64, 128, 256});
        writeIcns(assets.resolve("icon.icns"));
        System.out.println("Wrote assets/icon.png, assets/icon.ico, assets/icon.icns");
    }

    /** Draws the icon at the given square size. */
    static BufferedImage render(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double s = size / 256.0;
        int rad = (int) (52 * s);

        // Rounded sky background.
        g.setPaint(new GradientPaint(0, 0, new Color(125, 200, 250), 0, size, new Color(60, 120, 210)));
        g.fillRoundRect(0, 0, size, size, rad, rad);
        // Clip everything else to the rounded shape.
        g.setClip(new RoundRectangle2D.Double(0, 0, size, size, rad, rad));

        // Clouds.
        g.setColor(new Color(255, 255, 255, 235));
        cloud(g, s, 40, 56);
        cloud(g, s, 168, 40);

        // Ground (dirt + grass).
        rect(g, s, new Color(150, 95, 55), 0, 206, 256, 50);
        rect(g, s, new Color(95, 175, 65), 0, 206, 256, 14);
        rect(g, s, new Color(60, 135, 45), 0, 206, 256, 4);

        // Hero (mario.Player big-sprite coordinates scaled up).
        drawHero(g, s);

        // Soft inner border.
        g.setColor(new Color(255, 255, 255, 150));
        g.setStroke(new BasicStroke((float) Math.max(1, 5 * s)));
        g.drawRoundRect((int) (3 * s), (int) (3 * s), size - (int) (6 * s), size - (int) (6 * s), rad, rad);

        g.dispose();
        return img;
    }

    static void cloud(Graphics2D g, double s, double x, double y) {
        oval(g, s, x, y + 8, 40, 22);
        oval(g, s, x + 18, y, 34, 30);
        oval(g, s, x + 40, y + 8, 36, 22);
    }

    static void drawHero(Graphics2D g, double s) {
        double k = 3.6, ox = 81, oy = 52; // 26x44 sprite -> ~94x158, feet near ground
        g.setColor(RED);
        spr(g, s, ox, oy, k, 3, 0, 20, 6);
        spr(g, s, ox, oy, k, 6, 5, 20, 3);
        g.setColor(SKIN);
        spr(g, s, ox, oy, k, 4, 8, 18, 10);
        g.setColor(Color.BLACK);
        spr(g, s, ox, oy, k, 16, 10, 3, 5);
        g.setColor(BROWN);
        spr(g, s, ox, oy, k, 14, 15, 8, 3);
        g.setColor(RED);
        spr(g, s, ox, oy, k, 2, 18, 22, 9);
        g.setColor(BLUE);
        spr(g, s, ox, oy, k, 4, 25, 18, 13);
        spr(g, s, ox, oy, k, 7, 18, 3, 8);
        spr(g, s, ox, oy, k, 16, 18, 3, 8);
        g.setColor(BUTTON);
        sprOval(g, s, ox, oy, k, 8, 20, 4, 4);
        sprOval(g, s, ox, oy, k, 14, 20, 4, 4);
        g.setColor(SKIN);
        sprOval(g, s, ox, oy, k, 21, 24, 6, 6);
        g.setColor(BROWN);
        spr(g, s, ox, oy, k, 2, 38, 9, 5);
        spr(g, s, ox, oy, k, 15, 38, 9, 5);
    }

    // ---- drawing helpers (coordinates in a 256-unit design space) ----

    static void rect(Graphics2D g, double s, Color c, double x, double y, double w, double h) {
        g.setColor(c);
        g.fillRect((int) (x * s), (int) (y * s), (int) Math.ceil(w * s), (int) Math.ceil(h * s));
    }

    static void oval(Graphics2D g, double s, double x, double y, double w, double h) {
        g.fillOval((int) (x * s), (int) (y * s), (int) Math.ceil(w * s), (int) Math.ceil(h * s));
    }

    static void spr(Graphics2D g, double s, double ox, double oy, double k, double x, double y, double w, double h) {
        g.fillRect((int) ((ox + x * k) * s), (int) ((oy + y * k) * s),
                (int) Math.ceil(w * k * s), (int) Math.ceil(h * k * s));
    }

    static void sprOval(Graphics2D g, double s, double ox, double oy, double k, double x, double y, double w, double h) {
        g.fillOval((int) ((ox + x * k) * s), (int) ((oy + y * k) * s),
                (int) Math.ceil(w * k * s), (int) Math.ceil(h * k * s));
    }

    // ---- container formats (PNG payloads, no external tooling) ----

    static byte[] png(int size) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ImageIO.write(render(size), "png", b);
        return b.toByteArray();
    }

    /** Windows .ico holding PNG-compressed images (supported since Windows Vista). */
    static void writeIco(Path out, int[] sizes) throws Exception {
        byte[][] imgs = new byte[sizes.length][];
        for (int i = 0; i < sizes.length; i++) imgs[i] = png(sizes[i]);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(body);
        writeLE16(d, 0);              // reserved
        writeLE16(d, 1);              // type: icon
        writeLE16(d, sizes.length);   // image count
        int offset = 6 + 16 * sizes.length;
        for (int i = 0; i < sizes.length; i++) {
            int dim = sizes[i] >= 256 ? 0 : sizes[i];
            d.writeByte(dim);         // width  (0 = 256)
            d.writeByte(dim);         // height (0 = 256)
            d.writeByte(0);           // palette
            d.writeByte(0);           // reserved
            writeLE16(d, 1);          // color planes
            writeLE16(d, 32);         // bits per pixel
            writeLE32(d, imgs[i].length);
            writeLE32(d, offset);
            offset += imgs[i].length;
        }
        for (byte[] img : imgs) d.write(img);
        Files.write(out, body.toByteArray());
    }

    /** macOS .icns holding PNG payloads. */
    static void writeIcns(Path out) throws Exception {
        // OSType -> pixel size for PNG-based entries.
        Object[][] entries = {
                {"ic11", 32}, {"ic12", 64}, {"ic07", 128},
                {"ic13", 256}, {"ic08", 256}, {"ic09", 512}, {"ic10", 1024},
        };
        ByteArrayOutputStream chunks = new ByteArrayOutputStream();
        for (Object[] e : entries) {
            byte[] data = png((Integer) e[1]);
            writeIcnsChunk(chunks, (String) e[0], data);
        }
        byte[] body = chunks.toByteArray();
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(file);
        d.writeBytes("icns");
        d.writeInt(8 + body.length); // total file length (big-endian)
        d.write(body);
        Files.write(out, file.toByteArray());
    }

    static void writeIcnsChunk(OutputStream os, String type, byte[] data) throws Exception {
        DataOutputStream d = new DataOutputStream(os);
        d.writeBytes(type);
        d.writeInt(8 + data.length); // chunk length includes the 8-byte header
        d.write(data);
    }

    static void writeLE16(DataOutputStream d, int v) throws Exception {
        d.writeByte(v & 0xFF);
        d.writeByte((v >> 8) & 0xFF);
    }

    static void writeLE32(DataOutputStream d, int v) throws Exception {
        d.writeByte(v & 0xFF);
        d.writeByte((v >> 8) & 0xFF);
        d.writeByte((v >> 16) & 0xFF);
        d.writeByte((v >> 24) & 0xFF);
    }
}
