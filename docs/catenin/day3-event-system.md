# Day 3: 事件系統實作

## 工作目標
- 實作簡化的卡牌事件系統
- 讓卡牌具備特殊能力（攻擊、治療、效果）
- 支援基本的事件觸發機制
- 讓遊戲變得真正「好玩」

## 今日範圍

### 擴展 YAML 格式 - 事件定義
```yaml
# game3.yaml - 加入事件系統
meta:
  name: "戰鬥卡牌遊戲"
  target_age: [8, 12]
  player_count: [2, 4]

cards:
  fire_attack:
    count: 8
    properties:
      damage: {type: int, min: 2, max: 5}
      element: {type: enum, values: [fire]}
    events:
      on_play:
        action: "deal_damage"
        target: "opponent"
        amount: "{damage}"
  
  heal_card:
    count: 4
    properties:
      healing: {type: int, min: 2, max: 4}
    events:
      on_play:
        action: "restore_health"
        target: "self"
        amount: "{healing}"
  
  shield_card:
    count: 3
    properties:
      defense: {type: int, min: 1, max: 3}
    events:
      on_play:
        action: "add_shield"
        target: "self"
        amount: "{defense}"

mechanics:
  setup:
    players:
      health: 15
      hand_size: 5
      shield: 0
```

### 事件系統核心

```kotlin
// commonMain/events/EventSystem.kt
package org.junction.catenin.events

import org.junction.catenin.model.*
import kotlinx.serialization.Serializable

@Serializable
data class CardEvent(
    val action: String,
    val target: String? = null,
    val amount: String? = null,
    val condition: String? = null
)

@Serializable
data class GameEffect(
    val type: String,
    val targetPlayerId: String,
    val amount: Int,
    val description: String
)

class EventProcessor {
    
    fun processCardPlay(
        gameState: GameState,
        playerId: String,
        card: Card
    ): List<GameEffect> {
        val effects = mutableListOf<GameEffect>()
        
        // 獲取卡牌的 on_play 事件
        val cardType = gameState.definition.cards[card.type]
        val onPlayEvent = cardType?.events?.get("on_play")
        
        if (onPlayEvent != null) {
            val effect = processEvent(gameState, playerId, card, onPlayEvent)
            if (effect != null) {
                effects.add(effect)
                applyEffect(gameState, effect)
            }
        }
        
        return effects
    }
    
    private fun processEvent(
        gameState: GameState,
        playerId: String,
        card: Card,
        event: CardEvent
    ): GameEffect? {
        val targetPlayerId = resolveTarget(gameState, playerId, event.target)
            ?: return null
        
        val amount = resolveAmount(card, event.amount)
        
        return when (event.action) {
            "deal_damage" -> GameEffect(
                type = "damage",
                targetPlayerId = targetPlayerId,
                amount = amount,
                description = "${getPlayerName(gameState, playerId)} 對 ${getPlayerName(gameState, targetPlayerId)} 造成 ${amount} 點傷害"
            )
            
            "restore_health" -> GameEffect(
                type = "heal",
                targetPlayerId = targetPlayerId,
                amount = amount,
                description = "${getPlayerName(gameState, targetPlayerId)} 恢復 ${amount} 點生命值"
            )
            
            "add_shield" -> GameEffect(
                type = "shield",
                targetPlayerId = targetPlayerId,
                amount = amount,
                description = "${getPlayerName(gameState, targetPlayerId)} 獲得 ${amount} 點護盾"
            )
            
            else -> null
        }
    }
    
    private fun resolveTarget(
        gameState: GameState,
        playerId: String,
        targetString: String?
    ): String? {
        return when (targetString) {
            "self" -> playerId
            "opponent" -> {
                // 簡化：選擇第一個非自己的玩家
                gameState.players.find { it.id != playerId }?.id
            }
            "all_opponents" -> {
                // 暫時不支援多目標，返回第一個對手
                gameState.players.find { it.id != playerId }?.id
            }
            else -> null
        }
    }
    
    private fun resolveAmount(card: Card, amountString: String?): Int {
        return when {
            amountString == null -> 1
            amountString.startsWith("{") && amountString.endsWith("}") -> {
                // 參數替換，例如 "{damage}" 
                val propertyName = amountString.removeSurrounding("{", "}")
                card.getIntProperty(propertyName) ?: 1
            }
            else -> amountString.toIntOrNull() ?: 1
        }
    }
    
    private fun applyEffect(gameState: GameState, effect: GameEffect) {
        val targetPlayer = gameState.getPlayer(effect.targetPlayerId) ?: return
        
        when (effect.type) {
            "damage" -> {
                val actualDamage = maxOf(0, effect.amount - targetPlayer.shield)
                targetPlayer.takeDamage(actualDamage)
                targetPlayer.shield = maxOf(0, targetPlayer.shield - effect.amount)
            }
            
            "heal" -> {
                targetPlayer.heal(effect.amount)
            }
            
            "shield" -> {
                targetPlayer.shield += effect.amount
            }
        }
    }
    
    private fun getPlayerName(gameState: GameState, playerId: String): String {
        return gameState.getPlayer(playerId)?.name ?: playerId
    }
}
```

