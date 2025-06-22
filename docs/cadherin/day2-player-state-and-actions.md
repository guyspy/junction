# Day 2: 玩家狀態與基本動作

## 工作目標
- 實作遊戲狀態管理系統
- 加入玩家動作處理（抽牌、打牌）
- 建立命令列互動介面
- 讓遊戲真正可以「玩」

## 今日範圍

### 擴展遊戲狀態模型

```kotlin
// commonMain/model/GameState.kt
package org.junction.cadherin.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val gameId: String,
    val definition: GameDefinition,
    val players: List<Player>,
    val deck: MutableList<Card>,
    val discardPile: MutableList<Card> = mutableListOf(),
    val currentPlayerIndex: Int = 0,
    val gamePhase: GamePhase = GamePhase.PLAYING,
    val turnNumber: Int = 1
) {
    fun getCurrentPlayer(): Player = players[currentPlayerIndex]
    
    fun getPlayer(playerId: String): Player? = players.find { it.id == playerId }
    
    fun nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }
    
    fun isGameOver(): Boolean = gamePhase == GamePhase.FINISHED
}

@Serializable
enum class GamePhase {
    SETUP,
    PLAYING, 
    FINISHED
}
```

### 玩家動作系統

```kotlin
// commonMain/actions/PlayerAction.kt
package org.junction.cadherin.actions

import kotlinx.serialization.Serializable

@Serializable
sealed class PlayerAction {
    abstract val playerId: String
    
    @Serializable
    data class DrawCard(override val playerId: String) : PlayerAction()
    
    @Serializable
    data class PlayCard(
        override val playerId: String, 
        val cardId: String
    ) : PlayerAction()
    
    @Serializable
    data class EndTurn(override val playerId: String) : PlayerAction()
}

@Serializable
data class ActionResult(
    val success: Boolean,
    val message: String,
    val effects: List<GameEffect> = emptyList()
) {
    companion object {
        fun success(message: String = "動作成功") = ActionResult(true, message)
        fun failure(message: String) = ActionResult(false, message)
        fun withEffects(message: String, effects: List<GameEffect>) = 
            ActionResult(true, message, effects)
    }
}

@Serializable
data class GameEffect(
    val type: String,
    val target: String,
    val amount: Int? = null,
    val description: String
)
```

### 升級 GameEngine - 動作處理

