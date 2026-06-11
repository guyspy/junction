# 2026-06-11: Implementation Strategy Revision — Cadherin 技術選型複查

## 背景

距離 [2026-03-03 的 UI integration research](./2026-03-03-ui-integration-research-raw.md) 已過三個月。當時的研究指出生態系正在快速演進（A2UI 才剛在 2025-12 發布），因此在動工前重新驗證整個技術選型。

自上次研究後，repo 已新增 `protocol/` package（Hearthstone-inspired wire protocol：`GameEvent`, `EffectBlock`, `AnimationHint`, `GameOptions`, `GameRenderer`/`GameInput` interfaces，TDD 完成，commit a72c34c）。

## 研究方法

5 個平行研究 agent（live web search，所有版本號與專案狀態皆經 npm registry / GitHub / 官方 blog 驗證）：
1. React + PixiJS stack 各套件現況驗證
2. A2UI / SDUI / AI-generated UI 標準演進
3. Kotlin-native frontend 可行性（Compose Multiplatform Web / Kotlin Wasm）— 三月時未深查的新角度
4. Card game rendering 實作生態掃描
5. Multiplayer transport 複查（範圍鎖定 turn-based）

## 總體結論

**三月的核心架構全部通過驗證，不需要推翻。** React + PixiJS hybrid、template system、Hearthstone power-history model、plain WebSocket —— 全部成立，而且比三月時更有信心：

> 「client-owned component catalog + AI-generated constrained JSON + streaming render」這個 Junction 在三月独立設計出的模式，如今已成為業界共識架構 —— Google (A2UI v0.9)、Vercel (json-render, 15.1k stars)、OpenAI (Open-JSON-UI) 三家在 2026 上半年各自收斂到同一個答案。

修訂集中在邊緣：兩個套件要換掉/降級、一個新的發布管道（MCP Apps）、一次便宜的 Kotlin 升級、以及針對教室硬體現實的 protocol 強化。

---

## 各項發現

### 1. Stack 驗證結果（agent 1）

| 套件 | 2026-06 現況 | 判定 |
|------|-------------|------|
| React | 19.2.7（19.2 起 `<Activity />` 轉 stable；React Compiler 1.0；治理移交 React Foundation） | ✅ KEEP |
| PixiJS | 8.19.0（月更節奏；8.18 新增 experimental Canvas renderer + renderer fallback array；無 v9） | ✅ KEEP |
| @pixi/react | 8.0.5（2025-12 後無 release，repo 自 2026-01 靜默，memory-leak issue #648 無人回應） | ⚠️ KEEP + watch flag |
| @pixi/layout | 3.2.0（feature-stable，與 pixi 8.19 相容） | ✅ KEEP |
| Motion | 12.40.0（活躍，從 `motion/react` import） | ✅ KEEP |
| GSAP | 3.15.0（**3.13 起含全部 Club plugins 100% 免費**，Webflow 旗下持續維護） | ✅ KEEP，放心用 |
| dnd-kit | legacy core 凍結 18 個月；rewrite `@dnd-kit/react` 0.4/0.5-beta 仍在 API churn | ❌ 移出預設 stack |
| Tailwind | 4.3.0（無 breaking） | ✅ KEEP |
| shadcn/ui | CLI v4（2026-03）：**`shadcn/skills` 給 coding agent 用的 context** + design-system presets | ✅ KEEP，啟用 skills |

**重要修訂：**

