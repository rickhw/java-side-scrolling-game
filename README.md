# Fario — Super Mario (Java Edition)

[![CI](https://github.com/rickhw/fario/actions/workflows/ci.yml/badge.svg)](https://github.com/rickhw/fario/actions/workflows/ci.yml)
[![Release](https://github.com/rickhw/fario/actions/workflows/release.yml/badge.svg)](https://github.com/rickhw/fario/actions/workflows/release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/rickhw/fario?display_name=tag)](https://github.com/rickhw/fario/releases/latest)

用純 Java（Swing/AWT）打造的超級瑪莉風格橫向捲軸遊戲，無任何外部相依，連音效都是程式即時合成（不需音檔）。

> 目前版本：**0.1.0**（詳見 [CHANGELOG](CHANGELOG.md)）

## 下載與安裝

到 [Releases](https://github.com/rickhw/fario/releases/latest) 下載對應平台的安裝檔，**內建 Java 執行環境，安裝後直接玩、不需另外安裝 Java**：

| 平台 | 檔案 | 安裝方式 |
|------|------|----------|
| macOS | `Fario-x.y.z.dmg` | 開啟 dmg，把 Fario 拖到 Applications |
| Windows | `Fario-x.y.z.exe` | 執行安裝精靈 |
| Linux (Debian/Ubuntu) | `fario_x.y.z_amd64.deb` | `sudo dpkg -i fario_*.deb` |
| 任意平台 | `fario-x.y.z.jar` | 需自備 Java 17+，執行 `java -jar fario-*.jar` |

> macOS 的安裝檔未經 Apple 簽章，首次開啟若被 Gatekeeper 阻擋，請在「系統設定 → 隱私權與安全性」按一下「仍要打開」。

## 從原始碼執行

需要 **Java 17 以上**。

```bash
./run.sh                       # 編譯並直接執行
./build.sh                     # 只編譯並產生 dist/fario.jar
java -jar dist/fario.jar       # 執行打包好的 jar
```

## 操作方式

| 按鍵 | 功能 |
|------|------|
| ← → 或 A / D | 左右移動 |
| Space / ↑ / W | 跳躍（按住跳得高，放開跳得低） |
| Shift | 奔跑（加速） |
| X / F | 發射火球（需火力瑪莉狀態） |
| P | 暫停 / 繼續 |
| M | 靜音切換 |
| G | 無敵模式切換（探索用） |
| N | 跳到下一個世界（限無敵模式） |
| Enter | 開頭畫面開始遊戲 / 結束畫面回到開頭 |
| R | 隨時重新開始 |

## 遊戲內容

### 變身系統
- **蘑菇**：小瑪莉 → 大瑪莉（可撞碎磚塊）
- **火焰花**：大瑪莉 → 火力瑪莉（白帽紅褲，可發射火球）
- 道具藏在問號磚中；小瑪莉敲出蘑菇，大瑪莉敲出火焰花
- 受傷時降回小瑪莉（附 2 秒無敵），小瑪莉受傷才損失生命

### 敵人
- **栗寶寶（Goomba）**：行走巡邏，踩頭消滅（+100）
- **烏龜（Koopa）**：踩一下縮進殼裡，再碰龜殼會踢出去滑行，滑行的殼會消滅沿路所有敵人（每隻 +200），但反彈回來也會撞傷你
- **刺龜（Spiny）**：背上有刺不能踩！只能用火球或龜殼消滅

### 三個世界
1. **草原**：經典的藍天綠地入門關
2. **地下**：深藍洞窟，有天花板與更多烏龜、刺龜
3. **黃昏**：高難度——更寬的坑洞、踏板跳台、大量刺龜

### 無敵模式（探索用）
按 **G** 切換，方便快速瀏覽整個遊戲與關卡設計：
- 完全不會受傷，掉進坑洞會被送回畫面上方
- 按住跳躍鍵可以飛行，移動速度恆為奔跑速度
- 時間暫停，可以慢慢逛
- 按 **N** 直接跳到下一個世界（1 → 2 → 3 → 1 循環）

### 其他
- 火球沿地面彈跳、撞牆消失，同時最多 2 顆
- 金幣每收集 100 枚獎勵 1 條命（1UP）
- 每關 300 秒倒數，過關時剩餘秒數 ×10 換算加分
- 13 種程式合成的 8-bit 風音效（跳躍、金幣、踩敵、變身、火球、過關⋯）

## 專案結構

```
src/mario/
├── Main.java       # 程式進入點，建立視窗、顯示版本
├── GamePanel.java  # 遊戲主迴圈（60 FPS）、狀態機、碰撞互動、HUD
├── Level.java      # 3 個世界的關卡地圖、主題配色、地形繪製
├── Player.java     # 玩家物理、變身（小/大/火力）、繪製
├── Enemy.java      # 敵人 AI（栗寶寶/烏龜+龜殼/刺龜）
├── Item.java       # 道具（蘑菇、火焰花）
├── Fireball.java   # 火球彈道
├── Particle.java   # 視覺效果（金幣彈出、碎片、得分文字）
└── Sound.java      # 程式合成 8-bit 音效（方波/掃頻/噪音）

assets/             # 遊戲圖示（icon.png / icon.ico / icon.icns）
tools/IconGen.java  # 純 Java 圖示產生器（重繪後執行）
build.sh            # 編譯並產生可執行 jar（版本寫入 manifest、內嵌圖示）
package.sh          # 用 jpackage 產生原生安裝檔
VERSION             # 單一版本來源
.github/workflows/  # CI 與 Release 自動化
```

重新產生圖示：

```bash
javac -d build-tools tools/IconGen.java && java -cp build-tools IconGen
```

## 版本與發佈流程

- 版本號遵循 [Semantic Versioning](https://semver.org/)，單一來源為 [`VERSION`](VERSION) 檔，並寫入 jar manifest 供遊戲顯示。
- 推送 `vX.Y.Z` 格式的 tag 會觸發 [Release workflow](.github/workflows/release.yml)，
  在 macOS / Windows / Linux runner 上各自用 `jpackage` 打包，並把 `.dmg`、`.exe`、`.deb`
  與 `fario.jar` 自動附加到對應的 GitHub Release。

發佈新版本：

```bash
# 1. 更新 VERSION 與 CHANGELOG.md
echo "0.2.0" > VERSION
# 2. 提交
git commit -am "Release v0.2.0"
# 3. 打 tag 並推送（觸發自動發佈）
git tag v0.2.0
git push origin main --tags
```

也可在 Actions 頁面手動執行 Release workflow（`workflow_dispatch`），此時只會產生安裝檔 artifact，不會建立 Release。

本機自行打包（會產生對應目前作業系統的安裝檔）：

```bash
./package.sh dmg     # macOS
./package.sh exe     # Windows（需安裝 WiX Toolset）
./package.sh deb     # Linux
```

## 授權

本專案採用 [MIT License](LICENSE)。
