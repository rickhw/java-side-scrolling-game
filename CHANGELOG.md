# Changelog

本專案的所有重要變更都會記錄於此。
格式參考 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)，
版本號遵循 [Semantic Versioning](https://semver.org/lang/zh-TW/)。

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
