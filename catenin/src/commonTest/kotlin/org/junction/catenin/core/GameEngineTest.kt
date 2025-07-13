package org.junction.catenin.core

import org.junction.catenin.model.GameDefinition
import org.junction.catenin.model.GameMeta
import org.junction.catenin.model.CardTypeDefinition
import org.junction.catenin.model.PropertyDefinition
import org.junction.catenin.model.GameMechanics
import org.junction.catenin.model.SetupMechanics
import org.junction.catenin.model.PlayerSetup
import org.junction.catenin.actions.*
import org.junction.catenin.model.GamePhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GameEngineTest {
    
    private fun createTestGameYaml() = """
        meta:
          name: "Test Game"
          target_age: [8, 12]
        
        cards:
          test_card:
            count: 10
            properties:
              value:
                type: int
                min: 1
                max: 5
          
          attack_card:
            count: 8
            properties:
              damage:
                type: int
                min: 2
                max: 4
              element:
                type: enum
                values: [fire, water]
        
        mechanics:
          setup:
            players:
              health: 10
              hand_size: 3
    """.trimIndent()
    
    @Test
    fun testGameEngineCreation() {
        val yaml = """
            meta:
              name: "Test Game"
              target_age: [8, 12]
            
            cards:
              number_card:
                count: 5
                properties:
                  value:
                    type: int
                    min: 1
                    max: 5
            
            mechanics:
              setup:
                players:
                  health: 20
        """.trimIndent()
        
        val engine = GameEngine.fromYaml(yaml, listOf("Alice", "Bob"))
        
        val definition = engine.getGameDefinition()
        assertEquals("Test Game", definition.meta.name)
        
        val players = engine.getPlayers()
        assertEquals(2, players.size)
        assertEquals("Alice", players[0].name)
        assertEquals("Bob", players[1].name)
        assertEquals("player_0", players[0].id)
        assertEquals("player_1", players[1].id)
        assertEquals(20, players[0].health)
        assertEquals(20, players[1].health)
    }
    
    @Test
    fun testGameEngineWithDefaultHealth() {
        val yaml = """
            meta:
              name: "Simple Game"
              target_age: [8, 12]
            
            cards:
              basic_card:
                count: 2
                properties:
                  power:
                    type: int
                    min: 1
                    max: 3
        """.trimIndent()
        
        val engine = GameEngine.fromYaml(yaml, listOf("Player1"))
        val players = engine.getPlayers()
        
        assertEquals(1, players.size)
        assertEquals(10, players[0].health) // Default health
    }
    
    @Test
    fun testGameStateInitialization() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val gameState = engine.getGameState()
        
        assertEquals(2, gameState.players.size)
        assertEquals("Alice", gameState.players[0].name)
        assertEquals("Bob", gameState.players[1].name)
        
        // Check initial setup
        gameState.players.forEach { player ->
            assertEquals(10, player.health)
            assertEquals(3, player.hand.size)
        }
        
        // Check game state
        assertEquals(GamePhase.PLAYING, gameState.gamePhase)
        assertEquals(1, gameState.turnNumber)
        assertEquals(0, gameState.currentPlayerIndex)
        assertEquals(12, gameState.getDeckSize()) // 18 cards - 6 in hands = 12
        assertEquals(0, gameState.getDiscardSize())
    }
    
    @Test
    fun testDrawCardAction() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val gameState = engine.getGameState()
        val player = gameState.players[0]
        val initialHandSize = player.hand.size
        val initialDeckSize = gameState.getDeckSize()
        
        val action = PlayerAction.DrawCard(player.id)
        val result = engine.processAction(action)
        
        assertTrue(result.success)
        assertEquals(ActionType.DRAW_CARD, result.type)
        assertEquals(1, result.effects.size)
        assertEquals(EffectType.CARD_DRAWN, result.effects[0].type)
        assertEquals(player.id, result.effects[0].targetPlayerId)
        
        // Check state changes - get updated player from game state
        val updatedPlayer = engine.getGameState().getPlayer(player.id)!!
        assertEquals(initialHandSize + 1, updatedPlayer.hand.size)
        assertEquals(initialDeckSize - 1, engine.getGameState().getDeckSize())
    }
    
    @Test
    fun testPlayCardAction() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val gameState = engine.getGameState()
        val player = gameState.players[0]
        val cardToPlay = player.hand.first()
        val initialHandSize = player.hand.size
        val initialDiscardSize = gameState.getDiscardSize()
        
        val action = PlayerAction.PlayCard(player.id, cardToPlay.id)
        val result = engine.processAction(action)
        
        assertTrue(result.success)
        assertEquals(ActionType.PLAY_CARD, result.type)
        assertEquals(1, result.effects.size)
        assertEquals(EffectType.CARD_PLAYED, result.effects[0].type)
        assertEquals(player.id, result.effects[0].targetPlayerId)
        assertEquals(cardToPlay.id, result.effects[0].sourceCardId)
        
        // Check state changes - get updated player from game state
        val updatedPlayer = engine.getGameState().getPlayer(player.id)!!
        assertEquals(initialHandSize - 1, updatedPlayer.hand.size)
        assertEquals(initialDiscardSize + 1, engine.getGameState().getDiscardSize())
        assertFalse(updatedPlayer.hasCard(cardToPlay.id))
    }
    
    @Test
    fun testEndTurnAction() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val gameState = engine.getGameState()
        val currentPlayer = gameState.getCurrentPlayer()
        
        assertEquals(0, gameState.currentPlayerIndex) // Alice's turn
        assertEquals(1, gameState.turnNumber)
        
        val action = PlayerAction.EndTurn(currentPlayer.id)
        val result = engine.processAction(action)
        
        assertTrue(result.success)
        assertEquals(ActionType.END_TURN, result.type)
        assertEquals(1, result.effects.size)
        assertEquals(EffectType.TURN_ENDED, result.effects[0].type)
        
        // Check turn change
        val newGameState = engine.getGameState()
        assertEquals(1, newGameState.currentPlayerIndex) // Bob's turn
        assertEquals(1, newGameState.turnNumber) // Still turn 1 until both players have played
        assertEquals("Bob", newGameState.getCurrentPlayer().name)
    }
    
    @Test
    fun testTurnCycle() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Alice ends turn
        var action = PlayerAction.EndTurn(engine.getCurrentPlayer().id)
        engine.processAction(action)
        assertEquals("Bob", engine.getCurrentPlayer().name)
        assertEquals(1, engine.getGameState().turnNumber)
        
        // Bob ends turn - should advance to turn 2
        action = PlayerAction.EndTurn(engine.getCurrentPlayer().id)
        engine.processAction(action)
        assertEquals("Alice", engine.getCurrentPlayer().name)
        assertEquals(2, engine.getGameState().turnNumber)
    }
    
    @Test
    fun testActionValidation() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val gameState = engine.getGameState()
        val aliceId = gameState.players[0].id
        val bobId = gameState.players[1].id
        
        // Test invalid player
        val invalidPlayerAction = PlayerAction.DrawCard("invalid_player")
        val result1 = engine.processAction(invalidPlayerAction)
        assertFalse(result1.success)
        assertTrue(result1.validationErrors.any { it.contains("Player not found") })
        
        // Test wrong turn
        val wrongTurnAction = PlayerAction.DrawCard(bobId) // Alice's turn, Bob tries to act
        val result2 = engine.processAction(wrongTurnAction)
        assertFalse(result2.success)
        assertTrue(result2.validationErrors.any { it.contains("Not your turn") })
        
        // Test invalid card
        val invalidCardAction = PlayerAction.PlayCard(aliceId, "invalid_card")
        val result3 = engine.processAction(invalidCardAction)
        assertFalse(result3.success)
        assertTrue(result3.validationErrors.any { it.contains("not found in") || it.contains("Card not in hand") })
    }
    
    // Removed testDrawCardWhenDeckEmpty - covered by GameEngineQueryTest.testCanPlayerDrawCardWithEmptyDeck
    
    @Test
    fun testHandLimit() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val player = engine.getCurrentPlayer()
        
        // Draw cards until hand is full (limit 10)
        while (player.hand.size < 10 && engine.getGameState().getDeckSize() > 0) {
            val action = PlayerAction.DrawCard(player.id)
            val result = engine.processAction(action)
            if (!result.success) break
        }
        
        // Try to draw when hand is full
        if (player.hand.size >= 10) {
            val action = PlayerAction.DrawCard(player.id)
            val result = engine.processAction(action)
            assertFalse(result.success)
            assertTrue(result.validationErrors.any { it.contains("Hand is full") })
        }
    }
    
    // Removed testUIState - covered by GameEngineQueryTest.testQueryMethodConsistencyWithUIState
    
    @Test
    fun testValidationResult() {
        // Test valid result
        val valid = ValidationResult.valid()
        assertTrue(valid.isValid)
        assertEquals(0, valid.errors.size)
        
        // Test invalid result
        val invalid = ValidationResult.invalid("Error 1", "Error 2")
        assertFalse(invalid.isValid)
        assertEquals(2, invalid.errors.size)
        assertEquals("Error 1", invalid.errors[0])
        assertEquals("Error 2", invalid.errors[1])
    }
    
    @Test
    fun testActionResultFactoryMethods() {
        // Test success
        val success = ActionResult.success(ActionType.DRAW_CARD)
        assertTrue(success.success)
        assertEquals(ActionType.DRAW_CARD, success.type)
        assertEquals(0, success.effects.size)
        assertEquals(0, success.validationErrors.size)
        
        // Test failure
        val failure = ActionResult.failure(ActionType.PLAY_CARD, arrayOf("Error"))
        assertFalse(failure.success)
        assertEquals(ActionType.PLAY_CARD, failure.type)
        assertEquals(0, failure.effects.size)
        assertEquals(1, failure.validationErrors.size)
        assertEquals("Error", failure.validationErrors[0])
        
        // Test with effects
        val effect = GameEffect(EffectType.CARD_DRAWN, "player_0", description = "Test effect")
        val withEffects = ActionResult.success(ActionType.DRAW_CARD, arrayOf(effect))
        assertTrue(withEffects.success)
        assertEquals(ActionType.DRAW_CARD, withEffects.type)
        assertEquals(1, withEffects.effects.size)
        assertEquals(EffectType.CARD_DRAWN, withEffects.effects[0].type)
    }
}