# Day 2: 玩家動作與遊戲狀態

## 工作目標
- 加入玩家概念和遊戲狀態管理
- 實作基本玩家動作（抽牌、打牌）
- 建立簡單的遊戲循環
- 擴展 YAML 格式支援遊戲設置

## 今日範圍

### 擴展 YAML 格式
```yaml
# game2.yaml - 加入玩家設置
meta:
  name: "玩家互動遊戲"
  target_age: [6, 10]
  player_count: [2, 4]

cards:
  number_card:
    count: 20
    properties:
      value: {type: int, min: 1, max: 10}
      color: {type: enum, values: [red, blue, green]}

mechanics:
  setup:
    deck: {shuffle: true}
    players:
      hand_size: 5
      initial_score: 0
  
  # 今日暫時簡化，只支援基本動作
  actions:
    - draw_card
    - play_card
```

### 遊戲狀態模型
```kotlin
// 遊戲狀態管理
data class GameState(
    val gameId: String,
    val definition: GameDefinition,
    val players: List<Player>,
    val deck: MutableList<Card>,
    val discardPile: MutableList<Card>,
    val currentPlayer: Int,
    val gamePhase: GamePhase
) {
    fun getCurrentPlayer(): Player = players[currentPlayer]
    fun getPlayer(playerId: String): Player? = players.find { it.id == playerId }
}

data class Player(
    val id: String,
    val name: String,
    val hand: MutableList<Card>,
    val score: Int
) {
    fun hasCards(): Boolean = hand.isNotEmpty()
    fun canDraw(): Boolean = hand.size < 10 // 暫時限制
}

enum class GamePhase {
    SETUP, PLAYING, FINISHED
}

// 玩家動作定義
sealed class PlayerAction {
    abstract val playerId: String
    
    data class DrawCard(override val playerId: String) : PlayerAction()
    data class PlayCard(override val playerId: String, val cardId: String) : PlayerAction()
}

sealed class ActionResult {
    object Success : ActionResult()
    data class Failure(val reason: String) : ActionResult()
}
```

### 遊戲引擎核心
```kotlin
class SimpleGameEngine {
    
    fun createGame(definition: GameDefinition, playerNames: List<String>): GameState {
        require(playerNames.size >= 2) { "至少需要2位玩家" }
        
        // 生成所有卡牌
        val cardGenerator = CardGenerator()
        val allCards = cardGenerator.generateCards(definition).toMutableList()
        
        // 洗牌
        allCards.shuffle()
        
        // 創建玩家
        val players = playerNames.mapIndexed { index, name ->
            Player(
                id = "player_$index",
                name = name,
                hand = mutableListOf(),
                score = 0
            )
        }
        
        // 發初始手牌
        val handSize = getHandSize(definition)
        players.forEach { player ->
            repeat(handSize) {
                if (allCards.isNotEmpty()) {
                    player.hand.add(allCards.removeFirst())
                }
            }
        }
        
        return GameState(
            gameId = "game_${System.currentTimeMillis()}",
            definition = definition,
            players = players,
            deck = allCards,
            discardPile = mutableListOf(),
            currentPlayer = 0,
            gamePhase = GamePhase.PLAYING
        )
    }
    
    fun processAction(gameState: GameState, action: PlayerAction): ActionResult {
        return when (action) {
            is PlayerAction.DrawCard -> processDrawCard(gameState, action)
            is PlayerAction.PlayCard -> processPlayCard(gameState, action)
        }
    }
    
    private fun processDrawCard(gameState: GameState, action: PlayerAction.DrawCard): ActionResult {
        val player = gameState.getPlayer(action.playerId)
            ?: return ActionResult.Failure("玩家不存在")
        
        if (!player.canDraw()) {
            return ActionResult.Failure("手牌已滿，無法抽牌")
        }
        
        if (gameState.deck.isEmpty()) {
            return ActionResult.Failure("牌庫已空")
        }
        
        val drawnCard = gameState.deck.removeFirst()
        player.hand.add(drawnCard)
        
        return ActionResult.Success
    }
    
    private fun processPlayCard(gameState: GameState, action: PlayerAction.PlayCard): ActionResult {
        val player = gameState.getPlayer(action.playerId)
            ?: return ActionResult.Failure("玩家不存在")
        
        val cardIndex = player.hand.indexOfFirst { it.id == action.cardId }
        if (cardIndex == -1) {
            return ActionResult.Failure("手牌中沒有此卡牌")
        }
        
        val playedCard = player.hand.removeAt(cardIndex)
        gameState.discardPile.add(playedCard)
        
        return ActionResult.Success
    }
    
    private fun getHandSize(definition: GameDefinition): Int {
        // 從 YAML 中讀取，或使用預設值
        return 5 // 暫時硬編碼
    }
}
```