### 擴展 Player 模型

```kotlin
// commonMain/model/Player.kt (擴展)
@Serializable
data class Player(
    val id: String,
    val name: String,
    val hand: MutableList<Card> = mutableListOf(),
    var health: Int = 10,
    var score: Int = 0,
    var shield: Int = 0  // 新增護盾屬性
) {
    // ... 原有方法 ...
    
    fun takeDamage(amount: Int) {
        health = maxOf(0, health - amount)
    }
    
    fun heal(amount: Int) {
        health += amount
    }
    
    fun isAlive(): Boolean = health > 0
    
    fun addShield(amount: Int) {
        shield += amount
    }
}
```

### 升級 GameEngine - 事件處理

```kotlin
// commonMain/core/GameEngine.kt (擴展)
@JsExport
class GameEngine private constructor(
    private var gameState: GameState
) {
    private val eventProcessor = EventProcessor()
    
    // ... 原有方法 ...
    
    private fun processPlayCard(action: PlayerAction.PlayCard): ActionResult {
        val player = gameState.getPlayer(action.playerId)
            ?: return ActionResult.failure("玩家不存在")
        
        val card = player.removeCard(action.cardId)
            ?: return ActionResult.failure("手牌中沒有此卡牌")
        
        // 處理卡牌事件
        val effects = eventProcessor.processCardPlay(gameState, action.playerId, card)
        
        // 將卡牌放入棄牌堆
        gameState.discardPile.add(card)
        
        // 準備結果訊息
        val effectDescriptions = effects.map { it.description }
        val message = "${player.name} 打出了 ${card.id}" + 
                     if (effectDescriptions.isNotEmpty()) {
                         "\n效果：${effectDescriptions.joinToString(", ")}"
                     } else ""
        
        return ActionResult.withEffects(message, effects)
    }
    
    @JsName("getUIState")
    fun getUIState(): UIState {
        return UIState(
            players = gameState.players.map { player ->
                PlayerUIState(
                    id = player.id,
                    name = player.name,
                    health = player.health,
                    shield = player.shield,
                    handSize = player.hand.size,
                    score = player.score
                )
            },
            currentPlayerId = gameState.getCurrentPlayer().id,
            gamePhase = gameState.gamePhase.name,
            turnNumber = gameState.turnNumber,
            deckSize = gameState.deck.size,
            discardPileSize = gameState.discardPile.size
        )
    }
}

@JsExport
data class PlayerUIState(
    val id: String,
    val name: String,
    val health: Int,
    val shield: Int,
    val handSize: Int,
    val score: Int
)

@JsExport
data class UIState(
    val players: List<PlayerUIState>,
    val currentPlayerId: String,
    val gamePhase: String,
    val turnNumber: Int,
    val deckSize: Int,
    val discardPileSize: Int
)
```

### 升級命令列介面

