# Catenin DSL - Kotlin Multiplatform 遊戲引擎

## 專案概述

Catenin 是 Junction 平台的教育卡牌遊戲引擎，採用 **Kotlin Multiplatform** 架構，核心邏輯編譯為前後端共享的程式庫。使用 YAML DSL 定義遊戲規則，專為 AI Agent 驅動的遊戲創作設計。

## 核心理念

### **跨邊界的蛋白質**
如同細胞膜上的 catenin 蛋白，此引擎跨越前後端邊界：
- **前端**: 編譯為 JavaScript，提供 TypeScript API
- **後端**: 編譯為 JVM bytecode，處理伺服器邏輯
- **核心**: 共享遊戲規則，零不一致性

### **AI 優先設計**
- YAML DSL 便於 AI 理解和修改
- 語義化的結構設計
- 內建 `ai_hints` 系統指導修改

## 技術架構

```
                 Kotlin Multiplatform Core
                         ↓
              ┌─────────────────────────┐
              │     commonMain/         │
              │   - GameEngine.kt       │
              │   - YamlParser.kt       │
              │   - EventSystem.kt      │
              │   - GameState.kt        │
              └─────────────────────────┘
                         ↓
        ┌────────────────┼────────────────┐
        ↓                                 ↓
   ┌─────────────┐                ┌─────────────┐
   │   JVM 後端   │                │  JS 前端     │
   │             │                │             │
   │ - REST API  │                │ - React UI  │
   │ - MongoDB   │                │ - Canvas    │
   │ - AI Agent  │                │ - 動畫效果   │
   └─────────────┘                └─────────────┘
```

## YAML DSL 設計

### 基本遊戲結構
```yaml
meta:
  name: "數字戰爭"
  target_age: [8, 12]
  player_count: [2, 4]

cards:
  attack_card:
    count: 15
    properties:
      damage: {type: int, min: 1, max: 5}
      element: {type: enum, values: [fire, water, earth]}
    events:
      on_play:
        action: "deal_damage"
        target: "opponent"
        amount: "{damage}"

mechanics:
  setup:
    players:
      health: 10
      hand_size: 5
  
  win_conditions:
    - type: "health_depleted"
      message: "{winner} 獲勝！"

ai_hints:
  difficulty_factors: [cards.attack_card.properties.damage.max, mechanics.setup.players.health]
  common_modifications:
    easier: {damage_max: 3, health: 15}
    harder: {damage_max: 7, health: 8}
```

## MVP 開發計劃

### **第一週：核心引擎 MVP**

#### **Day 1: Kotlin Multiplatform 專案架構**
- 建立 KMP 專案結構
- 配置 Gradle 多平台編譯
- 實作基礎 YAML 解析
- 建立資料模型

**交付目標**: 可解析 YAML 並轉換為遊戲物件

#### **Day 2: 玩家狀態與基本動作**
- 實作遊戲狀態管理
- 加入玩家模型和動作系統
- 支援抽牌、打牌基本操作
- 建立 stdout 介面

**交付目標**: 可進行基本玩家互動的命令列遊戲

#### **Day 3: 事件系統（核心）**
- 設計卡牌事件觸發機制
- 實作基本事件類型（傷害、治療、效果）
- 支援事件參數和目標選擇
- 事件處理順序管理

**交付目標**: 卡牌具備特殊能力，有真正的「玩法」

#### **Day 4: 回合管理與計分**
- 結構化回合階段流程
- 實作計分和生命值系統
- 加入回合結束條件
- 狀態變化追蹤

**交付目標**: 有節奏感的回合制遊戲

#### **Day 5: 勝利條件與完整遊戲**
- 多樣化勝利條件檢查
- 遊戲結束處理
- 完整的 stdout 遊戲體驗
- JS 編譯驗證

**交付目標**: 完整可玩的命令列遊戲 + JS 模組

## 前端工程師介面

編譯後的 JavaScript 模組提供簡潔 API：

```typescript
import { GameEngine, ActionResult } from './catenin-core'

// 創建遊戲
const game = GameEngine.fromYaml(yamlString, playerNames)

// 執行動作
const result: ActionResult = game.playCard(playerId, cardId)

// 獲取 UI 狀態
const uiState = game.getUIState()
// 包含：玩家手牌、場上狀態、動畫提示等
```

## 後端工程師介面

JVM 版本提供伺服器端功能：

```kotlin
// 創建遊戲實例
val gameEngine = GameEngine.fromYaml(yamlContent, playerIds)

// 處理 WebSocket 動作
val result = gameEngine.processPlayerAction(action)

// 同步到資料庫
mongoAdapter.saveGameState(gameEngine.getGameState())
```

## AI Agent 整合

核心引擎提供 AI 輔助功能：

```kotlin
// AI 修改遊戲難度
val hints = gameEngine.getAIHints()
val modifications = aiAgent.suggestModifications(hints, "make it easier")
val newGame = gameEngine.applyModifications(modifications)
```

## 成功指標

### 技術指標
- KMP 編譯成功率 100%
- 前後端遊戲狀態一致性 100%
- 遊戲啟動時間 < 500ms
- 支援 2-6 人遊戲

### 功能指標
- 完整的命令列遊戲體驗
- 前端可直接使用 JS 模組
- AI Agent 可理解和修改遊戲
- YAML 定義的遊戲可正常運行

## 下一階段規劃

MVP 完成後的發展方向：
- **第二週**: 複雜事件系統、多回合策略
- **第三週**: AI Agent 深度整合、自然語言轉換
- **第四週**: 教育功能、課堂適用工具

這個計劃專注於打造真正可用的跨平台遊戲引擎核心，為後續功能擴展奠定堅實基礎。