### 遊戲顯示系統升級
```kotlin
class GameDisplaySystem {
    
    fun displayGameState(gameState: GameState) {
        println("=== ${gameState.definition.meta.name} ===")
        println("遊戲 ID: ${gameState.gameId}")
        println("目前階段: ${gameState.gamePhase}")
        println("牌庫剩餘: ${gameState.deck.size} 張")
        println("棄牌堆: ${gameState.discardPile.size} 張")
        println()
        
        displayPlayers(gameState)
    }
    
    private fun displayPlayers(gameState: GameState) {
        gameState.players.forEachIndexed { index, player ->
            val isCurrent = index == gameState.currentPlayer
            val marker = if (isCurrent) "👉" else "  "
            
            println("$marker 玩家 ${player.name} (${player.id})")
            println("   手牌: ${player.hand.size} 張")
            println("   分數: ${player.score}")
            
            if (isCurrent) {
                displayPlayerHand(player)
            }
            println()
        }
    }
    
    private fun displayPlayerHand(player: Player) {
        println("   手牌詳情:")
        player.hand.forEach { card ->
            val value = card.properties["value"]
            val color = card.properties["color"]
            println("     - ${card.id}: ${color}色數字${value}")
        }
    }
    
    fun displayActionResult(action: PlayerAction, result: ActionResult) {
        when (result) {
            is ActionResult.Success -> {
                when (action) {
                    is PlayerAction.DrawCard -> 
                        println("✅ ${action.playerId} 成功抽牌")
                    is PlayerAction.PlayCard -> 
                        println("✅ ${action.playerId} 成功打出卡牌 ${action.cardId}")
                }
            }
            is ActionResult.Failure -> {
                println("❌ 動作失敗: ${result.reason}")
            }
        }
    }
}
```

## 互動式遊戲循環
```kotlin
class InteractiveGameLoop {
    private val display = GameDisplaySystem()
    private val engine = SimpleGameEngine()
    private val scanner = Scanner(System.`in`)
    
    fun startGame(definition: GameDefinition) {
        println("=== 歡迎來到 ${definition.meta.name} ===")
        
        // 獲取玩家名稱
        val playerNames = getPlayerNames()
        
        // 創建遊戲
        val gameState = engine.createGame(definition, playerNames)
        
        // 遊戲主循環
        gameLoop(gameState)
    }
    
    private fun getPlayerNames(): List<String> {
        println("請輸入玩家數量 (2-4):")
        val playerCount = scanner.nextInt()
        scanner.nextLine() // 消耗換行符
        
        val names = mutableListOf<String>()
        repeat(playerCount) { index ->
            println("請輸入玩家 ${index + 1} 的名稱:")
            names.add(scanner.nextLine())
        }
        
        return names
    }
    
    private fun gameLoop(gameState: GameState) {
        while (gameState.gamePhase == GamePhase.PLAYING) {
            display.displayGameState(gameState)
            
            val currentPlayer = gameState.getCurrentPlayer()
            println("輪到 ${currentPlayer.name} 的回合")
            println("請選擇動作: 1) 抽牌  2) 打牌  3) 結束回合")
            
            when (scanner.nextLine()) {
                "1" -> handleDrawCard(gameState, currentPlayer)
                "2" -> handlePlayCard(gameState, currentPlayer)
                "3" -> nextPlayer(gameState)
                else -> println("無效選擇，請重新輸入")
            }
        }
    }
    
    private fun handleDrawCard(gameState: GameState, player: Player) {
        val action = PlayerAction.DrawCard(player.id)
        val result = engine.processAction(gameState, action)
        display.displayActionResult(action, result)
    }
    
    private fun handlePlayCard(gameState: GameState, player: Player) {
        if (player.hand.isEmpty()) {
            println("手牌為空，無法打牌")
            return
        }
        
        println("請選擇要打出的卡牌 ID:")
        player.hand.forEachIndexed { index, card ->
            println("$index) ${card.id}")
        }
        
        val choice = scanner.nextLine().toIntOrNull()
        if (choice != null && choice in 0 until player.hand.size) {
            val cardId = player.hand[choice].id
            val action = PlayerAction.PlayCard(player.id, cardId)
            val result = engine.processAction(gameState, action)
            display.displayActionResult(action, result)
        } else {
            println("無效選擇")
        }
    }
    
    private fun nextPlayer(gameState: GameState) {
        gameState.apply {
            currentPlayer = (currentPlayer + 1) % players.size
        }
        println("回合結束，下一位玩家")
    }
}
```

## 測試策略

