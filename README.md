# Junction - 教學遊戲互動平台

## 專案概述

Junction 是一個教學遊戲互動平台，以 cell junction 生物概念為命名靈感，讓教育者能夠透過 AI 協助創作線上 2D 卡牌桌遊，並提供完整的遊戲生態系統。

## 核心功能

- **遊戲創作引擎**: 透過 DSL 和 AI 協助創作卡牌桌遊
- **遊戲遊玩平台**: 線上多人 2D 卡牌桌遊
- **社群功能**: 評價系統、分享機制
- **群眾募資**: 維持平台運作
- **公益功能**: 待用額度系統，幫助弱勢孩童

## 技術架構

- **後端**: Kotlin + Quarkus
- **前端**: TypeScript
- **資料庫**: MongoDB
- **開發方法**: Test Driven Development

## 命名規範

基於 cell junction 生物概念：
- `cadherin`: DSL 解釋器 (server & client 共用)
- 其他組件將沿用細胞連接相關命名

## 開發計劃概覽

### Phase 1: 遊戲引擎核心 (優先開發)
1. **DSL 設計與實作** (`cadherin`)
2. **遊戲規則引擎**
3. **卡牌系統基礎設施**

### Phase 2: 平台基礎設施
1. **使用者管理系統**
2. **遊戲房間管理**
3. **即時通訊系統**

### Phase 3: 創作工具
1. **遊戲編輯器**
2. **AI 輔助創作**
3. **素材管理系統**

### Phase 4: 社群與商業模式
1. **評價與分享系統**
2. **群眾募資功能**
3. **待用額度系統**

## 目錄結構

```
junction/
├── docs/                     # 所有開發計劃文檔
│   ├── overview.md           # 總體規劃
│   ├── architecture.md       # 技術架構設計
│   ├── cadherin/            # DSL 相關文檔
│   ├── game-engine/         # 遊戲引擎計劃
│   ├── platform/            # 平台基礎設施計劃  
│   ├── creator-tools/       # 創作工具計劃
│   └── community/           # 社群功能計劃
├── cadherin/                # DSL 解釋器
├── game-engine/             # 遊戲引擎核心
├── platform-services/      # 平台微服務
├── creator-tools/           # 創作工具
└── web-client/             # 前端應用
```