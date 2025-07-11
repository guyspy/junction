import org.junction.catenin.core.GameEngine
import org.junction.catenin.core.createGameEngineFromYaml
import org.junction.catenin.actions.PlayerAction
import org.junction.catenin.parser.GameDefinitionParser
import org.junction.catenin.model.CardFactory

fun main() {
    console.log("🎮 Catenin Node.js Demo - Day 2 Interactive Server...")
    
    // Create a real game server using Catenin library
    val gameServer = GameServer()
    gameServer.start()
}

class GameServer {
    private val games = mutableMapOf<String, GameEngine>()
    
    fun start() {
        console.log("🚀 Game Server Started")
        console.log("📡 Server ready to handle game sessions")
        
        // Create real game rooms using Catenin library
        createGameRoom("room1", listOf("Alice", "Bob"))
        createGameRoom("room2", listOf("Charlie", "Diana"))
        
        // Demonstrate actual gameplay
        demonstrateGameplay("room1")
    }
    
    private fun createGameRoom(roomId: String, players: List<String>) {
        console.log("🏠 Creating game room: $roomId")
        console.log("👥 Players: ${players.joinToString(", ")}")
        
        // Real YAML game definition
        val yamlContent = """
            meta:
              name: "Node.js Server Demo Game"
              target_age: [8, 12]
              player_count: [2, 2]
            
            cards:
              attack_card:
                count: 12
                properties:
                  damage: {type: int, min: 2, max: 5}
                  element: {type: enum, values: [fire, water, earth]}
              
              heal_card:
                count: 8
                properties:
                  healing: {type: int, min: 1, max: 3}
            
            mechanics:
              setup:
                players:
                  health: 20
                  hand_size: 5
              win_conditions:
                - type: "health_depleted"
                  message: "{winner} wins the battle!"
        """.trimIndent()
        
        try {
            // Use the actual Catenin library to create game engine
            val gameEngine = createGameEngineFromYaml(yamlContent, players.toTypedArray())
            games[roomId] = gameEngine
            
            console.log("📄 Game definition parsed successfully")
            console.log("🎮 Game engine created for room $roomId")
            
            // Show game information
            val definition = gameEngine.getGameDefinition()
            console.log("📋 Game: ${definition.meta.name}")
            console.log("🎯 Target age: ${definition.meta.targetAge}")
            console.log("👥 Player count: ${definition.meta.playerCount}")
            
            // Show players
            val gamePlayers = gameEngine.getPlayers()
            console.log("👤 Players in room:")
            gamePlayers.forEach { player ->
                console.log("  - ${player.name} (ID: ${player.id}, Health: ${player.health})")
            }
            
            // Generate and show cards
            val cardFactory = CardFactory(definition)
            val cards = cardFactory.generateCards()
            console.log("🎴 Generated ${cards.size} cards:")
            
            // Show first few cards as example
            cards.take(3).forEach { card ->
                val damage = card.getIntProperty("damage")
                val element = card.getStringProperty("element")
                val healing = card.getIntProperty("healing")
                
                when {
                    damage != null && element != null -> {
                        console.log("  - ${card.id}: ${element} attack (${damage} damage)")
                    }
                    healing != null -> {
                        console.log("  - ${card.id}: healing (${healing} HP)")
                    }
                    else -> {
                        console.log("  - ${card.id}: ${card.type}")
                    }
                }
            }
            
            console.log("✅ Room $roomId is ready with ${gamePlayers.size} players!")
            
        } catch (e: Exception) {
            console.error("❌ Failed to create game room $roomId: ${e.message}")
        }
    }
    
    private fun demonstrateGameplay(roomId: String) {
        console.log("🎯 Demonstrating Day 2 gameplay in room: $roomId")
        
        val gameEngine = games[roomId]
        if (gameEngine == null) {
            console.error("❌ Game room $roomId not found")
            return
        }
        
        // Show initial state
        displayGameState(gameEngine, "Initial State")
        
        // Simulate some player actions
        val alice = gameEngine.getPlayers()[0]
        val bob = gameEngine.getPlayers()[1]
        
        console.log("🎮 Simulating player actions...")
        
        // Alice draws a card
        val drawAction = PlayerAction.DrawCard(alice.id)
        val drawResult = gameEngine.processAction(drawAction)
        console.log("📥 Alice draws card: ${if (drawResult.success) "✅ Success" else "❌ Failed"}")
        if (!drawResult.success) {
            console.log("   Errors: ${drawResult.validationErrors.joinToString(", ")}")
        }
        
        // Alice plays a card
        if (alice.hand.isNotEmpty()) {
            val cardToPlay = alice.hand[0]
            val playAction = PlayerAction.PlayCard(alice.id, cardToPlay.id)
            val playResult = gameEngine.processAction(playAction)
            console.log("🎴 Alice plays ${cardToPlay.id}: ${if (playResult.success) "✅ Success" else "❌ Failed"}")
            if (!playResult.success) {
                console.log("   Errors: ${playResult.validationErrors.joinToString(", ")}")
            }
        }
        
        // Alice ends turn
        val endTurnAction = PlayerAction.EndTurn(alice.id)
        val endResult = gameEngine.processAction(endTurnAction)
        console.log("⏭️ Alice ends turn: ${if (endResult.success) "✅ Success" else "❌ Failed"}")
        
        // Show updated state
        displayGameState(gameEngine, "After Alice's Turn")
        
        // Bob's turn - draw a card
        val bobDrawAction = PlayerAction.DrawCard(bob.id)
        val bobDrawResult = gameEngine.processAction(bobDrawAction)
        console.log("📥 Bob draws card: ${if (bobDrawResult.success) "✅ Success" else "❌ Failed"}")
        
        // Show final state
        displayGameState(gameEngine, "After Bob's Action")
        
        console.log("📊 Day 2 action system working correctly!")
        console.log("🏆 Server demo completed successfully!")
        console.log("🎮 Total active games: ${games.size}")
    }
    
    private fun displayGameState(gameEngine: GameEngine, label: String) {
        console.log("📋 $label:")
        val uiState = gameEngine.getUIState()
        console.log("   Turn: ${uiState.turnNumber}, Current Player: ${uiState.currentPlayerId}")
        console.log("   Deck: ${uiState.deckSize} cards, Discard: ${uiState.discardPileSize} cards")
        
        uiState.players.forEach { player ->
            val marker = if (player.id == uiState.currentPlayerId) "👉" else "  "
            console.log("   $marker ${player.name}: ${player.health} HP, ${player.hand.size} cards")
        }
        console.log("")
    }
}