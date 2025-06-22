# Day 4: 勝利條件與遊戲結束

## 工作目標
- 實作勝利條件檢查系統
- 加入遊戲結束處理邏輯
- 設計遊戲結果顯示
- 完成一個完整可玩的遊戲循環

## 今日範圍

### 擴展 YAML 格式 - 勝利條件
```yaml
# game4.yaml - 完整的可結束遊戲
meta:
  name: "完整數字比大小遊戲"
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
  
  win_conditions:
    - type: "first_to_score"
      target: 5
      message: "{winner} 率先達到 5 分獲勝！"
    - type: "most_score_when_cards_depleted"
      message: "卡牌用盡，{winner} 以 {score} 分獲勝！"
    - type: "turn_limit"
      max_turns: 10
      message: "回合數達上限，{winner} 以 {score} 分獲勝！"

# AI 輔助資訊
ai_hints:
  difficulty_factors: 
    - mechanics.win_conditions[0].target
    - mechanics.setup.players.hand_size
  common_modifications:
    easier: 
      target_score: 3
      hand_size: 3
    harder:
      target_score: 10
      hand_size: 7
```

### 勝利條件系統
```kotlin
// 勝利條件定義
sealed class WinCondition {
    abstract val message: String
    abstract fun check(gameState: GameState): WinResult
    
    data class FirstToScore(
        val target: Int,
        override val message: String
    ) : WinCondition() {
        override fun check(gameState: GameState): WinResult {
            val winner = gameState.players.find { it.score >= target }
            return if (winner != null) {
                WinResult.GameWon(
                    winner = winner,
                    reason = message.replace("{winner}", winner.name)
                                  .replace("{score}", winner.score.toString())
                )
            } else {
                WinResult.GameContinues
            }
        }
    }
    
    data class MostScoreWhenCardsDepleted(
        override val message: String
    ) : WinCondition() {
        override fun check(gameState: GameState): WinResult {
            // 檢查是否所有玩家手牌都空了且牌庫也空了
            val allHandsEmpty = gameState.players.all { it.hand.isEmpty() }
            val deckEmpty = gameState.deck.isEmpty()
            
            if (allHandsEmpty && deckEmpty) {
                val winner = gameState.players.maxByOrNull { it.score }
                return if (winner != null) {
                    WinResult.GameWon(
                        winner = winner,
                        reason = message.replace("{winner}", winner.name)
                                      .replace("{score}", winner.score.toString())
                    )
                } else {
                    WinResult.Draw("遊戲平手！")
                }
            }
            
            return WinResult.GameContinues
        }
    }
    
    data class TurnLimit(
        val maxTurns: Int,
        override val message: String
    ) : WinCondition() {
        override fun check(gameState: GameState): WinResult {
            if (gameState.turnState.turnNumber >= maxTurns) {
                val winner = gameState.players.maxByOrNull { it.score }
                return if (winner != null) {
                    WinResult.GameWon(
                        winner = winner,
                        reason = message.replace("{winner}", winner.name)
                                      .replace("{score}", winner.score.toString())
                    )
                } else {
                    WinResult.Draw("達到回合上限，遊戲平手！")
                }
            }
            
            return WinResult.GameContinues
        }
    }
}

// 勝利結果
sealed class WinResult {
    object GameContinues : WinResult()
    data class GameWon(val winner: Player, val reason: String) : WinResult()
    data class Draw(val reason: String) : WinResult()
}

// 遊戲最終結果
data class GameResult(
    val gameId: String,
    val winner: Player?,
    val finalScores: Map<String, Int>,
    val totalTurns: Int,
    val winCondition: String,
    val gameHistory: List<TurnResult>,
    val duration: Long // 遊戲時長（毫秒）
)
```

