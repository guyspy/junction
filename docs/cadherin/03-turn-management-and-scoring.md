# Day 3: 回合管理與計分機制

## 工作目標
- 實作結構化的回合管理系統
- 加入基本計分機制
- 設計回合階段流程
- 讓遊戲有明確的進行節奏

## 今日範圍

### 擴展 YAML 格式 - 回合結構
```yaml
# game3.yaml - 加入回合管理和計分
meta:
  name: "回合制數字比大小"
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
  
  turn_structure:
    phases:
      - name: "play"
        description: "每位玩家打出一張牌"
        action: "simultaneous_play"
      - name: "compare"
        description: "比較卡牌數值"
        action: "compare_values"
      - name: "score"
        description: "計算得分"
        action: "award_points"
      - name: "cleanup"
        description: "清理回合"
        action: "discard_played_cards"
  
  scoring:
    method: "highest_value_wins"
    points_per_round: 1
    bonus_rules: []
```

### 回合管理系統
```kotlin
// 回合階段定義
enum class TurnPhase {
    PLAY,       // 玩家出牌階段
    COMPARE,    // 比較階段  
    SCORE,      // 計分階段
    CLEANUP,    // 清理階段
    TURN_END    // 回合結束
}

// 回合狀態
data class TurnState(
    val turnNumber: Int,
    val currentPhase: TurnPhase,
    val playedCards: MutableMap<String, Card>, // playerId -> card
    val roundWinner: String? = null,
    val roundScore: Int = 0
)

// 擴展遊戲狀態
data class GameState(
    val gameId: String,
    val definition: GameDefinition,
    val players: List<Player>,
    val deck: MutableList<Card>,
    val discardPile: MutableList<Card>,
    val currentPlayer: Int,
    val gamePhase: GamePhase,
    val turnState: TurnState,
    val gameHistory: MutableList<TurnResult>
) {
    fun getCurrentPlayer(): Player = players[currentPlayer]
    fun getPlayer(playerId: String): Player? = players.find { it.id == playerId }
    fun allPlayersPlayedThisRound(): Boolean = 
        playedCards.size == players.size
}

// 回合結果記錄
data class TurnResult(
    val turnNumber: Int,
    val playedCards: Map<String, Card>,
    val winner: String,
    val scores: Map<String, Int>
)
```

### 回合管理器
```kotlin
class TurnManager {
    
    fun startNewTurn(gameState: GameState): GameState {
        val newTurnNumber = gameState.turnState.turnNumber + 1
        
        return gameState.copy(
            turnState = TurnState(
                turnNumber = newTurnNumber,
                currentPhase = TurnPhase.PLAY,
                playedCards = mutableMapOf()
            )
        )
    }
    
    fun processPhase(gameState: GameState): PhaseResult {
        return when (gameState.turnState.currentPhase) {
            TurnPhase.PLAY -> processPlayPhase(gameState)
            TurnPhase.COMPARE -> processComparePhase(gameState)
            TurnPhase.SCORE -> processScorePhase(gameState)
            TurnPhase.CLEANUP -> processCleanupPhase(gameState)
            TurnPhase.TURN_END -> PhaseResult.TurnComplete
        }
    }
    
    private fun processPlayPhase(gameState: GameState): PhaseResult {
        // 檢查是否所有玩家都已出牌
        if (gameState.turnState.playedCards.size == gameState.players.size) {
            return PhaseResult.AdvanceToNext(TurnPhase.COMPARE)
        }
        
        return PhaseResult.WaitingForPlayers
    }
    
    private fun processComparePhase(gameState: GameState): PhaseResult {
        val playedCards = gameState.turnState.playedCards
        
        // 找出數值最大的卡牌
        val (winnerPlayerId, winningCard) = playedCards.maxByOrNull { (_, card) ->
            card.properties["value"] as Int
        } ?: return PhaseResult.Error("沒有卡牌可比較")
        
        // 更新回合狀態
        gameState.turnState.apply {
            roundWinner = winnerPlayerId
            roundScore = calculateRoundScore(playedCards)
        }
        
        return PhaseResult.AdvanceToNext(TurnPhase.SCORE)
    }
    
    private fun processScorePhase(gameState: GameState): PhaseResult {
        val winner = gameState.turnState.roundWinner ?: return PhaseResult.Error("沒有回合勝利者")
        val score = gameState.turnState.roundScore
        
        // 更新玩家分數
        val winnerPlayer = gameState.getPlayer(winner)
        if (winnerPlayer != null) {
            winnerPlayer.score += score
        }
        
        return PhaseResult.AdvanceToNext(TurnPhase.CLEANUP)
    }
    
    private fun processCleanupPhase(gameState: GameState): PhaseResult {
        // 將打出的卡牌移到棄牌堆
        gameState.turnState.playedCards.values.forEach { card ->
            gameState.discardPile.add(card)
        }
        
        // 記錄回合結果
        val turnResult = TurnResult(
            turnNumber = gameState.turnState.turnNumber,
            playedCards = gameState.turnState.playedCards.toMap(),
            winner = gameState.turnState.roundWinner!!,
            scores = gameState.players.associate { it.id to it.score }
        )
        gameState.gameHistory.add(turnResult)
        
        return PhaseResult.AdvanceToNext(TurnPhase.TURN_END)
    }
    
    private fun calculateRoundScore(playedCards: Map<String, Card>): Int {
        // 簡單計分：贏家得1分
        return 1
    }
}

sealed class PhaseResult {
    object WaitingForPlayers : PhaseResult()
    data class AdvanceToNext(val nextPhase: TurnPhase) : PhaseResult()
    object TurnComplete : PhaseResult()
    data class Error(val message: String) : PhaseResult()
}
```

