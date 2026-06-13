package fario;

import java.awt.Color;

/**
 * 世界主題（配色 + 裝飾風格）。把「外觀」從「關卡地圖」抽離出來，
 * 讓同一套地圖產生器可以套用到不同世界，提高重用性。
 */
public class Theme {
    public final String name;
    public final boolean underground;
    public final boolean desert;

    public final Color skyTop, skyBottom, hill;
    public final Color groundFill, groundSpeck, grass, grassDark;
    public final Color brickFill, brickLine;

    public Theme(String name, boolean underground, boolean desert,
                 Color skyTop, Color skyBottom, Color hill,
                 Color groundFill, Color groundSpeck, Color grass, Color grassDark,
                 Color brickFill, Color brickLine) {
        this.name = name;
        this.underground = underground;
        this.desert = desert;
        this.skyTop = skyTop;
        this.skyBottom = skyBottom;
        this.hill = hill;
        this.groundFill = groundFill;
        this.groundSpeck = groundSpeck;
        this.grass = grass;
        this.grassDark = grassDark;
        this.brickFill = brickFill;
        this.brickLine = brickLine;
    }

    /** World 1：草原（綠色為主）。 */
    public static final Theme GRASSLAND = new Theme(
            "草原", false, false,
            new Color(110, 165, 250), new Color(170, 215, 255), new Color(120, 195, 110),
            new Color(150, 90, 40), new Color(120, 70, 30), new Color(90, 185, 70), new Color(60, 150, 50),
            new Color(190, 95, 50), new Color(120, 55, 25));

    /** World 2：沙漠（沙黃為主）。 */
    public static final Theme DESERT = new Theme(
            "沙漠", false, true,
            new Color(135, 190, 235), new Color(232, 220, 180), new Color(225, 195, 130),
            new Color(222, 180, 115), new Color(196, 156, 92), new Color(244, 220, 150), new Color(208, 178, 108),
            new Color(220, 178, 110), new Color(165, 125, 70));
}
