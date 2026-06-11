# 2026-03-03: UI Integration Research — Cadherin 技術選型

## 背景

Phase 1 (Data Models) 已完成，引擎核心穩固。開始研究 Cadherin (UI/Graphics layer) 的技術選型，探索如何讓 Catenin 的 YAML game definition 自動產生可玩的遊戲 UI。

身為後端開發者，需要找到一條不需要深入前端專業的路徑，同時利用 AI 在 UI 生成方面的最新進展。

## 研究方法

使用 5 個平行研究 agent 同時探索不同方向：
1. AI-Generated UI frameworks (v0.dev, Vercel AI SDK, A2UI, CopilotKit)
2. Browser game rendering engines (Phaser 4, PixiJS v8, Excalibur, Kaplay)
3. React/web approaches for game UI (boardgame.io, Framer Motion, dnd-kit)
4. Config-driven UI / no-code game builders (SDUI, GDevelop, Wanderer)
5. Multiplayer architecture (Colyseus, PartyKit, Hearthstone protocol, BGA)

## 核心發現

### 1. 不需要 Game Engine — 需要 Rendering Layer

Catenin 已經是 game engine。加 Phaser/Excalibur/Kaplay 等完整 game framework 會造成兩個引擎搶控制權的衝突。需要的是純粹的 rendering layer。

**結論：排除 Phaser, Excalibur, Kaplay, Godot**

### 2. Hybrid Architecture: React + PixiJS

最佳方案是混合架構：

```
┌─────────────────────────────────────────┐
│  React Layer (HTML/CSS, DOM overlay)    │
│  - Health bars, score, turn indicator   │
│  - Menus, chat, hand management         │
│  - Responsive layout, accessibility     │
│                                         │
│  PixiJS Canvas (underneath)             │
│  - Game board, sprites, bitmap art      │
│  - Particle effects, animations         │
│  - Adventure scenes, card art rendering │
└─────────────────────────────────────────┘
```

**為什麼：**
- PixiJS v8 是純 rendering engine，不干涉 game logic（完美配合 Catenin）
- @pixi/react v8 讓 PixiJS 可以用 JSX 寫（`<pixiSprite texture="card.png" />`）
- React 處理 UI chrome（選單、計分板、手牌互動）
- PixiJS 處理 game visuals（bitmap art, sprites, 粒子效果, 60fps 動畫）
- AI (Claude/GPT) 對 React + PixiJS 的程式碼生成能力遠優於純 game engine code

### 3. Server-Driven UI (SDUI) + Component Registry Pattern

來自 Airbnb/Netflix/Instagram 的 SDUI 模式完美適用：

```
YAML Game Definition
        │
        ▼
Catenin Engine (Kotlin/JS)
        │
        ▼
UI Description (JSON)          ← 新的 data contract
   {
     template: "card_game",
     components: [
       { type: "hand", cards: [...] },
       { type: "board", slots: [...] }
     ],
     animations: [...],
     availableActions: [...]
   }
        │
        ▼
Component Registry (React + PixiJS)
   componentMap = {
     hand: <HandComponent />,
     board: <BoardComponent />,
     card: <CardComponent />
   }
        │
        ▼
Playable Game in Browser
```

### 4. Template System 是殺手級功能

不需要每個遊戲都寫 UI code。建立 3-4 個 game templates：

| Template | Components | 適用遊戲 |
|----------|-----------|----------|
| `card_game` | Hand, Deck, DiscardPile, PlayArea, Score | 卡牌遊戲、教學卡牌 |
| `board_game` | Grid/Hex Board, Tokens, Dice, TurnIndicator | 棋盤遊戲、策略遊戲 |
| `adventure` | Scene, DialogueBox, InventoryPanel, Hotspots | 冒險遊戲、互動故事 |
| `quiz` | QuestionCard, AnswerButtons, Timer, Leaderboard | 測驗、問答遊戲 |

教育者寫 YAML + 選 template → 自動產生可玩遊戲。**零前端程式碼。**

### 5. Multiplayer 已經被 Catenin 的架構解決了

Catenin 的 immutable GameWorld + WorldUpdate events 本質上就是 Hearthstone 的 power history model：