### 計分系統
```kotlin
class ScoringSystem {
    
    fun calculateScore(
        scoringMethod: String,
        playedCards: Map<String, Card>,
        players: List<Player>
    ): ScoringResult {
        return when (scoringMethod) {
            "highest_value_wins" -> calculateHighestValueWins(playedCards)
            "sum_of_values" -> calculateSumOfValues(playedCards)
            else -> ScoringResult.Error("未知的計分方法: $scoringMethod")
        }
    }
    
    private fun calculateHighestValueWins(playedCards: Map<String, Card>): ScoringResult {
        val cardValues = playedCards.mapValues { (_, card) ->
            card.properties["value"] as Int
        }
        
        val maxValue = cardValues.values.maxOrNull() ?: 0
        val winners = cardValues.filter { it.value == maxValue }.keys
        
        return if (winners.size == 1) {
            ScoringResult.SingleWinner(winners.first(), 1)
        } else {
            ScoringResult.Tie(winners.toList())
        }
    }
    
    private fun calculateSumOfValues(playedCards: Map<String, Card>): ScoringResult {
        val totalValue = playedCards.values.sumOf { card ->
            card.properties["value"] as Int
        }
        
        // 所有玩家都獲得總和分數（示例規則）
        return ScoringResult.AllPlayersScore(totalValue)
    }
}

sealed class ScoringResult {
    data class SingleWinner(val playerId: String, val points: Int) : ScoringResult()
    data class Tie(val playerIds: List<String>) : ScoringResult()
    data class AllPlayersScore(val points: Int) : ScoringResult()
    data class Error(val message: String) : ScoringResult()
}
```