```kotlin
// commonMain/core/GameEngine.kt
package org.junction.cadherin.core

import org.junction.cadherin.model.*
import org.junction.cadherin.actions.*
import org.junction.cadherin.parser.GameDefinitionParser

@JsExport
class GameEngine private constructor(
    private var gameState: GameState
) {
    companion object {
        fun fromYaml(yamlContent: String, playerNames: List<String>): GameEngine {
            val parser = GameDefinitionParser()
            val definition = parser.parseFromString(yamlContent)
            
            val gameState = initializeGame(definition, playerNames)
            return GameEngine(gameState)
        }
        
        private fun initializeGame(definition: GameDefinition, playerNames: List<String>): GameState {
            // 生成所有卡牌
            val cardFactory = CardFactory(definition)
            val allCards = cardFactory.generateCards().toMutableList()
            allCards.shuffle()
            
            // 創建玩家
            val defaultHealth = definition.mechanics?.setup?.players?.health ?: 10
            val handSize = definition.mechanics?.setup?.players?.handSize ?: 5
            
            val players = playerNames.mapIndexed { index, name ->
                Player(
                    id = "player_$index",
                    name = name,
                    health = defaultHealth
                )
            }
            
            // 發初始手牌
            players.forEach { player ->
                repeat(handSize) {
                    if (allCards.isNotEmpty()) {
                        player.addCard(allCards.removeFirst())
                    }
                }
            }
            
            return GameState(
                gameId = "game_${System.currentTimeMillis()}",
                definition = definition,
                players = players,
                deck = allCards
            )
        }
    }
    
    @JsName("processAction")
    fun processAction(action: PlayerAction): ActionResult {
        return when (action) {
            is PlayerAction.DrawCard -> processDrawCard(action)
            is PlayerAction.PlayCard -> processPlayCard(action)
            is PlayerAction.EndTurn -> processEndTurn(action)
        }
    }
    
    private fun processDrawCard(action: PlayerAction.DrawCard): ActionResult {
        val player = gameState.getPlayer(action.playerId)
            ?: return ActionResult.failure("玩家不存在")
        
        if (gameState.deck.isEmpty()) {
            return ActionResult.failure("牌庫已空，無法抽牌")
        }
        
        if (player.hand.size >= 10) { // 暫時限制手牌上限
            return ActionResult.failure("手牌已滿")
        }
        
        val drawnCard = gameState.deck.removeFirst()
        player.addCard(drawnCard)
        
        return ActionResult.success("${player.name} 抽了一張牌")
    }
    
    private fun processPlayCard(action: PlayerAction.PlayCard): ActionResult {
        val player = gameState.getPlayer(action.playerId)
            ?: return ActionResult.failure("玩家不存在")
        
        val card = player.removeCard(action.cardId)
            ?: return ActionResult.failure("手牌中沒有此卡牌")
        
        // 將卡牌放入棄牌堆
        gameState.discardPile.add(card)
        
        // 今日先簡化：打牌就是丟棄，明日加入事件系統
        return ActionResult.success("${player.name} 打出了 ${card.id}")
    }
    
    private fun processEndTurn(action: PlayerAction.EndTurn): ActionResult {
        gameState.nextPlayer()
        gameState.turnNumber++
        
        val nextPlayer = gameState.getCurrentPlayer()
        return ActionResult.success("輪到 ${nextPlayer.name} 的回合")
    }
    
    @JsName("getGameState")
    fun getGameState(): GameState = gameState
    
    @JsName("getUIState") 
    fun getUIState(): UIState {
        return UIState(
            players = gameState.players,
            currentPlayerId = gameState.getCurrentPlayer().id,
            gamePhase = gameState.gamePhase.name,
            turnNumber = gameState.turnNumber,
            deckSize = gameState.deck.size,
            discardPileSize = gameState.discardPile.size
        )
    }
}

@JsExport
data class UIState(
    val players: List<Player>,
    val currentPlayerId: String,
    val gamePhase: String,
    val turnNumber: Int,
    val deckSize: Int,
    val discardPileSize: Int
)
```

### 命令列介面系統