### 勝利條件管理器
```kotlin
class WinConditionManager {
    
    fun parseWinConditions(winConditionsData: List<Map<String, Any>>): List<WinCondition> {
        return winConditionsData.mapNotNull { conditionData ->
            parseWinCondition(conditionData)
        }
    }
    
    private fun parseWinCondition(data: Map<String, Any>): WinCondition? {
        val type = data["type"] as? String ?: return null
        val message = data["message"] as? String ?: "遊戲結束"
        
        return when (type) {
            "first_to_score" -> {
                val target = data["target"] as? Int ?: return null
                WinCondition.FirstToScore(target, message)
            }
            "most_score_when_cards_depleted" -> {
                WinCondition.MostScoreWhenCardsDepleted(message)
            }
            "turn_limit" -> {
                val maxTurns = data["max_turns"] as? Int ?: return null
                WinCondition.TurnLimit(maxTurns, message)
            }
            else -> null
        }
    }
    
    fun checkWinConditions(gameState: GameState, winConditions: List<WinCondition>): WinResult {
        for (condition in winConditions) {
            val result = condition.check(gameState)
            if (result != WinResult.GameContinues) {
                return result
            }
        }
        return WinResult.GameContinues
    }
}
```

### 升級遊戲引擎 - 勝利檢查
```kotlin
class GameEngine {
    private val turnManager = TurnManager()
    private val scoringSystem = ScoringSystem()
    private val winConditionManager = WinConditionManager()
    private var gameStartTime: Long = 0
    
    fun createGame(definition: GameDefinition, playerNames: List<String>): GameState {
        gameStartTime = System.currentTimeMillis()
        
        // 原有創建邏輯...
        val gameState = GameState(
            // ... 其他欄位 ...
            winConditions = parseWinConditions(definition)
        )
        
        return gameState
    }
    
    fun processAction(gameState: GameState, action: PlayerAction): ActionResult {
        val actionResult = when (action) {
            is PlayerAction.DrawCard -> processDrawCard(gameState, action)
            is PlayerAction.PlayCard -> processPlayCardInTurn(gameState, action)
            is PlayerAction.EndTurn -> processEndTurn(gameState)
        }
        
        // 每次動作後檢查勝利條件
        if (actionResult == ActionResult.Success) {
            checkGameEnd(gameState)
        }
        
        return actionResult
    }
    
    private fun checkGameEnd(gameState: GameState) {
        val winResult = winConditionManager.checkWinConditions(
            gameState, 
            gameState.winConditions
        )
        
        when (winResult) {
            is WinResult.GameWon -> {
                gameState.gamePhase = GamePhase.FINISHED
                gameState.gameResult = createGameResult(gameState, winResult.winner, winResult.reason)
            }
            is WinResult.Draw -> {
                gameState.gamePhase = GamePhase.FINISHED
                gameState.gameResult = createGameResult(gameState, null, winResult.reason)
            }
            WinResult.GameContinues -> {
                // 遊戲繼續
            }
        }
    }
    
    private fun createGameResult(gameState: GameState, winner: Player?, reason: String): GameResult {
        return GameResult(
            gameId = gameState.gameId,
            winner = winner,
            finalScores = gameState.players.associate { it.id to it.score },
            totalTurns = gameState.turnState.turnNumber,
            winCondition = reason,
            gameHistory = gameState.gameHistory,
            duration = System.currentTimeMillis() - gameStartTime
        )
    }
    
    private fun parseWinConditions(definition: GameDefinition): List<WinCondition> {
        // 從 YAML 定義中解析勝利條件
        val mechanicsData = definition.rawData["mechanics"] as? Map<String, Any>
        val winConditionsData = mechanicsData?.get("win_conditions") as? List<Map<String, Any>>
        
        return if (winConditionsData != null) {
            winConditionManager.parseWinConditions(winConditionsData)
        } else {
            // 預設勝利條件
            listOf(WinCondition.FirstToScore(5, "{winner} 獲勝！"))
        }
    }
}

// 擴展遊戲狀態
data class GameState(
    // ... 原有欄位 ...
    val winConditions: List<WinCondition>,
    var gameResult: GameResult? = null
)
```

