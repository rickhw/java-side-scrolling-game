# Changelog

本專案的所有重要變更都會記錄於此。
格式參考 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)，
版本號遵循 [Semantic Versioning](https://semver.org/lang/zh-TW/)。

## [0.3.0] - 2026-06-13

### Added
- **背景音樂**：新增 `Music`，程式即時合成可無縫循環的 8-bit 配樂（不需音檔），
  開頭、世界地圖、一般關卡、Boss 戰各有不同曲子；按 **M** 與音效一起靜音。
  每首以多個樂句組成起承轉合、含切分節奏的較長循環
  （開場約 15 秒、世界地圖約 13 秒、關卡約 30 秒、Boss 約 20 秒）；
  旋律為原創音高，僅借用輕快平台遊戲的節奏感。
- **世界地圖**：新增 `WorldMap`，每個世界一張地圖（草原、沙漠各一張）。
  玩家先在地圖上於關卡節點間移動（← →），再按 ↑ / Enter 進入關卡；
  通關後標記完成並解鎖下一關，最後一關（Boss）通關即前往下一個世界。

### Changed
- 遊戲流程改為「開頭畫面 → 世界地圖 → 進入關卡 → 過關回到地圖」；
  無敵模式 `N` 在關卡中為直接過關、在地圖上為解鎖全部關卡。

[0.3.0]: https://github.com/rickhw/fario/releases/tag/v0.3.0

## [0.2.0] - 2026-06-13

### Changed
- 重構關卡系統以提高重用性：把世界外觀抽成 `Theme`、把地圖建構積木放進通用的
  `Level`，新增 `Stages` 關卡工廠用可重用區塊（水管／坑洞／磚塊群／樓梯／金幣／
  敵人群）組裝關卡，並由難度參數驅動。
- `Enemy` 一般化尺寸與生命值（`w` / `hp` / `hitInvuln`），方便重用與擴充新敵人。

### Added
- 關卡結構由「3 個世界各 1 關」改為 **2 個世界、每個世界 5 關**：
  - **World 1：草原**（綠色為主）
  - **World 2：沙漠**（沙黃配色、仙人掌裝飾）
- 每個世界的**第 5 關為 Boss 關**：頭目需踩頭或火球擊中 5 次才能擊敗，
  畫面頂端顯示 Boss 血條。
- HUD 改以 `WORLD w-s` 顯示世界與關數；無敵模式 `N` 鍵改為逐關前進。

[0.2.0]: https://github.com/rickhw/fario/releases/tag/v0.2.0

## [0.1.0] - 2026-06-13

首次發佈 🎉

### Added
- 純 Java（Swing/AWT）橫向捲軸遊戲，無外部相依、音效程式即時合成。
- 三個世界（草原、地下、黃昏）、變身系統、敵人 AI 與 8-bit 音效。
- 開頭畫面（標題、版本、操作說明、按 ENTER 開始）與破關／結束畫面。
- 遊戲圖示（程式繪製的像素角色場景），同時用於視窗、Dock 與安裝檔，
  並由 `tools/IconGen.java` 純 Java 產生 `.png` / `.ico` / `.icns`。
- 跨平台發佈流程：透過 GitHub Actions + `jpackage` 打包 `.dmg`（macOS）、
  `.exe`（Windows）、`.deb`（Linux）原生安裝檔，以及可攜式 `fario.jar`。
- 安裝檔內建 Java 執行環境，玩家端不需另外安裝 Java。
- 視窗標題顯示版本資訊（讀取 jar manifest 的 `Implementation-Version`）。

[0.1.0]: https://github.com/rickhw/fario/releases/tag/v0.1.0