### 升級遊戲引擎
```kotlin
class GameEngine {
    private val turnManager = TurnManager()
    private val scoringSystem = ScoringSystem()
    
    fun createGame(definition: GameDefinition, playerNames: List<String>): GameState {
        // 原有創建邏輯...
        
        return GameState(
            gameId = "game_${System.currentTimeMillis()}",
            definition = definition,
            players = players,
            deck = allCards,
            discardPile = mutableListOf(),
            currentPlayer = 0,
            gamePhase = GamePhase.PLAYING,
            turnState = TurnState(
                turnNumber = 0,
                currentPhase = TurnPhase.PLAY,
                playedCards = mutableMapOf()
            ),
            gameHistory = mutableListOf()
        )
    }
    
    fun processAction(gameState: GameState, action: PlayerAction): ActionResult {
        return when (action) {
            is PlayerAction.DrawCard -> processDrawCard(gameState, action)
            is PlayerAction.PlayCard -> processPlayCardInTurn(gameState, action)
            is PlayerAction.EndTurn -> processEndTurn(gameState)
        }
    }
    
    private fun processPlayCardInTurn(gameState: GameState, action: PlayerAction.PlayCard): ActionResult {
        // 檢查是否在出牌階段
        if (gameState.turnState.currentPhase != TurnPhase.PLAY) {
            return ActionResult.Failure("當前不是出牌階段")
        }
        
        // 檢查玩家是否已經出過牌
        if (gameState.turnState.playedCards.containsKey(action.playerId)) {
            return ActionResult.Failure("本回合已經出過牌")
        }
        
        // 執行出牌
        val player = gameState.getPlayer(action.playerId)
            ?: return ActionResult.Failure("玩家不存在")
        
        val cardIndex = player.hand.indexOfFirst { it.id == action.cardId }
        if (cardIndex == -1) {
            return ActionResult.Failure("手牌中沒有此卡牌")
        }
        
        val playedCard = player.hand.removeAt(cardIndex)
        gameState.turnState.playedCards[action.playerId] = playedCard
        
        // 檢查是否需要進入下一階段
        processTurnPhases(gameState)
        
        return ActionResult.Success
    }
    
    private fun processEndTurn(gameState: GameState): ActionResult {
        // 強制進入下一階段或結束回合
        val result = turnManager.processPhase(gameState)
        
        when (result) {
            is PhaseResult.AdvanceToNext -> {
                gameState.turnState.currentPhase = result.nextPhase
                processTurnPhases(gameState)
            }
            is PhaseResult.TurnComplete -> {
                startNewTurn(gameState)
            }
            is PhaseResult.Error -> {
                return ActionResult.Failure(result.message)
            }
        }
        
        return ActionResult.Success
    }
    
    private fun processTurnPhases(gameState: GameState) {
        var continueProcessing = true
        
        while (continueProcessing) {
            val result = turnManager.processPhase(gameState)
            
            when (result) {
                is PhaseResult.AdvanceToNext -> {
                    gameState.turnState.currentPhase = result.nextPhase
                }
                is PhaseResult.TurnComplete -> {
                    startNewTurn(gameState)
                    continueProcessing = false
                }
                is PhaseResult.WaitingForPlayers -> {
                    continueProcessing = false
                }
                is PhaseResult.Error -> {
                    println("回合處理錯誤: ${result.message}")
                    continueProcessing = false
                }
            }
        }
    }
    
    private fun startNewTurn(gameState: GameState) {
        turnManager.startNewTurn(gameState)
    }
}

// 新增動作類型
sealed class PlayerAction {
    abstract val playerId: String
    
    data class DrawCard(override val playerId: String) : PlayerAction()
    data class PlayCard(override val playerId: String, val cardId: String) : PlayerAction()
    data class EndTurn(override val playerId: String) : PlayerAction()
}
```

### 升級顯示系統
```kotlin
class GameDisplaySystem {
    
    fun displayGameState(gameState: GameState) {
        println("=== ${gameState.definition.meta.name} ===")
        println("回合 ${gameState.turnState.turnNumber} - 階段: ${gameState.turnState.currentPhase}")
        println("牌庫剩餘: ${gameState.deck.size} 張")
        println()
        
        displayCurrentRound(gameState)
        displayPlayers(gameState)
        displayScoreboard(gameState)
    }
    
    private fun displayCurrentRound(gameState: GameState) {
        println("--- 本回合狀況 ---")
        if (gameState.turnState.playedCards.isNotEmpty()) {
            gameState.turnState.playedCards.forEach { (playerId, card) ->
                val playerName = gameState.getPlayer(playerId)?.name ?: playerId
                val value = card.properties["value"]
                val color = card.properties["color"]
                println("$playerName: ${color}色數字${value}")
            }
        } else {
            println("尚無玩家出牌")
        }
        
        if (gameState.turnState.roundWinner != null) {
            val winnerName = gameState.getPlayer(gameState.turnState.roundWinner!!)?.name
            println("本回合勝利者: $winnerName (+${gameState.turnState.roundScore}分)")
        }
        println()
    }
    
    private fun displayScoreboard(gameState: GameState) {
        println("--- 計分板 ---")
        val sortedPlayers = gameState.players.sortedByDescending { it.score }
        sortedPlayers.forEachIndexed { index, player ->
            val rank = index + 1
            println("$rank. ${player.name}: ${player.score} 分")
        }
        println()
    }
    
    fun displayTurnResult(turnResult: TurnResult) {
        println("=== 回合 ${turnResult.turnNumber} 結果 ===")
        turnResult.playedCards.forEach { (playerId, card) ->
            val value = card.properties["value"]
            val color = card.properties["color"]
            println("$playerId: ${color}色數字${value}")
        }
        println("勝利者: ${turnResult.winner}")
        println()
    }
}
```

## 測試策略