- **Drag & drop 改用原生 Pixi pointer events。** dnd-kit / pragmatic-drag-and-drop 都是 DOM 導向；canvas 內的卡牌拖曳本來就該用 Pixi 的 `eventMode` + pointer events + 自訂 hit-testing（所有正經 card game 都這樣做）。只有「跨 DOM/canvas 邊界拖曳」才需要 DnD library —— 屆時選 **pragmatic-drag-and-drop 1.8.x**（Atlassian 維護中、headless、4.7kB），不選 pre-1.0 churn 中的 dnd-kit rewrite。
- **@pixi/react 要架構隔離。** 它是 thin react-reconciler bridge，官方但節奏放緩，且有未回應的 GPU memory-leak 報告（#648，與 `draw` prop 用法相關 —— 對長 session 遊戲是實際風險）。對策：component registry 內部把「React 元件」與「Pixi scene 操作」的接縫設計成可替換 —— 退路是 React 只管 DOM/HUD，canvas scene 由 animation queue 以 imperative pixi.js 驅動（許多遊戲團隊本來就跳過 reconciler）。先用 @pixi/react 起步（AI codegen 對 JSX 友善），但別讓它滲透進 template 元件的公開 API。
- **動畫分工明確化：** Motion 管 DOM/HUD/React 元件動畫；GSAP timeline 管 canvas 內 EffectBlock 播放編排（frame-accurate sequencing 是 GSAP 強項，且現在全免費）。**不要用 View Transitions API 做卡牌移動**（screenshot-crossfade、不可中斷、阻擋互動）—— 它只適合 screen-level 轉場（lobby → game）。
- **學校硬體韌性（新要求）：** PixiJS renderer 設 `preference: ['webgl', 'canvas']`（8.18 的 fallback array + experimental canvas renderer 正好為老 Chromebook 而生）；WebGPU 已是 Baseline（2026-01 全瀏覽器）但 PixiJS 官方仍建議 production 用 WebGL —— 之後一行 flag 切換，不是架構決策。初始 bundle 目標 < 1MB（code-split）。
- React 19.2 `<Activity />` 用於 screen 堆疊管理（背景保留遊戲畫面 state）；React Compiler 1.0 對 HUD 開啟（對 Pixi tree 無作用）。

### 2. UI Description JSON contract — 設計定調（agent 2）

**結論：bespoke contract、layout 層對齊 A2UI 慣例、以 MCP Apps 為發布目標。不整包採用任何標準。**

contract 拆成兩層：

1. **Scene/layout 層 —— 對齊 A2UI 慣例，但不依賴它。** Flat adjacency-list component nodes、client 宣告的 component catalog（`Hand`, `Board`, `Card`, `DialogueBox` 作為 custom catalog —— A2UI 明確支援此模式）、declarative data binding、streaming create-then-update。借用這些 shape 零成本，讓未來的 A2UI bridge 或 json-render renderer 只是 thin adapter。**現在不直接採用的理由：** A2UI 仍 pre-1.0（0.8→0.9 曾大改格式）、單一廠商治理（未像 A2A/MCP 進中立基金會）、且 Junction 自己控制 renderer 兩端，互通性現階段買不到東西。
2. **Game event stream 層 —— 完全 bespoke，Hearthstone pattern。** 這正是 `protocol/` package 已實作的方向，研究再次驗證：ordered semantic events、nested blocks 表達因果（BLOCK_START/BLOCK_END）、**非狀態變更的 animation/targeting hints（META_DATA 等價物 —— 即 `AnimationHint`）**、hidden information 的 show/hide 語義。沒有任何標準涵蓋這層 —— production 遊戲與 production SDUI（Airbnb Ghost Platform、Instagram Bloks）都把動畫編排放在 client 端、由語義事件驅動。

**A2UI 現況：** v0.9.1（2026-04-17 v0.9 大改版），v1.0 RC 目標 2026 Q4 + renderer 認證計畫。15.3k stars、官方 React renderer 已出、**Kotlin Agent SDK 宣布 coming soon**（repo 已有 5.7% Kotlin）。已採用：Gemini Enterprise、Flutter GenUI、AG2、Vercel json-render。

**追蹤清單：**
- **A2UI v1.0 RC（2026 Q4）** —— 屆時重新評估，特別是 Kotlin SDK 落地後（Catenin 可原生輸出 spec-compliant payload）
- **Vercel json-render**（Apache 2.0）—— catalog-constrained generative UI 的參考實作，命名/shape 值得借
- AG-UI / CopilotKit（$27M A 輪，AWS/Oracle 採用）—— 與 game contract 正交；只在未來做「educator 聊天改遊戲」copilot 時作為 transport

### 3. 🆕 MCP Apps 作為發布管道（agent 2 — 本次最大戰略新增）

**MCP Apps 於 2026-01-26 成為第一個官方 MCP extension**（Anthropic + OpenAI 共同開發，合併 MCP-UI 與 OpenAI Apps SDK 血統），由 Linux Foundation 的 Agentic AI Foundation 中立治理。Client 支援：ChatGPT、Claude / Claude Desktop、VS Code Copilot、Goose 等。

它**不是** declarative JSON UI 標準 —— 是 sandboxed iframe 裡的 HTML 頁 + postMessage JSON-RPC。所以它不跟 UI Description contract 競爭，而是**封裝/發布管道**：把 Junction renderer（React + Pixi canvas + component registry，吃自己的 JSON）打包成 MCP App，遊戲就能**直接在 Claude / ChatGPT 對話裡玩**，內部 contract 零修改（ext-apps repo 已有 Three.js/CesiumJS 範例證明 rich canvas app 可行）。

