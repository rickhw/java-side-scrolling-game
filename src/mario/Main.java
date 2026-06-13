package mario;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    /** Reads the version embedded in the jar manifest; falls back to "dev" when run from class files. */
    public static String version() {
        String v = Main.class.getPackage().getImplementationVersion();
        return v != null ? v : "dev";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Fario v" + version() + " - Super Mario (Java Edition)");
            GamePanel panel = new GamePanel();
            frame.add(panel);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            panel.start();
        });
    }
}