```kotlin
// jvmMain/cli/GameCLI.kt (擴展)
class GameCLI {
    // ... 原有方法 ...
    
    private fun displayGameState() {
        val uiState = gameEngine.getUIState()
        val gameState = gameEngine.getGameState()
        
        println("=" * 50)
        println("回合 ${uiState.turnNumber}")
        println("牌庫剩餘：${uiState.deckSize} 張")
        println("棄牌堆：${uiState.discardPileSize} 張")
        println()
        
        // 顯示玩家狀態（包含護盾）
        uiState.players.forEach { player ->
            val isCurrent = player.id == uiState.currentPlayerId
            val marker = if (isCurrent) "👉 " else "   "
            
            val shieldDisplay = if (player.shield > 0) " 🛡️${player.shield}" else ""
            println("$marker${player.name} (❤️${player.health}$shieldDisplay, 🂠${player.handSize})")
            
            if (isCurrent) {
                val actualPlayer = gameState.getPlayer(player.id)
                if (actualPlayer != null) {
                    displayPlayerHand(actualPlayer)
                }
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
            val description = getCardDescription(card)
            println("       $index) ${card.id}: $description")
        }
    }
    
    private fun getCardDescription(card: Card): String {
        val damage = card.getIntProperty("damage")
        val healing = card.getIntProperty("healing")
        val defense = card.getIntProperty("defense")
        val element = card.getStringProperty("element")
        
        return when {
            damage != null && element != null -> 
                "🔥 $element 攻擊 ($damage 傷害)"
            healing != null -> 
                "💚 治療 ($healing 生命值)"
            defense != null -> 
                "🛡️ 護盾 ($defense 防禦)"
            else -> {
                val value = card.getIntProperty("value")
                val color = card.getStringProperty("color")
                if (value != null && color != null) {
                    "$color 數字 $value"
                } else {
                    "未知卡牌"
                }
            }
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
        
        if (result.success) {
            println("✅ ${result.message}")
            
            // 顯示效果
            if (result.effects.isNotEmpty()) {
                println("⚡ 效果觸發:")
                result.effects.forEach { effect ->
                    println("   ${effect.description}")
                }
            }
        } else {
            println("❌ ${result.message}")
        }
        
        // 打牌後繼續當前玩家回合
        if (result.success) {
            handlePlayerTurn()
        }
    }
}
```

### 主程式更新