對 Junction 的「AI agent 在對話中創建/修改遊戲」願景，這是現成的最後一哩路：educator 在 chat 裡請 AI 生成 YAML → engine 跑起來 → 遊戲直接在同一個對話裡可玩。**Phase 3（full generative）的 demo 路徑突然變得非常短。**

### 4. Kotlin-native frontend 評估（agent 3）

**結論：React 仍是對的，Compose Web 還沒準備好 —— 但 Kotlin 2.3.x 的 interop 紅利要立刻收割。**

Compose Multiplatform for Web 不採用的理由（2026-06 證據）：
- 仍是 **Beta**（2025-09 至今），無 stable 時程；2026-05 的 CMP 1.11 還在重做 web touch/scroll 處理
- **Kotlin/Wasm 需要 Safari 18.2+**（2024-12）—— 老 iPad 直接出局；教室硬體是最壞情境受眾
- Bundle 3–6MB（vs React+Pixi code-split < 1MB）；accessibility 僅「initial」；canvas 渲染無 DOM 可給 agent inspect/fix loop
- Kotlin 2D 遊戲生態脆弱：KorGE 創作者 2025-04 離開、現靠單一志工維護；Kubriko v0.2 web 還在 alpha
- AI codegen 對 React 的優勢未翻轉（Compose 社群還在出「防 AI 幻覺」的 agent skills 補丁）

**立刻收割的 interop 紅利（Catenin 現於 Kotlin 2.3.10 → 升 2.3.20+，小版本升級）：**

| 功能 | 旗標/API | 對 Junction 的意義 |
|------|---------|-------------------|
| `suspend` function 直接 `@JsExport` | `-Xenable-suspend-function-exporting`（2.3.0, experimental） | engine API 不用再手寫 Promise wrapper |
| `@JsExport.Default`（stable, 2.3.0） | export default | React.lazy / ES module 整合乾淨 |
| **TS class 實作 Kotlin interface**（2.3.20） | `-Xenable-implementing-interfaces-from-typescript` + `generateTypeScriptDefinitions()` | **Cadherin (TS) 可直接實作 `protocol/` 的 `GameRenderer`/`GameInput` interface，型別檢查由 compiler 把關** —— 引擎↔渲染層的 contract 從文件變成編譯期保證 |

這把 Compose 承諾的「one language, shared types」紅利收掉大半，同時保住 React 的 AI 槓桿。再評估時機：CMP-web 宣布 Stable（社群預期 Kotlin/Wasm 2026 底）—— 屆時先從次要介面（educator dashboard）試點，永遠不從 game renderer 開始。

順帶：JetBrains–Anthropic 合作深化（Claude 原生進 IntelliJ、Kotlin SWE-bench 86.4%、官方 Kotlin MCP SDK）—— 強化的是現有架構（Kotlin engine + AI 寫 TS UI），不是 Compose bet。

### 5. Card game 生態掃描（agent 4）

**市場空隙仍開著。** 競品分兩類：(a) 固定 quiz 模板 + AI 填內容（Kahoot AI、Blooket、Quizizz→Wayground）；(b) 自由 AI code-gen 無保證（Websim、Rosebud AI、GDevelop AI agent）。**沒有人做「schema-validated 定義 + deterministic engine + auto-rendering templates」。** 2025 年 Websim 課堂研究報告的缺口（無 analytics、無 assessment 整合、無 QA）正是 schema-driven engine 修掉的東西。Blooket 的成功（固定 game-mode templates + 可換題庫內容）直接驗證 template system 的商業模式 —— Junction 把「quiz 內容」推廣到「YAML 任意機制」。

**護城河提醒：YAML validation error 要做成 first-class —— 快速、友善、AI 可自動修。** 這是對抗「自由 code-gen 夠好就行」路線的核心差異。

（Google Genie 3 / Project Genie：prompt 生成 3D 互動世界，但 ~60s session、不可重現、無法 deterministic 編輯 —— 教育需要可重現性與可評量性，近期不構成威脅。）

**動工前必讀的三個參考實作：**