```kotlin
// jvmMain/cli/GameCLI.kt
package org.junction.cadherin.cli

import org.junction.cadherin.core.GameEngine
import org.junction.cadherin.actions.PlayerAction
import org.junction.cadherin.model.Card
import org.junction.cadherin.model.Player

class GameCLI {
    private lateinit var gameEngine: GameEngine
    
    fun startGame(yamlContent: String, playerNames: List<String>) {
        println("=== 歡迎來到 Cadherin 卡牌遊戲 ===")
        
        try {
            gameEngine = GameEngine.fromYaml(yamlContent, playerNames)
            val gameState = gameEngine.getGameState()
            
            println("遊戲：${gameState.definition.meta.name}")
            println("玩家：${playerNames.joinToString(", ")}")
            println()
            
            gameLoop()
            
        } catch (e: Exception) {
            println("❌ 遊戲啟動失敗：${e.message}")
        }
    }
    
    private fun gameLoop() {
        while (!gameEngine.getGameState().isGameOver()) {
            displayGameState()
            handlePlayerTurn()
        }
    }
    
    private fun displayGameState() {
        val uiState = gameEngine.getUIState()
        val gameState = gameEngine.getGameState()
        
        println("=".repeat(50))
        println("回合 ${uiState.turnNumber}")
        println("牌庫剩餘：${uiState.deckSize} 張")
        println("棄牌堆：${uiState.discardPileSize} 張")
        println()
        
        // 顯示所有玩家狀態
        gameState.players.forEach { player ->
            val isCurrent = player.id == uiState.currentPlayerId
            val marker = if (isCurrent) "👉 " else "   "
            
            println("$marker${player.name} (生命值: ${player.health}, 手牌: ${player.hand.size})")
            
            if (isCurrent) {
                displayPlayerHand(player)
            }
        }
        println()
    }
    
    private fun displayPlayerHand(player: Player) {
        if (player.hand.isEmpty()) {
            println("     手牌：無")
            return
        }
        
        println("     手牌：")
        player.hand.forEachIndexed { index, card ->
            val damage = card.getIntProperty("damage")
            val element = card.getStringProperty("element")
            val healing = card.getIntProperty("healing")
            
            val description = when {
                damage != null && element != null -> "$element 攻擊 ($damage 傷害)"
                healing != null -> "治療 ($healing 生命值)"
                else -> {
                    val value = card.getIntProperty("value")
                    val color = card.getStringProperty("color")
                    "$color 數字 $value"
                }
            }
            
            println("       $index) ${card.id}: $description")
        }
    }
    
    private fun handlePlayerTurn() {
        val currentPlayer = gameEngine.getGameState().getCurrentPlayer()
        println("輪到 ${currentPlayer.name} 的回合")
        println("請選擇動作：")
        println("1) 抽牌")
        println("2) 打牌")
        println("3) 結束回合")
        print("請輸入選擇 (1-3): ")
        
        when (readLine()) {
            "1" -> handleDrawCard(currentPlayer)
            "2" -> handlePlayCard(currentPlayer)
            "3" -> handleEndTurn(currentPlayer)
            else -> {
                println("無效選擇，請重新輸入")
                handlePlayerTurn()
            }
        }
    }
    
    private fun handleDrawCard(player: Player) {
        val action = PlayerAction.DrawCard(player.id)
        val result = gameEngine.processAction(action)
        
        println(if (result.success) "✅ ${result.message}" else "❌ ${result.message}")
        
        if (result.success) {
            // 顯示抽到的牌
            val lastCard = player.hand.lastOrNull()
            if (lastCard != null) {
                println("   抽到：${lastCard.id}")
            }
        }
        
        // 抽牌後繼續當前玩家回合
        if (result.success) {
            handlePlayerTurn()
        }
    }
    
    private fun handlePlayCard(player: Player) {
        if (player.hand.isEmpty()) {
            println("手牌為空，無法打牌")
            handlePlayerTurn()
            return
        }
        
        print("請選擇要打出的卡牌編號 (0-${player.hand.size - 1}): ")
        val input = readLine()
        val cardIndex = input?.toIntOrNull()
        
        if (cardIndex == null || cardIndex !in 0 until player.hand.size) {
            println("無效選擇")
            handlePlayerTurn()
            return
        }
        
        val card = player.hand[cardIndex]
        val action = PlayerAction.PlayCard(player.id, card.id)
        val result = gameEngine.processAction(action)
        
        println(if (result.success) "✅ ${result.message}" else "❌ ${result.message}")
        
        // 打牌後繼續當前玩家回合
        if (result.success) {
            handlePlayerTurn()
        }
    }
    
    private fun handleEndTurn(player: Player) {
        val action = PlayerAction.EndTurn(player.id)
        val result = gameEngine.processAction(action)
        
        println("✅ ${result.message}")
        println()
    }
}
```

### 主程式更新

```kotlin
// jvmMain/Main.kt
package org.junction.cadherin

import org.junction.cadherin.cli.GameCLI

fun main() {
    val gameYaml = """
        meta:
          name: "Day 2 互動遊戲"
          target_age: [8, 12]
          player_count: [2, 4]
        
        cards:
          attack_card:
            count: 12
            properties:
              damage:
                type: int
                min: 2
                max: 5
              element:
                type: enum
                values: [fire, water, earth]
          
          heal_card:
            count: 6
            properties:
              healing:
                type: int
                min: 2
                max: 4
        
        mechanics:
          setup:
            players:
              health: 15
              hand_size: 4
    """.trimIndent()
    
    println("請輸入玩家數量 (2-4):")
    val playerCount = readLine()?.toIntOrNull() ?: 2
    
    val playerNames = mutableListOf<String>()
    repeat(playerCount) { index ->
        println("請輸入玩家 ${index + 1} 的名稱:")
        val name = readLine() ?: "玩家${index + 1}"
        playerNames.add(name)
    }
    
    val cli = GameCLI()
    cli.startGame(gameYaml, playerNames)
}
```

### 測試更新

