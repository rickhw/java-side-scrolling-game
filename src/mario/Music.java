package mario;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * 程式合成的 8-bit 風背景音樂（不需任何音檔）。每首曲子是一段可無縫循環的波形，
 * 用 {@link Clip#loop} 持續播放。和 {@link Sound} 一樣，沒有音訊裝置時會安靜地略過。
 *
 * 與 SFX 共用 M 鍵靜音：GamePanel 切換時呼叫 {@link #setMuted}。
 */
public enum Music {
    TITLE, MAP, STAGE, BOSS;

    private static final float RATE = 22050f;
    private Clip clip;
    private static volatile Music current;
    private static volatile boolean muted;

    static {
        try {
            // 每首曲子：主旋律（lead，方波）+ 低音（bass），0 代表休止。
            TITLE.clip = clip(build(0.17,
                    new double[]{392, 523, 659, 784, 0, 659, 784, 0},
                    new double[]{131, 0, 0, 0, 196, 0, 0, 0}));
            MAP.clip = clip(build(0.26,
                    new double[]{523, 659, 784, 659, 880, 784, 659, 587},
                    new double[]{131, 0, 196, 0, 175, 0, 196, 0}));
            STAGE.clip = clip(build(0.15,
                    new double[]{659, 659, 0, 659, 0, 523, 659, 0, 784, 0, 0, 0, 392, 0, 0, 0},
                    new double[]{131, 0, 131, 0, 196, 0, 196, 0, 131, 0, 131, 0, 196, 0, 196, 0}));
            BOSS.clip = clip(build(0.13,
                    new double[]{440, 0, 440, 523, 440, 0, 392, 0, 440, 0, 440, 349, 440, 0, 330, 0},
                    new double[]{110, 0, 110, 0, 87, 0, 87, 0, 110, 0, 110, 0, 117, 0, 117, 0}));
        } catch (Throwable t) {
            // 無音訊裝置時靜音執行，遊戲照常運作
        }
    }

    /** 切換背景音樂；若該曲已在播放則不重來，達到跨關卡連續播放。 */
    public static void play(Music m) {
        if (current == m && m.clip != null && m.clip.isRunning()) return;
        stop();
        current = m;
        if (!muted && m.clip != null) {
            m.clip.setFramePosition(0);
            m.clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void stop() {
        if (current != null && current.clip != null) current.clip.stop();
    }

    public static void setMuted(boolean m) {
        muted = m;
        if (m) {
            stop();
        } else if (current != null && current.clip != null) {
            current.clip.setFramePosition(0);
            current.clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    // ---- 合成 ----

    private static Clip clip(byte[] pcm) throws Exception {
        AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
        Clip c = AudioSystem.getClip();
        c.open(fmt, pcm, 0, pcm.length);
        return c;
    }

    /** 把 lead / bass 兩個聲部混成一段完整循環。bass 比 lead 短時會自動重複。 */
    private static byte[] build(double beat, double[] lead, double[] bass) {
        int beatN = (int) (RATE * beat);
        int n = beatN * lead.length;
        double[] mix = new double[n];
        addVoice(mix, lead, beatN, 0.42);
        addVoice(mix, bass, beatN, 0.30);

        // 頭尾各淡入淡出一點點，避免循環接縫的爆音。
        int fade = (int) (RATE * 0.004);
        for (int i = 0; i < fade && i < n; i++) {
            double g = (double) i / fade;
            mix[i] *= g;
            mix[n - 1 - i] *= g;
        }

        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double s = Math.max(-1, Math.min(1, mix[i]));
            short v = (short) (s * 32767);
            out[i * 2] = (byte) v;
            out[i * 2 + 1] = (byte) (v >> 8);
        }
        return out;
    }

    private static void addVoice(double[] mix, double[] notes, int beatN, double vol) {
        int beats = mix.length / beatN;
        int att = (int) (beatN * 0.04) + 1;
        int rel = (int) (beatN * 0.12) + 1;
        for (int b = 0; b < beats; b++) {
            double f = notes[b % notes.length];
            if (f <= 0) continue; // 休止
            double phase = 0;
            for (int i = 0; i < beatN; i++) {
                phase += 2 * Math.PI * f / RATE;
                double s = Math.sin(phase) > 0 ? 1 : -1; // 方波
                double env = 1;
                if (i < att) env = (double) i / att;
                else if (i > beatN - rel) env = (double) (beatN - i) / rel;
                mix[b * beatN + i] += s * env * vol;
            }
        }
    }
}