### 單元測試
```kotlin
class TurnManagerTest {
    
    @Test
    fun `should advance from play to compare phase when all players played`() {
        val gameState = createTestGameState()
        val turnManager = TurnManager()
        
        // 模擬所有玩家都出牌
        gameState.players.forEach { player ->
            gameState.turnState.playedCards[player.id] = createTestCard()
        }
        
        val result = turnManager.processPhase(gameState)
        
        assertThat(result).isInstanceOf<PhaseResult.AdvanceToNext>()
        assertThat((result as PhaseResult.AdvanceToNext).nextPhase)
            .isEqualTo(TurnPhase.COMPARE)
    }
    
    @Test
    fun `should determine winner correctly in compare phase`() {
        val gameState = createTestGameState()
        val turnManager = TurnManager()
        
        // 設置測試卡牌
        gameState.turnState.apply {
            currentPhase = TurnPhase.COMPARE
            playedCards["player_0"] = createCardWithValue(5)
            playedCards["player_1"] = createCardWithValue(8)
            playedCards["player_2"] = createCardWithValue(3)
        }
        
        val result = turnManager.processPhase(gameState)
        
        assertThat(gameState.turnState.roundWinner).isEqualTo("player_1")
        assertThat(result).isInstanceOf<PhaseResult.AdvanceToNext>()
    }
}

class ScoringSystemTest {
    
    @Test
    fun `should calculate highest value wins correctly`() {
        val scoringSystem = ScoringSystem()
        val playedCards = mapOf(
            "player_0" to createCardWithValue(5),
            "player_1" to createCardWithValue(8),
            "player_2" to createCardWithValue(3)
        )
        
        val result = scoringSystem.calculateScore("highest_value_wins", playedCards, emptyList())
        
        assertThat(result).isInstanceOf<ScoringResult.SingleWinner>()
        val winner = result as ScoringResult.SingleWinner
        assertThat(winner.playerId).isEqualTo("player_1")
        assertThat(winner.points).isEqualTo(1)
    }
}
```

### 整合測試
```kotlin
class Day3IntegrationTest {
    
    @Test
    fun `complete turn cycle with scoring`() {
        val definition = loadTestGameDefinition()
        val engine = GameEngine()
        val gameState = engine.createGame(definition, listOf("Alice", "Bob", "Charlie"))
        
        // 模擬一個完整回合
        // 1. 每位玩家出牌
        gameState.players.forEach { player ->
            val card = player.hand.first()
            val result = engine.processAction(
                gameState,
                PlayerAction.PlayCard(player.id, card.id)
            )
            assertThat(result).isEqualTo(ActionResult.Success)
        }
        
        // 2. 檢查回合是否自動進行
        assertThat(gameState.turnState.currentPhase).isEqualTo(TurnPhase.PLAY) // 新回合開始
        assertThat(gameState.turnState.turnNumber).isEqualTo(1)
        
        // 3. 檢查分數是否正確更新
        val totalScore = gameState.players.sumOf { it.score }
        assertThat(totalScore).isEqualTo(1) // 應該有一位玩家得到1分
        
        println("✅ Day 3 完整回合循環測試成功！")
    }
}
```

## 執行範例

### 測試 YAML 檔案
```yaml
# day3_test.yaml
meta:
  name: "回合制比大小"
  target_age: [8, 12]
  player_count: [2, 4]

cards:
  number_card:
    count: 24
    properties:
      value: {type: int, min: 1, max: 12}
      color: {type: enum, values: [red, blue, green, yellow]}

mechanics:
  setup:
    deck: {shuffle: true}
    players:
      hand_size: 6
      initial_score: 0
  
  turn_structure:
    phases:
      - name: "play"
        action: "simultaneous_play"
      - name: "compare"
        action: "compare_values"
      - name: "score"
        action: "award_points"
      - name: "cleanup"
        action: "discard_played_cards"
  
  scoring:
    method: "highest_value_wins"
    points_per_round: 1
```

## 今日交付成果
- [ ] ✅ 結構化回合管理系統
- [ ] ✅ 回合階段自動流程
- [ ] ✅ 基本計分機制
- [ ] ✅ 回合結果記錄
- [ ] ✅ 升級的遊戲顯示
- [ ] ✅ 完整的回合循環測試

## 明日工作預告
Day 4 將加入勝利條件檢查和遊戲結束處理，讓遊戲有明確的結束時機。

## 技術債務記錄
- 計分規則較簡單（可後續擴展複雜計分）
- 平手處理邏輯待完善
- 回合超時機制未實作

今日成功讓遊戲有了結構化的進行節奏，玩家行動變得更有意義！