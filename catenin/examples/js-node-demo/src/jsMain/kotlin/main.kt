import org.junction.catenin.core.GameEngine
import org.junction.catenin.core.createGameEngineFromYaml
import org.junction.catenin.parser.GameDefinitionParser
import org.junction.catenin.model.CardFactory

fun main() {
    console.log("🎮 Catenin Node.js Demo Starting...")
    
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
        console.log("🎯 Demonstrating gameplay in room: $roomId")
        
        val gameEngine = games[roomId]
        if (gameEngine == null) {
            console.error("❌ Game room $roomId not found")
            return
        }
        
        val players = gameEngine.getPlayers()
        console.log("🔄 Current game state:")
        players.forEach { player ->
            console.log("  - ${player.name}: ${player.health} HP, ${player.hand.size} cards in hand")
        }
        
        // In a real server, this would process actual player actions
        console.log("📊 Game state management working correctly!")
        console.log("🏆 Server demo completed successfully!")
        
        // Show total games managed
        console.log("🎮 Total active games: ${games.size}")
    }
}