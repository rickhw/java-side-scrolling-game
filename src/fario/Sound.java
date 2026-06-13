package fario;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/** 程式合成的 8-bit 風音效（不需任何音檔）。按 M 可靜音。 */
public enum Sound {
    JUMP, COIN, STOMP, BUMP, BREAK, POWERUP, POWERDOWN, APPEAR, FIREBALL, KICK, DIE, CLEAR, ONE_UP;

    public static volatile boolean muted = false;
    private static final float RATE = 22050f;
    private Clip clip;

    static {
        try {
            JUMP.clip = clip(sweep(250, 760, 0.20, true));
            COIN.clip = clip(concat(square(988, 0.07), square(1319, 0.16)));
            STOMP.clip = clip(sweep(330, 90, 0.12, true));
            BUMP.clip = clip(square(95, 0.08));
            BREAK.clip = clip(noise(0.18));
            POWERUP.clip = clip(melody(0.055, 392, 523, 659, 784, 988, 1175, 1319));
            POWERDOWN.clip = clip(melody(0.055, 1319, 1175, 988, 784, 659, 523, 392));
            APPEAR.clip = clip(sweep(220, 900, 0.25, false));
            FIREBALL.clip = clip(sweep(720, 180, 0.10, true));
            KICK.clip = clip(square(420, 0.07));
            DIE.clip = clip(melody(0.09, 660, 622, 587, 523, 440, 349, 262, 196));
            CLEAR.clip = clip(melody(0.11, 523, 587, 659, 784, 880, 1047, 1319));
            ONE_UP.clip = clip(melody(0.08, 659, 784, 1319, 1047, 1175, 1568));
        } catch (Throwable t) {
            // 無音訊裝置時靜音執行，遊戲照常運作
        }
    }

    public void play() {
        Clip c = clip;
        if (muted || c == null) return;
        c.stop();
        c.setFramePosition(0);
        c.start();
    }

    private static Clip clip(byte[] pcm) throws Exception {
        AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
        Clip c = AudioSystem.getClip();
        c.open(fmt, pcm, 0, pcm.length);
        return c;
    }

    private static byte[] square(double freq, double dur) {
        return wave(freq, freq, dur, true);
    }

    private static byte[] sweep(double f1, double f2, double dur, boolean squareWave) {
        return wave(f1, f2, dur, squareWave);
    }

    private static byte[] wave(double f1, double f2, double dur, boolean squareWave) {
        int n = (int) (RATE * dur);
        byte[] out = new byte[n * 2];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double f = f1 + (f2 - f1) * t;
            phase += 2 * Math.PI * f / RATE;
            double s = Math.sin(phase);
            if (squareWave) s = s > 0 ? 1 : -1;
            double env = Math.min(1, (1 - t) / 0.3); // 結尾淡出避免爆音
            short v = (short) (s * env * 0.4 * 32767);
            out[i * 2] = (byte) v;
            out[i * 2 + 1] = (byte) (v >> 8);
        }
        return out;
    }

    private static byte[] noise(double dur) {
        int n = (int) (RATE * dur);
        byte[] out = new byte[n * 2];
        java.util.Random rnd = new java.util.Random(42);
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            short v = (short) ((rnd.nextDouble() * 2 - 1) * (1 - t) * 0.35 * 32767);
            out[i * 2] = (byte) v;
            out[i * 2 + 1] = (byte) (v >> 8);
        }
        return out;
    }

    private static byte[] melody(double noteDur, double... freqs) {
        byte[][] parts = new byte[freqs.length][];
        for (int i = 0; i < freqs.length; i++) parts[i] = square(freqs[i], noteDur);
        return concat(parts);
    }

    private static byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) len += p.length;
        byte[] out = new byte[len];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }
}
