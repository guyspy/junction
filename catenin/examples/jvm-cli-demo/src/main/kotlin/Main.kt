package org.junction.catenin

import org.junction.catenin.core.GameEngine
import org.junction.catenin.actions.PlayerAction
import org.junction.catenin.model.Player

fun main() {
    println("=== Catenin Card Game - Day 2 Interactive Demo ===")
    
    try {
        val gameYaml = """
            meta:
              name: "Day 2 Interactive Card Game"
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
        
        println("Enter number of players (2-4):")
        val playerCount = readLine()?.toIntOrNull() ?: 2
        
        val playerNames = mutableListOf<String>()
        repeat(playerCount) { index ->
            println("Enter name for player ${index + 1}:")
            val name = readLine() ?: "Player${index + 1}"
            playerNames.add(name)
        }
        
        val engine = GameEngine.fromYaml(gameYaml, playerNames)
        println("\n✅ Game initialized!")
        println("Game: ${engine.getGameDefinition().meta.name}")
        println("Players: ${playerNames.joinToString(", ")}")
        println()
        
        gameLoop(engine)
        
    } catch (e: Exception) {
        println("❌ Game failed: ${e.message}")
        e.printStackTrace()
    }
}

fun gameLoop(engine: GameEngine) {
    while (!engine.getGameState().isGameOver()) {
        displayGameState(engine)
        handlePlayerTurn(engine)
    }
}

fun displayGameState(engine: GameEngine) {
    val uiState = engine.getUIState()
    val gameState = engine.getGameState()
    
    println("=".repeat(50))
    println("Turn ${uiState.turnNumber}")
    println("Deck: ${uiState.deckSize} cards remaining")
    println("Discard pile: ${uiState.discardPileSize} cards")
    println()
    
    // Display all players
    gameState.players.forEach { player ->
        val isCurrent = player.id == uiState.currentPlayerId
        val marker = if (isCurrent) "👉 " else "   "
        
        println("$marker${player.name} (Health: ${player.health}, Hand: ${player.hand.size} cards)")
        
        if (isCurrent) {
            displayPlayerHand(player)
        }
    }
    println()
}

fun displayPlayerHand(player: Player) {
    if (player.hand.isEmpty()) {
        println("     Hand: Empty")
        return
    }
    
    println("     Hand:")
    player.hand.forEachIndexed { index, card ->
        val damage = card.getIntProperty("damage")
        val element = card.getStringProperty("element")
        val healing = card.getIntProperty("healing")
        
        val description = when {
            damage != null && element != null -> "$element attack ($damage damage)"
            healing != null -> "heal ($healing health)"
            else -> {
                val value = card.getIntProperty("value")
                val color = card.getStringProperty("color")
                "$color number $value"
            }
        }
        
        println("       $index) ${card.id}: $description")
    }
}

fun handlePlayerTurn(engine: GameEngine) {
    val currentPlayer = engine.getCurrentPlayer()
    println("${currentPlayer.name}'s turn")
    println("Choose action:")
    println("1) Draw card")
    println("2) Play card") 
    println("3) End turn")
    print("Enter choice (1-3): ")
    
    when (readLine()) {
        "1" -> handleDrawCard(engine, currentPlayer)
        "2" -> handlePlayCard(engine, currentPlayer)
        "3" -> handleEndTurn(engine, currentPlayer)
        else -> {
            println("Invalid choice, try again")
            handlePlayerTurn(engine)
        }
    }
}

fun handleDrawCard(engine: GameEngine, player: Player) {
    val action = PlayerAction.DrawCard(player.id)
    val result = engine.processAction(action)
    
    if (result.success) {
        println("✅ ${player.name} drew a card")
        val lastCard = player.hand.lastOrNull()
        if (lastCard != null) {
            println("   Drew: ${lastCard.id}")
        }
        // Continue current player's turn
        handlePlayerTurn(engine)
    } else {
        println("❌ ${result.validationErrors.joinToString(", ")}")
        handlePlayerTurn(engine)
    }
}

fun handlePlayCard(engine: GameEngine, player: Player) {
    if (player.hand.isEmpty()) {
        println("No cards in hand")
        handlePlayerTurn(engine)
        return
    }
    
    print("Choose card to play (0-${player.hand.size - 1}): ")
    val input = readLine()
    val cardIndex = input?.toIntOrNull()
    
    if (cardIndex == null || cardIndex !in 0 until player.hand.size) {
        println("Invalid choice")
        handlePlayerTurn(engine)
        return
    }
    
    val card = player.hand[cardIndex]
    val action = PlayerAction.PlayCard(player.id, card.id)
    val result = engine.processAction(action)
    
    if (result.success) {
        println("✅ ${player.name} played ${card.id}")
        // Continue current player's turn
        handlePlayerTurn(engine)
    } else {
        println("❌ ${result.validationErrors.joinToString(", ")}")
        handlePlayerTurn(engine)
    }
}

fun handleEndTurn(engine: GameEngine, player: Player) {
    val action = PlayerAction.EndTurn(player.id)
    val result = engine.processAction(action)
    
    println("✅ ${player.name} ended turn")
    println()
}