```kotlin
// commonTest/core/GameEngineTest.kt
package org.junction.cadherin.core

import org.junction.cadherin.actions.PlayerAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GameEngineTest {
    
    private fun createTestGameYaml() = """
        meta:
          name: "測試遊戲"
          target_age: [8, 12]
        
        cards:
          test_card:
            count: 10
            properties:
              value:
                type: int
                min: 1
                max: 5
        
        mechanics:
          setup:
            players:
              health: 10
              hand_size: 3
    """.trimIndent()
    
    @Test
    fun testGameInitialization() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val gameState = engine.getGameState()
        
        assertEquals(2, gameState.players.size)
        assertEquals("Alice", gameState.players[0].name)
        assertEquals("Bob", gameState.players[1].name)
        
        // 檢查初始設定
        gameState.players.forEach { player ->
            assertEquals(10, player.health)
            assertEquals(3, player.hand.size)
        }
        
        // 檢查牌庫
        assertEquals(4, gameState.deck.size) // 10張卡 - 6張手牌 = 4張
    }
    
    @Test
    fun testDrawCardAction() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val gameState = engine.getGameState()
        val player = gameState.players[0]
        val initialHandSize = player.hand.size
        val initialDeckSize = gameState.deck.size
        
        val action = PlayerAction.DrawCard(player.id)
        val result = engine.processAction(action)
        
        assertTrue(result.success)
        assertEquals(initialHandSize + 1, player.hand.size)
        assertEquals(initialDeckSize - 1, gameState.deck.size)
    }
    
    @Test
    fun testPlayCardAction() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val gameState = engine.getGameState()
        val player = gameState.players[0]
        val cardToPlay = player.hand.first()
        val initialHandSize = player.hand.size
        
        val action = PlayerAction.PlayCard(player.id, cardToPlay.id)
        val result = engine.processAction(action)
        
        assertTrue(result.success)
        assertEquals(initialHandSize - 1, player.hand.size)
        assertEquals(1, gameState.discardPile.size)
        assertEquals(cardToPlay, gameState.discardPile.first())
    }
    
    @Test
    fun testEndTurnAction() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val gameState = engine.getGameState()
        
        assertEquals(0, gameState.currentPlayerIndex) // Alice's turn
        assertEquals(1, gameState.turnNumber)
        
        val action = PlayerAction.EndTurn(gameState.getCurrentPlayer().id)
        val result = engine.processAction(action)
        
        assertTrue(result.success)
        assertEquals(1, gameState.currentPlayerIndex) // Bob's turn
        assertEquals(2, gameState.turnNumber)
    }
    
    @Test
    fun testInvalidActions() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // 測試不存在的玩家
        val invalidPlayerAction = PlayerAction.DrawCard("invalid_player")
        val result1 = engine.processAction(invalidPlayerAction)
        assertFalse(result1.success)
        
        // 測試不存在的卡牌
        val player = engine.getGameState().players[0]
        val invalidCardAction = PlayerAction.PlayCard(player.id, "invalid_card")
        val result2 = engine.processAction(invalidCardAction)
        assertFalse(result2.success)
    }
}
```

## 今日交付目標

- [ ] ✅ 遊戲狀態管理系統完成
- [ ] ✅ 玩家動作處理 (抽牌、打牌、結束回合)
- [ ] ✅ 命令列互動介面
- [ ] ✅ 完整的回合制遊戲循環
- [ ] ✅ 錯誤處理和驗證
- [ ] ✅ UI 狀態提供給前端使用
- [ ] ✅ 單元測試覆蓋關鍵功能

## 驗證指令

```bash
# 編譯測試
./gradlew build

# 執行測試
./gradlew test

# 執行命令列遊戲
./gradlew jvmRun

# 生成 JS 模組
./gradlew jsBrowserDistribution
```

## 前端使用範例

```typescript
// 前端工程師可以這樣使用
import { GameEngine, PlayerAction } from './cadherin-core'

const engine = GameEngine.fromYaml(yamlContent, ['Alice', 'Bob'])

// 處理玩家動作
const drawResult = engine.processAction({
    type: 'DrawCard',
    playerId: 'player_0'
})

// 獲取 UI 狀態用於渲染
const uiState = engine.getUIState()
console.log(`當前玩家: ${uiState.currentPlayerId}`)
console.log(`手牌數量: ${uiState.players[0].hand.length}`)
```

## 明日工作預告

Day 3 將加入事件系統，讓卡牌具備真正的特殊能力（攻擊、治療、效果），這是讓遊戲變「好玩」的關鍵！

## 技術債務記錄

- 手牌上限硬編碼 (明日可從 YAML 配置)
- 卡牌效果系統尚未實作 (Day 3 重點)
- 勝利條件檢查還未加入 (Day 5 實作)

今日成功讓遊戲可以真正「互動」，玩家可以進行有意義的動作！