1. **[delucis/bgio-effects](https://github.com/delucis/bgio-effects)** —— animation queue 的現成 API 藍圖：game code *宣告* effects（「別讓 client diff 兩個 state 去猜發生了什麼」—— 正是 EffectBlock 哲學）、ordered queue、React hooks（`useEffectListener`、`useEffectQueue` 含 `clear()`/`flush()`）、per-effect duration、rAF 驅動。**這就是 Cadherin 消費 EffectBlock→AnimationHint 的核心難題，已經被 API-design 過一次。**
2. **[TheCardGoat/tcg-engines](https://github.com/TheCardGoat/tcg-engines)** —— 活著的姊妹架構：Zod-validated declarative card defs + typed engine + UI component kit（@tcg/core-ui）+ per-game packages（已實作 Lorcana、Gundam）。定 Cadherin component-registration API 前先看它的 package boundaries。
3. **[swen128/balatro](https://github.com/swen128/balatro)** + **[pixijs/open-games](https://github.com/pixijs/open-games)** —— 完整可玩的 TS+React 卡牌遊戲（手牌管理、出牌序列、計分連鎖）+ Pixi 官方遊戲技法。

其他結論：boardgame.io 凍結（npm 停在 ~2021）—— 別依賴，只挖 pattern；hand-of-cards fan 數學自己寫（~100 行，所有正經專案都 bespoke）；Hearthstone protocol 規格書級參考 = [fireplace wiki](https://github.com/jleclanche/fireplace/wiki/Understanding-the-Hearthstone-Protocol)（注意：resolution queue 一旦開始 drain 即不可變 —— client queue 值得照抄這條規則）；XCOM 2 的 visualization-block pattern 適合做 AnimationHint → 可組合視覺原語的映射。

### 6. Transport 複查（agent 5）

**WebSocket 維持原判，但 protocol 要做兩個便宜的強化。**

- WebTransport 2026-03 成為 Baseline（Safari 26.4），**但學校網路依政策封鎖 QUIC/UDP:443**（Zscaler/Fortinet/Forcepoint 標準作法，逼流量回可檢測的 TCP），且瀏覽器從未實作 TCP fallback —— 封了就得自己寫 WebSocket fallback，等於白繞。turn-based 遊戲對 WebTransport 的賣點（多 stream、unreliable datagrams）毫無需求。**WSS over TCP 443 是教室環境最可靠的選擇。**
- **Server 選 Ktor**（3.5.x）：`KotlinxWebsocketSerializationConverter(Json)` 直接收發 `commonMain` 的 `@Serializable` sealed classes —— solo Kotlin dev 零重複。Quarkus WebSockets Next 穩定但 codec 是 Vert.x/Jackson 系，要共用 kotlinx.serialization models 得自寫 codec。kotlinx-rpc 仍 pre-1.0，只追蹤不採用。
- **🆕 強化一：事件編號 + resume-from-sequence。** 每個 GameEvent 帶遞增 sequence number，client 可從任意序號續傳 —— 這不是 nice-to-have，是便宜 Chromebook 掉 Wi-Fi 的日常。SSE 的 `Last-Event-ID` 語義是好範本。
- **🆕 強化二：transport-agnostic 設計 + SSE 逃生門。** power-history model 本來就是「單向 ordered event stream + 離散 action 提交」，天生適配 SSE-down + POST-up。MCP 自己在 2025 把 SSE transport 換成 Streamable HTTP 正是為了 proxy 友善。設計時不把 transport 假設烙進 protocol，教室 pilot 撞到會嚼 WebSocket upgrade 的 proxy 時，fallback 幾乎免費（Ktor server/client 都內建 SSE）。
- **Wire 格式現在就定（早定便宜、晚定痛苦）：** kotlinx.serialization 設短的 `classDiscriminator` + 每個 subclass 穩定 `@SerialName` → 輸出即 TS discriminated union 的形狀。TS 端型別先手寫（protocol 還小）；KxsTsGen 已休眠（2024-01 後無 release），不依賴；protocol 長大後再上 OpenAPI `oneOf`+`discriminator` codegen。

---

## 修訂後的技術 Stack

| Layer | 三月決策 | 六月修訂 | 變更 |
|-------|---------|---------|------|
| Game Engine | Catenin (Kotlin MP) | Catenin，**升 Kotlin 2.3.20** + 開 d.ts/suspend-export/TS-implements-Kotlin | 🔄 升級收互operability 紅利 |
| UI Framework | React 19 | React 19.2.x + `<Activity />` + Compiler（HUD） | ✅ 不變 |
| Game Canvas | PixiJS v8 + @pixi/react | PixiJS 8.19 + @pixi/react 8.0.5（**接縫可替換**，退路 = imperative Pixi + React HUD） | ⚠️ 降級為可換件 |
| Renderer 韌性 | （未提） | `preference: ['webgl', 'canvas']`；WebGPU 後切 | 🆕 教室硬體 |
| Animations | Motion + GSAP | Motion（DOM）/ GSAP 3.15 timelines（canvas 編排，**全免費**）；View Transitions 只做 screen 轉場 | 🔄 分工明確化 |
| Drag & Drop | dnd-kit | **canvas 內 = 原生 Pixi pointer events**；DOM↔canvas 才用 pragmatic-drag-and-drop | ❌→🔄 換掉 |
| Styling | Tailwind + shadcn | Tailwind 4.3 + shadcn CLI v4（**開 `shadcn/skills` 餵 coding agent**） | ✅ + agent 工具 |
| UI Contract | 「A2UI-style JSON」 | **兩層 bespoke contract**：A2UI-aligned catalog/layout 層 + Hearthstone-pattern event 層 | 🔄 設計定調 |
| 發布管道 | （未提） | **MCP Apps 打包 renderer → 遊戲在 Claude/ChatGPT 內可玩** | 🆕 戰略新增 |
| WebSocket | Ktor or Quarkus | **Ktor 3.5**（kotlinx.serialization 原生） | 🔄 二選一定案 |
| Wire Protocol | JSON → Protobuf 之後 | JSON + `classDiscriminator`/`@SerialName` 慣例 + **sequence number/resume** | 🔄 強化 |
| State Sync | WorldUpdate event stream | 不變，加 per-player view filtering + show/hide 語義 | ✅ 驗證通過 |

## 修訂後的下一步行動

1. **完成 Phase 2 trigger system**（不變 —— 引擎先能跑有意義的遊戲）
2. **Kotlin 2.3.10 → 2.3.20** + 開 `generateTypeScriptDefinitions()` / suspend export / TS-implements-Kotlin-interfaces —— 讓 `protocol/GameRenderer` 成為編譯期 contract（半天工作量，先做）
3. **給 `GameEvent` 加 sequence number + resume 語義**（protocol 還小，現在改零痛）
4. **研讀三個參考實作**（bgio-effects 的 queue API、tcg-engines 的 registry boundaries、balatro 的卡牌手感）→ 定 Cadherin component registry API
5. **建第一個 template: `card_game`**（不變）—— React HUD + Pixi canvas，動畫 queue 照 bgio-effects shape 設計
6. **Ktor WebSocket layer**（定案 Ktor，含 SSE 逃生門的 transport-agnostic 設計）
7. **MCP Apps 打包 spike** —— 把 demo renderer 包進 iframe + postMessage，驗證「在 Claude 對話裡玩 Junction 遊戲」（Phase 3 的最短 demo 路徑）
8. 迭代：board/adventure/quiz templates、per-player filtering、multiplayer

## Watch List（下次複查觸發點）

| 事件 | 預期時間 | 行動 |
|------|---------|------|
| A2UI v1.0 RC + Kotlin SDK | 2026 Q4 | 重評 scene/layout 層直接採用 A2UI |
| @pixi/react 恢復維護 or issue #648 惡化 | — | 確認/觸發 imperative Pixi 退路 |
| Compose Multiplatform Web → Stable | 社群估 2026 底+ | 從 educator dashboard 試點（永不從 renderer 開始） |
| dnd-kit rewrite 達 1.0 | — | 重評 DOM 拖曳選項 |
| Kotlin/Wasm → Stable | 2026 底（未承諾） | 連動 CMP-web 評估 |

## 精神筆記

> 三個月前的研究說「方向是對的」。這次的研究說「全世界正在收斂到你的方向」。
>
> A2UI、json-render、Open-JSON-UI —— 三大陣營 2026 上半年各自做出了 Junction 三月就畫在白板上的架構。這不是巧合，是這條路本來就通。
> 而 MCP Apps 把終局提前了：educator 在對話裡說一句話，遊戲在同一個對話裡跑起來。
> 大腦（Catenin）已經會說 Hearthstone 的語言；臉（Cadherin）的五官圖紙現在齊了。

---

*研究由 5 個平行 Claude Code agent 執行（2026-06-11），所有版本號與專案狀態經 live web search 驗證；來源連結見各 agent 報告原文。*