```kotlin
// jvmMain/Main.kt
package org.junction.catenin

import org.junction.catenin.cli.GameCLI

fun main() {
    val gameYaml = """
        meta:
          name: "Day 3 戰鬥遊戲"
          target_age: [8, 12]
          player_count: [2, 4]
        
        cards:
          fire_attack:
            count: 8
            properties:
              damage:
                type: int
                min: 2
                max: 5
              element:
                type: enum
                values: [fire]
            events:
              on_play:
                action: "deal_damage"
                target: "opponent"
                amount: "{damage}"
          
          heal_card:
            count: 4
            properties:
              healing:
                type: int
                min: 2
                max: 4
            events:
              on_play:
                action: "restore_health"
                target: "self"
                amount: "{healing}"
          
          shield_card:
            count: 3
            properties:
              defense:
                type: int
                min: 1
                max: 3
            events:
              on_play:
                action: "add_shield"
                target: "self"
                amount: "{defense}"
        
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
// commonTest/events/EventSystemTest.kt
package org.junction.catenin.events

import org.junction.catenin.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventSystemTest {
    
    @Test
    fun testDamageEvent() {
        val gameState = createTestGameState()
        val eventProcessor = EventProcessor()
        
        val attackCard = Card(
            id = "attack_1",
            type = "fire_attack",
            properties = mapOf(
                "damage" to CardPropertyValue.IntValue(3),
                "element" to CardPropertyValue.StringValue("fire")
            )
        )
        
        val initialHealth = gameState.players[1].health
        val effects = eventProcessor.processCardPlay(gameState, "player_0", attackCard)
        
        assertEquals(1, effects.size)
        assertEquals("damage", effects[0].type)
        assertEquals(initialHealth - 3, gameState.players[1].health)
    }
    
    @Test
    fun testHealEvent() {
        val gameState = createTestGameState()
        val eventProcessor = EventProcessor()
        
        // 先讓玩家受傷
        gameState.players[0].takeDamage(5)
        val damagedHealth = gameState.players[0].health
        
        val healCard = Card(
            id = "heal_1",
            type = "heal_card",
            properties = mapOf(
                "healing" to CardPropertyValue.IntValue(3)
            )
        )
        
        val effects = eventProcessor.processCardPlay(gameState, "player_0", healCard)
        
        assertEquals(1, effects.size)
        assertEquals("heal", effects[0].type)
        assertEquals(damagedHealth + 3, gameState.players[0].health)
    }
    
    @Test
    fun testShieldEvent() {
        val gameState = createTestGameState()
        val eventProcessor = EventProcessor()
        
        val shieldCard = Card(
            id = "shield_1",
            type = "shield_card",
            properties = mapOf(
                "defense" to CardPropertyValue.IntValue(2)
            )
        )
        
        val effects = eventProcessor.processCardPlay(gameState, "player_0", shieldCard)
        
        assertEquals(1, effects.size)
        assertEquals("shield", effects[0].type)
        assertEquals(2, gameState.players[0].shield)
    }
    
    @Test
    fun testShieldBlocksDamage() {
        val gameState = createTestGameState()
        val eventProcessor = EventProcessor()
        
        // 先給玩家護盾
        gameState.players[1].addShield(2)
        val initialHealth = gameState.players[1].health
        
        val attackCard = Card(
            id = "attack_1",
            type = "fire_attack",
            properties = mapOf(
                "damage" to CardPropertyValue.IntValue(3)
            )
        )
        
        eventProcessor.processCardPlay(gameState, "player_0", attackCard)
        
        // 護盾應該擋住部分傷害
        assertEquals(initialHealth - 1, gameState.players[1].health) // 3傷害 - 2護盾 = 1實際傷害
        assertEquals(0, gameState.players[1].shield) // 護盾被消耗完
    }
    
    private fun createTestGameState(): GameState {
        val players = listOf(
            Player(id = "player_0", name = "Alice", health = 15),
            Player(id = "player_1", name = "Bob", health = 15)
        )
        
        return GameState(
            gameId = "test_game",
            definition = createTestDefinition(),
            players = players,
            deck = mutableListOf(),
            discardPile = mutableListOf(),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.PLAYING,
            turnNumber = 1
        )
    }
    
    private fun createTestDefinition(): GameDefinition {
        return GameDefinition(
            meta = GameMeta(name = "Test Game", targetAge = listOf(8, 12)),
            cards = mapOf(
                "fire_attack" to CardTypeDefinition(
                    count = 8,
                    properties = mapOf(
                        "damage" to PropertyDefinition.IntProperty(min = 2, max = 5),
                        "element" to PropertyDefinition.EnumProperty(values = listOf("fire"))
                    ),
                    events = mapOf(
                        "on_play" to EventDefinition(
                            action = "deal_damage",
                            target = "opponent",
                            amount = "{damage}"
                        )
                    )
                ),
                "heal_card" to CardTypeDefinition(
                    count = 4,
                    properties = mapOf(
                        "healing" to PropertyDefinition.IntProperty(min = 2, max = 4)
                    ),
                    events = mapOf(
                        "on_play" to EventDefinition(
                            action = "restore_health",
                            target = "self",
                            amount = "{healing}"
                        )
                    )
                ),
                "shield_card" to CardTypeDefinition(
                    count = 3,
                    properties = mapOf(
                        "defense" to PropertyDefinition.IntProperty(min = 1, max = 3)
                    ),
                    events = mapOf(
                        "on_play" to EventDefinition(
                            action = "add_shield",
                            target = "self",
                            amount = "{defense}"
                        )
                    )
                )
            )
        )
    }
}
```

## 今日交付目標

- [ ] ✅ 簡化的事件系統實作
- [ ] ✅ 卡牌特殊能力支援（攻擊、治療、護盾）
- [ ] ✅ 基本的參數替換機制
- [ ] ✅ 固定目標選擇（自己、對手）
- [ ] ✅ 效果執行和狀態更新
- [ ] ✅ 命令列介面升級（顯示護盾）
- [ ] ✅ 事件系統單元測試

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
import { GameEngine, PlayerAction } from './catenin-core'

const engine = GameEngine.fromYaml(yamlContent, ['Alice', 'Bob'])

// 處理玩家動作
const playResult = engine.processAction({
    type: 'PlayCard',
    playerId: 'player_0',
    cardId: 'fire_attack_1'
})

// 獲取 UI 狀態用於渲染
const uiState = engine.getUIState()
console.log(`玩家狀態: ${uiState.players[0].name} - 生命值: ${uiState.players[0].health}, 護盾: ${uiState.players[0].shield}`)

// 顯示效果
if (playResult.effects.length > 0) {
    playResult.effects.forEach(effect => {
        console.log(`效果: ${effect.description}`)
    })
}
```

## 明日工作預告

Day 4 將加入回合管理與計分系統，讓遊戲有結構化的進行節奏。

## 技術債務記錄

- 多目標事件支援待實作
- 條件式事件觸發尚未加入
- 事件優先級和順序管理
- 更複雜的參數替換機制

今日成功讓卡牌具備真正的特殊能力，遊戲變得有趣且具有策略性！