### 遊戲結果顯示系統
```kotlin
class GameResultDisplay {
    
    fun displayGameResult(gameResult: GameResult) {
        println("\n" + "=".repeat(50))
        println("             遊戲結束！")
        println("=".repeat(50))
        
        displayWinner(gameResult)
        displayFinalScores(gameResult)
        displayGameStatistics(gameResult)
        displayGameHistory(gameResult)
    }
    
    private fun displayWinner(gameResult: GameResult) {
        if (gameResult.winner != null) {
            println("🏆 勝利者: ${gameResult.winner.name}")
            println("🎯 最終分數: ${gameResult.winner.score}")
        } else {
            println("🤝 遊戲平手！")
        }
        println("📋 勝利條件: ${gameResult.winCondition}")
        println()
    }
    
    private fun displayFinalScores(gameResult: GameResult) {
        println("--- 最終計分板 ---")
        val sortedScores = gameResult.finalScores.toList()
            .sortedByDescending { it.second }
        
        sortedScores.forEachIndexed { index, (playerId, score) ->
            val rank = index + 1
            val medal = when (rank) {
                1 -> "🥇"
                2 -> "🥈" 
                3 -> "🥉"
                else -> "  "
            }
            println("$medal $rank. $playerId: $score 分")
        }
        println()
    }
    
    private fun displayGameStatistics(gameResult: GameResult) {
        println("--- 遊戲統計 ---")
        println("總回合數: ${gameResult.totalTurns}")
        println("遊戲時長: ${formatDuration(gameResult.duration)}")
        println("平均每回合時間: ${formatDuration(gameResult.duration / gameResult.totalTurns)}")
        println()
    }
    
    private fun displayGameHistory(gameResult: GameResult) {
        println("--- 遊戲歷程 (最後5回合) ---")
        gameResult.gameHistory.takeLast(5).forEach { turnResult ->
            println("回合 ${turnResult.turnNumber}:")
            val winner = turnResult.winner
            val maxValue = turnResult.playedCards.values.maxOfOrNull { card ->
                card.properties["value"] as Int
            } ?: 0
            println("  勝利者: $winner (出牌: $maxValue)")
        }
    }
    
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return if (minutes > 0) {
            "${minutes}分${remainingSeconds}秒"
        } else {
            "${remainingSeconds}秒"
        }
    }
    
    fun displayGameSummary(gameState: GameState) {
        if (gameState.gamePhase == GamePhase.FINISHED && gameState.gameResult != null) {
            displayGameResult(gameState.gameResult!!)
        }
    }
}
```

### 完整遊戲循環控制器
```kotlin
class CompleteGameController {
    private val engine = GameEngine()
    private val display = GameDisplaySystem()
    private val resultDisplay = GameResultDisplay()
    
    fun playCompleteGame(definition: GameDefinition, playerNames: List<String>) {
        println("開始遊戲: ${definition.meta.name}")
        println("玩家: ${playerNames.joinToString(", ")}")
        println()
        
        val gameState = engine.createGame(definition, playerNames)
        
        while (gameState.gamePhase == GamePhase.PLAYING) {
            playTurn(gameState)
        }
        
        resultDisplay.displayGameSummary(gameState)
    }
    
    private fun playTurn(gameState: GameState) {
        display.displayGameState(gameState)
        
        when (gameState.turnState.currentPhase) {
            TurnPhase.PLAY -> handlePlayPhase(gameState)
            else -> {
                // 自動處理其他階段
                engine.processAction(gameState, PlayerAction.EndTurn("system"))
            }
        }
    }
    
    private fun handlePlayPhase(gameState: GameState) {
        // 簡化：所有玩家自動出牌（用於演示）
        gameState.players.forEach { player ->
            if (!gameState.turnState.playedCards.containsKey(player.id) && player.hand.isNotEmpty()) {
                val cardToPlay = player.hand.random() // 隨機選牌
                engine.processAction(
                    gameState,
                    PlayerAction.PlayCard(player.id, cardToPlay.id)
                )
            }
        }
    }
}
```

