package mario;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * 程式合成的 8-bit 風背景音樂（不需任何音檔）。每首曲子是一段可無縫循環的波形，
 * 用 {@link Clip#loop} 持續播放。沒有音訊裝置時會安靜地略過。
 *
 * 曲子以「步進格（step grid）」記譜：每格代表一個八分音符，{@code REST} 為休止、
 * {@code T}（tie）為延續前一個音，藉此做出長短不一的切分節奏。每首由多個 16 格樂句
 * 組成（起・承・轉・合），整曲比單一樂句長很多。
 *
 * 旋律皆為原創音高，僅借用輕快平台跳躍遊戲常見的節奏感，不抄襲任何既有樂曲。
 *
 * 與 SFX 共用 M 鍵靜音：GamePanel 切換時呼叫 {@link #setMuted}。
 */
public enum Music {
    TITLE, MAP, STAGE, BOSS;

    private static final float RATE = 22050f;

    // 音高（十二平均律，A4=440Hz）。0 = 休止，-1 = 延續前音。
    private static final int REST = 0, T = -1;
    private static final int C4 = 262, D4 = 294, E4 = 330, F4 = 349, Fs4 = 370,
            G4 = 392, Gs4 = 415, A4 = 440, B4 = 494, Cs5 = 554;
    private static final int C5 = 523, D5 = 587, E5 = 659, F5 = 698,
            G5 = 784, A5 = 880, C6 = 1047;
    private static final int C3 = 131, D3 = 147, E3 = 165, F3 = 175, Fs3 = 185,
            G3 = 196, A3 = 220;
    private static final int E2 = 82, F2 = 87, A2 = 110;

    private Clip clip;
    private static volatile Music current;
    private static volatile boolean muted;

    static {
        try {
            // ---- TITLE：明亮的開場（約 15 秒）----
            int[] tA = {C5, REST, E5, G5, C6, T, G5, REST, A5, REST, F5, A5, G5, T, T, REST};
            int[] tB = {E5, F5, G5, A5, G5, T, E5, REST, C5, T, G5, T, C5, T, T, REST};
            int[] tC = {G5, A5, G5, E5, C5, T, E5, G5, A5, T, F5, REST, G5, T, T, REST};
            int[] tD = {C5, E5, G5, C6, A5, T, F5, REST, G5, T, E5, T, C5, T, T, REST};
            int[] tBass = {C3, T, T, REST, G3, T, T, REST, F3, T, T, REST, G3, T, T, REST};
            TITLE.clip = clip(build(0.156,
                    cat(tA, tB, tC, tA, tD, tB),
                    cat(tBass, tBass, tBass, tBass, tBass, tBass)));

            // ---- MAP：悠閒的世界地圖（D 大調，A B A B）----
            int[] mA = {D4, REST, Fs4, A4, D5, T, A4, REST, B4, REST, G4, B4, A4, T, T, REST};
            int[] mB = {Fs4, A4, D5, A4, G4, B4, D5, REST, A4, B4, Cs5, D5, A4, T, T, REST};
            int[] mBass = {D3, T, T, REST, Fs3, T, T, REST, G3, T, T, REST, A3, T, T, REST};
            MAP.clip = clip(build(0.20,
                    cat(mA, mB, mA, mB),
                    cat(mBass, mBass, mBass, mBass)));

            // ---- STAGE：輕快的關卡曲，起承轉合（約 30 秒）----
            int[] sA = {G4, T, E4, G4, A4, T, G4, E4, C5, T, B4, T, G4, T, T, REST};
            int[] sB = {E5, T, C5, D5, E5, T, G5, REST, A5, T, G5, E5, C5, T, T, REST};
            int[] sC = {F5, E5, D5, C5, A4, T, C5, REST, B4, D5, G5, REST, F5, T, E5, REST};
            int[] sD = {G4, A4, B4, C5, D5, T, E5, REST, G4, T, C5, T, C5, T, T, REST};
            int[] sE = {A4, B4, C5, D5, E5, T, C5, REST, F5, E5, D5, C5, B4, T, T, REST};
            int[] sF = {G5, T, E5, G5, A5, T, G5, E5, C6, T, A5, REST, G5, T, T, REST};
            int[] sG = {E5, D5, C5, B4, A4, T, G4, REST, C5, D5, E5, G5, E5, T, T, REST};
            int[] bC = {C3, T, T, REST, F3, T, T, REST, G3, T, T, REST, C3, T, G3, REST};
            int[] bMain = {C3, T, T, REST, A3, T, T, REST, F3, T, T, REST, G3, T, T, REST};
            int[] bBridge = {F3, T, T, REST, G3, T, T, REST, E3, T, T, REST, G3, T, T, REST};
            int[] bClimax = {C3, T, G3, REST, A3, T, E3, REST, F3, T, C3, REST, G3, T, G3, REST};
            STAGE.clip = clip(build(0.135,
                    cat(sA, sB, sA, sC, sB, sD, sE, sB, sF, sG, sC, sB, sD, sA),
                    cat(bC, bMain, bC, bBridge, bMain, bC, bMain, bMain, bClimax, bBridge, bBridge, bMain, bC, bC)));

            // ---- BOSS：緊張的小調戰鬥曲（約 20 秒）----
            int[] xA = {A4, REST, A4, C5, B4, REST, A4, G4, A4, REST, F4, A4, E4, T, T, REST};
            int[] xB = {C5, B4, A4, Gs4, A4, REST, E5, REST, F5, E5, D5, C5, B4, T, T, REST};
            int[] xC = {A4, A4, E5, A4, F5, REST, E5, REST, D5, REST, C5, B4, A4, T, T, REST};
            int[] xD = {E5, REST, D5, C5, B4, REST, C5, D5, E5, REST, F5, E5, A4, T, T, REST};
            int[] xE = {A4, C5, E5, A5, G5, REST, E5, REST, F5, E5, D5, C5, B4, T, T, REST};
            int[] xBass = {A2, T, A2, REST, A2, T, A2, REST, F2, T, F2, REST, E2, T, E2, REST};
            BOSS.clip = clip(build(0.115,
                    cat(xA, xB, xA, xC, xD, xB, xE, xC, xA, xD, xB),
                    cat(xBass, xBass, xBass, xBass, xBass, xBass, xBass, xBass, xBass, xBass, xBass)));
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

    private static int[] cat(int[]... parts) {
        int len = 0;
        for (int[] p : parts) len += p.length;
        int[] out = new int[len];
        int pos = 0;
        for (int[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }

    private static Clip clip(byte[] pcm) throws Exception {
        AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
        Clip c = AudioSystem.getClip();
        c.open(fmt, pcm, 0, pcm.length);
        return c;
    }

    /** 把 lead / bass 兩個步進格聲部混成一段完整循環。 */
    private static byte[] build(double stepSec, int[] lead, int[] bass) {
        int stepN = (int) (RATE * stepSec);
        int steps = Math.min(lead.length, bass.length);
        int n = stepN * steps;
        double[] mix = new double[n];
        addVoice(mix, lead, steps, stepN, 0.40);
        addVoice(mix, bass, steps, stepN, 0.28);

        // 頭尾各淡入淡出一點點，避免循環接縫的爆音。
        int fade = (int) (RATE * 0.005);
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

    /**
     * 把一個步進格聲部疊加到混音緩衝。遇到音符起點時，往後吃掉所有 {@code T}（tie）
     * 成為一個較長的音，整段使用連續相位並套用起音/收音的淡入淡出。
     */
    private static void addVoice(double[] mix, int[] steps, int count, int stepN, double vol) {
        int att = (int) (RATE * 0.004) + 1;
        int i = 0;
        while (i < count) {
            int f = steps[i];
            if (f <= 0) { // 休止或落單的 tie
                i++;
                continue;
            }
            int len = 1;
            while (i + len < count && steps[i + len] == T) len++;
            int total = len * stepN;
            int rel = Math.min((int) (RATE * 0.03) + 1, total / 3 + 1);
            double phase = 0;
            int base = i * stepN;
            for (int k = 0; k < total; k++) {
                phase += 2 * Math.PI * f / RATE;
                double s = Math.sin(phase) > 0 ? 1 : -1; // 方波
                double env = 1;
                if (k < att) env = (double) k / att;
                else if (k > total - rel) env = (double) (total - k) / rel;
                mix[base + k] += s * env * vol;
            }
            i += len;
        }
    }
}
