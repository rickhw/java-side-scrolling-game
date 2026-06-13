# Changelog

本專案的所有重要變更都會記錄於此。
格式參考 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)，
版本號遵循 [Semantic Versioning](https://semver.org/lang/zh-TW/)。

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