```
Client sends: PlayCard("fire_spell")
Server produces: [CardPlayed, DamageDealt, HealthUpdated, CardDestroyed]
Client animation queue processes sequentially with animations
```

只需要：
- Ktor/Quarkus WebSocket layer
- Serialize WorldUpdate events
- Per-player view filtering（隱藏對手手牌）
- Animation queue on client

**不需要 Colyseus, PartyKit, CRDTs。**

### 6. AI Integration 三階段

**Phase 1 (Build-time):** AI 讀 YAML → 生成 React UI components → 累積成 template library
**Phase 2 (Declarative runtime):** Catenin 輸出 A2UI-style JSON → component registry 自動 render
**Phase 3 (Full generative):** 教育者用自然語言描述遊戲 → AI 生成 YAML + 選/客製 template → 即時可玩

## 關鍵技術發現

### 新標準/框架 (2025-2026)

| 技術 | 說明 | 與 Junction 的關聯 |
|------|------|-------------------|
| **Google A2UI** | Agent-to-User Interface 開放標準，declarative JSON UI | 架構模型參考 |
| **CopilotKit / AG-UI** | AI agent ↔ frontend 雙向協議 | Phase 3 AI 整合 |
| **@pixi/react v8** | PixiJS 的 React binding，JSX 寫 canvas | 核心渲染方案 |
| **PixiJS Layout v3** | PixiJS 內建 flexbox-like layout | Canvas 內 UI layout |
| **Motion (Framer Motion)** | React 動畫庫 | 卡牌翻轉、佈局轉場 |
| **dnd-kit** | React drag-and-drop | 出牌、移動棋子 |
| **Pixi'VN** | PixiJS 視覺小說引擎 + React UI | 冒險遊戲參考實作 |
| **Wanderer** | YAML-only 遊戲引擎 (.NET) | 證明 YAML→遊戲 可行 |

### 參考實作

- **Sunwell** — HTML5 Canvas 繪製 Hearthstone 卡牌（bitmap art + effects）
- **react-redux-card-game** — React+Redux 的 Hearthstone clone
- **Board Game Arena** — PHP+MySQL+JS，1000+ 桌遊，驗證 server-authoritative 模式
- **Pixi'VN** — PixiJS scene rendering + React UI overlay（冒險遊戲）

## 建議的技術 Stack

| Layer | Technology | 理由 |
|-------|-----------|------|
| Game Engine | Catenin (Kotlin/JVM + JS) | 已完成 |
| UI Framework | React 19 | AI-friendly, 巨大生態系 |
| Game Canvas | PixiJS v8 + @pixi/react | 純 renderer，JSX 整合 |
| Animations | Motion (Framer Motion) + GSAP | 卡牌動畫、layout 轉場 |
| Drag & Drop | dnd-kit | 出牌、移棋 |
| Styling | Tailwind + shadcn/ui | 美觀預設、可客製主題 |
| Card Rendering | CSS Playing Cards + PixiJS sprites | 無障礙 + 美術效果 |
| WebSocket | Ktor or Quarkus | Native Kotlin, 協程 |
| Wire Protocol | JSON (初期) → Protobuf (優化) | 簡單開始 |
| State Sync | WorldUpdate event stream | Hearthstone model |

## 下一步行動

1. **完成 Phase 2 trigger system** — 引擎需要這個才能驅動有意義的遊戲
2. **定義 UI Description JSON contract** — Catenin 輸出給 renderer 的資料格式
3. **建立第一個 template: `card_game`** — 最有視覺衝擊力
4. **用 Claude 生成 React + PixiJS components** — 讓 AI 寫前端
5. **加入 Ktor WebSocket** — 推送遊戲狀態到瀏覽器
6. **迭代** — adventure template, board template, multiplayer

## 精神筆記

> "你不是缺少前端技能。你缺少的是引擎和 renderer 之間的 data contract。"
>
> 定義好 JSON contract，AI 就能幫你建前端。
> Catenin 已經是大腦 (brain)，現在要建臉 (face) — Cadherin。
> 生物學命名不只是好玩：catenin 連結創作與執行，cadherin 讓它被看見。

---

*研究由 5 個平行 Claude Code agent teams 執行，涵蓋 AI-gen UI, game engines, React approaches, config-driven UI, multiplayer architecture。*