## 測試策略

### 單元測試
```kotlin
class WinConditionTest {
    
    @Test
    fun `should detect first to score win condition`() {
        val winCondition = WinCondition.FirstToScore(5, "{winner} wins!")
        val gameState = createTestGameState()
        
        // 設置一個玩家達到目標分數
        gameState.players[0].score = 5
        
        val result = winCondition.check(gameState)
        
        assertThat(result).isInstanceOf<WinResult.GameWon>()
        val gameWon = result as WinResult.GameWon
        assertThat(gameWon.winner).isEqualTo(gameState.players[0])
    }
    
    @Test
    fun `should detect cards depleted win condition`() {
        val winCondition = WinCondition.MostScoreWhenCardsDepleted("Game over!")
        val gameState = createTestGameState()
        
        // 設置牌庫和手牌都空了
        gameState.deck.clear()
        gameState.players.forEach { it.hand.clear() }
        gameState.players[1].score = 3 // 設置最高分玩家
        
        val result = winCondition.check(gameState)
        
        assertThat(result).isInstanceOf<WinResult.GameWon>()
        val gameWon = result as WinResult.GameWon
        assertThat(gameWon.winner).isEqualTo(gameState.players[1])
    }
    
    @Test
    fun `should detect turn limit win condition`() {
        val winCondition = WinCondition.TurnLimit(5, "Turn limit reached!")
        val gameState = createTestGameState()
        
        gameState.turnState.turnNumber = 5
        gameState.players[0].score = 2 // 最高分
        
        val result = winCondition.check(gameState)
        
        assertThat(result).isInstanceOf<WinResult.GameWon>()
    }
}
```

### 完整遊戲測試
```kotlin
class CompleteGameTest {
    
    @Test
    fun `should play complete game to conclusion`() {
        val definition = loadTestGameDefinition()
        val controller = CompleteGameController()
        
        // 模擬自動遊戲
        assertDoesNotThrow {
            controller.playCompleteGame(definition, listOf("Alice", "Bob"))
        }
        
        println("✅ 完整遊戲流程測試成功！")
    }
    
    @Test
    fun `should handle different win conditions`() {
        // 測試分數勝利
        testWinCondition("first_to_score")
        
        // 測試卡牌耗盡勝利  
        testWinCondition("cards_depleted")
        
        // 測試回合限制勝利
        testWinCondition("turn_limit")
    }
    
    private fun testWinCondition(conditionType: String) {
        val definition = createGameWithWinCondition(conditionType)
        val engine = GameEngine()
        val gameState = engine.createGame(definition, listOf("Player1", "Player2"))
        
        // 模擬遊戲進行直到結束
        var safeguard = 0
        while (gameState.gamePhase == GamePhase.PLAYING && safeguard < 100) {
            simulateRandomTurn(gameState, engine)
            safeguard++
        }
        
        assertThat(gameState.gamePhase).isEqualTo(GamePhase.FINISHED)
        assertThat(gameState.gameResult).isNotNull()
        
        println("✅ $conditionType 勝利條件測試成功")
    }
}
```

## 今日交付成果
- [ ] ✅ 多樣化勝利條件系統
- [ ] ✅ 自動勝利檢查機制
- [ ] ✅ 遊戲結束處理邏輯
- [ ] ✅ 豐富的遊戲結果顯示
- [ ] ✅ 完整的遊戲循環控制
- [ ] ✅ 勝利條件 YAML 配置
- [ ] ✅ AI hints 系統框架

## 明日工作預告
Day 5 將專注於設計和實作 AI hints 系統，為 AI agent 提供遊戲修改的指導資訊。

## 技術債務記錄
- 複雜勝利條件組合邏輯待實作
- 遊戲中途退出處理
- 勝利條件優先級設定

今日完成了第一個真正可玩、可結束的完整遊戲！這是一個重要的里程碑。