### 單元測試
```kotlin
class SimpleGameEngineTest {
    
    @Test
    fun `should create game with correct initial state`() {
        val definition = createTestGameDefinition()
        val engine = SimpleGameEngine()
        
        val gameState = engine.createGame(definition, listOf("Alice", "Bob"))
        
        assertThat(gameState.players).hasSize(2)
        assertThat(gameState.players[0].name).isEqualTo("Alice")
        assertThat(gameState.players[1].name).isEqualTo("Bob")
        assertThat(gameState.gamePhase).isEqualTo(GamePhase.PLAYING)
        
        // 每位玩家應該有5張手牌
        gameState.players.forEach { player ->
            assertThat(player.hand).hasSize(5)
            assertThat(player.score).isEqualTo(0)
        }
    }
    
    @Test
    fun `should process draw card action correctly`() {
        val gameState = createTestGameState()
        val engine = SimpleGameEngine()
        val initialHandSize = gameState.players[0].hand.size
        val initialDeckSize = gameState.deck.size
        
        val action = PlayerAction.DrawCard("player_0")
        val result = engine.processAction(gameState, action)
        
        assertThat(result).isEqualTo(ActionResult.Success)
        assertThat(gameState.players[0].hand).hasSize(initialHandSize + 1)
        assertThat(gameState.deck).hasSize(initialDeckSize - 1)
    }
    
    @Test
    fun `should process play card action correctly`() {
        val gameState = createTestGameState()
        val engine = SimpleGameEngine()
        val player = gameState.players[0]
        val cardToPlay = player.hand.first()
        val initialDiscardSize = gameState.discardPile.size
        
        val action = PlayerAction.PlayCard(player.id, cardToPlay.id)
        val result = engine.processAction(gameState, action)
        
        assertThat(result).isEqualTo(ActionResult.Success)
        assertThat(player.hand).doesNotContain(cardToPlay)
        assertThat(gameState.discardPile).hasSize(initialDiscardSize + 1)
        assertThat(gameState.discardPile.last()).isEqualTo(cardToPlay)
    }
}
```

### 整合測試
```kotlin
class Day2IntegrationTest {
    
    @Test
    fun `complete day 2 workflow`() {
        // 1. 準備測試遊戲定義
        val definition = loadTestGameDefinition()
        
        // 2. 創建遊戲
        val engine = SimpleGameEngine()
        val gameState = engine.createGame(definition, listOf("測試玩家1", "測試玩家2"))
        
        // 3. 模擬一些玩家動作
        val display = GameDisplaySystem()
        
        // 玩家1抽牌
        val drawResult = engine.processAction(
            gameState, 
            PlayerAction.DrawCard("player_0")
        )
        assertThat(drawResult).isEqualTo(ActionResult.Success)
        
        // 玩家1打牌
        val firstCard = gameState.players[0].hand.first()
        val playResult = engine.processAction(
            gameState,
            PlayerAction.PlayCard("player_0", firstCard.id)
        )
        assertThat(playResult).isEqualTo(ActionResult.Success)
        
        // 顯示遊戲狀態
        display.displayGameState(gameState)
        
        println("✅ Day 2 功能驗證成功！")
    }
}
```

## 執行範例

### 測試 YAML 檔案
```yaml
# day2_test.yaml
meta:
  name: "Day 2 測試遊戲"
  target_age: [8, 12]
  player_count: [2, 4]

cards:
  number_card:
    count: 16
    properties:
      value: {type: int, min: 1, max: 8}
      color: {type: enum, values: [red, blue, green, yellow]}

mechanics:
  setup:
    deck: {shuffle: true}
    players:
      hand_size: 5
      initial_score: 0
  
  actions:
    - draw_card
    - play_card
```

### 主程式
```kotlin
fun main() {
    println("=== Cadherin DSL Day 2 Demo ===")
    
    try {
        // 解析遊戲定義
        val parser = GameDefinitionParser()
        val definition = parser.parseFromFile("day2_test.yaml")
        
        // 啟動互動式遊戲
        val gameLoop = InteractiveGameLoop()
        gameLoop.startGame(definition)
        
    } catch (e: Exception) {
        println("❌ 執行失敗: ${e.message}")
        e.printStackTrace()
    }
}
```

## 今日交付成果
- [ ] ✅ 遊戲狀態管理系統
- [ ] ✅ 玩家資料模型
- [ ] ✅ 基本玩家動作（抽牌、打牌）
- [ ] ✅ 簡單遊戲循環
- [ ] ✅ 互動式遊戲介面
- [ ] ✅ 擴展的 YAML 格式支援
- [ ] ✅ 完整測試覆蓋

## 明日工作預告
Day 3 將加入回合管理和基本計分機制，讓遊戲有明確的勝負判定。

## 技術債務記錄
- 回合切換邏輯較簡單（明日完善）
- 手牌上限硬編碼（後續從 YAML 讀取）
- 缺少動作合法性深度驗證（逐步加強）

今日成功建立了玩家互動的基礎，遊戲開始有了真正的「玩法」！