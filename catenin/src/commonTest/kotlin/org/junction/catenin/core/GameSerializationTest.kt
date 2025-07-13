package org.junction.catenin.core

import org.junction.catenin.actions.PlayerAction
import org.junction.catenin.model.GamePhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GameSerializationTest {
    
    private fun createTestGameYaml() = """
        meta:
          name: "Serialization Test Game"
          target_age: [8, 12]
        
        cards:
          test_card:
            count: 8
            properties:
              value:
                type: int
                min: 1
                max: 5
              color:
                type: enum
                values: [red, blue, green]
        
        mechanics:
          setup:
            players:
              health: 15
              hand_size: 3
    """.trimIndent()
    
    @Test
    fun testBasicGameStateSerialization() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Serialize the game state
        val serializedState = engine.saveGameState()
        assertNotNull(serializedState)
        assertTrue(serializedState.isNotEmpty())
        
        // Should be valid JSON
        assertTrue(serializedState.startsWith("{"))
        assertTrue(serializedState.endsWith("}"))
    }
    
    @Test
    fun testGameStateDeserialization() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Get initial state
        val originalState = engine.getGameState()
        val serializedState = engine.saveGameState()
        
        // Load from serialized state
        val loadedEngine = GameEngine.loadGameState(serializedState)
        val loadedState = loadedEngine.getGameState()
        
        // Verify core properties are preserved
        assertEquals(originalState.gameId, loadedState.gameId)
        assertEquals(originalState.players.size, loadedState.players.size)
        assertEquals(originalState.currentPlayerIndex, loadedState.currentPlayerIndex)
        assertEquals(originalState.gamePhase, loadedState.gamePhase)
        assertEquals(originalState.turnNumber, loadedState.turnNumber)
        assertEquals(originalState.getDeckSize(), loadedState.getDeckSize())
        assertEquals(originalState.getDiscardSize(), loadedState.getDiscardSize())
    }
    
    @Test
    fun testPlayerStateSerialization() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val originalState = engine.getGameState()
        
        // Serialize and deserialize
        val serializedState = engine.saveGameState()
        val loadedEngine = GameEngine.loadGameState(serializedState)
        val loadedState = loadedEngine.getGameState()
        
        // Check player details
        for (i in originalState.players.indices) {
            val originalPlayer = originalState.players[i]
            val loadedPlayer = loadedState.players[i]
            
            assertEquals(originalPlayer.id, loadedPlayer.id)
            assertEquals(originalPlayer.name, loadedPlayer.name)
            assertEquals(originalPlayer.health, loadedPlayer.health)
            assertEquals(originalPlayer.score, loadedPlayer.score)
            assertEquals(originalPlayer.hand.size, loadedPlayer.hand.size)
            
            // Check each card in hand
            for (j in originalPlayer.hand.indices) {
                val originalCard = originalPlayer.hand[j]
                val loadedCard = loadedPlayer.hand[j]
                
                assertEquals(originalCard.id, loadedCard.id)
                assertEquals(originalCard.type, loadedCard.type)
                assertEquals(originalCard.properties.size, loadedCard.properties.size)
            }
        }
    }
    
    @Test
    fun testGameDefinitionSerialization() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val originalDefinition = engine.getGameDefinition()
        
        // Serialize and deserialize
        val serializedState = engine.saveGameState()
        val loadedEngine = GameEngine.loadGameState(serializedState)
        val loadedDefinition = loadedEngine.getGameDefinition()
        
        // Check game definition preservation
        assertEquals(originalDefinition.meta.name, loadedDefinition.meta.name)
        assertEquals(originalDefinition.meta.targetAge, loadedDefinition.meta.targetAge)
        assertEquals(originalDefinition.cards.size, loadedDefinition.cards.size)
        
        // Check card type definitions
        originalDefinition.cards.forEach { (cardType, originalCardDef) ->
            val loadedCardDef = loadedDefinition.cards[cardType]
            assertNotNull(loadedCardDef)
            assertEquals(originalCardDef.count, loadedCardDef.count)
            assertEquals(originalCardDef.properties.size, loadedCardDef.properties.size)
        }
    }
    
    @Test
    fun testSerializationAfterActions() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val player = engine.getPlayers()[0]
        
        // Perform some actions
        val drawResult = engine.processAction(PlayerAction.DrawCard(player.id))
        assertTrue(drawResult.success)
        
        if (player.hand.isNotEmpty()) {
            val playResult = engine.processAction(PlayerAction.PlayCard(player.id, player.hand[0].id))
            assertTrue(playResult.success)
        }
        
        val endResult = engine.processAction(PlayerAction.EndTurn(player.id))
        assertTrue(endResult.success)
        
        // Get modified state
        val modifiedState = engine.getGameState()
        
        // Serialize and deserialize
        val serializedState = engine.saveGameState()
        val loadedEngine = GameEngine.loadGameState(serializedState)
        val loadedState = loadedEngine.getGameState()
        
        // Verify state after actions is preserved
        assertEquals(modifiedState.currentPlayerIndex, loadedState.currentPlayerIndex)
        assertEquals(modifiedState.turnNumber, loadedState.turnNumber)
        assertEquals(modifiedState.getDeckSize(), loadedState.getDeckSize())
        assertEquals(modifiedState.getDiscardSize(), loadedState.getDiscardSize())
    }
    
    @Test
    fun testSerializationPreservesGamePhase() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Verify initial phase
        assertEquals(GamePhase.PLAYING, engine.getGameState().gamePhase)
        
        // Serialize and deserialize
        val serializedState = engine.saveGameState()
        val loadedEngine = GameEngine.loadGameState(serializedState)
        
        // Verify phase is preserved
        assertEquals(GamePhase.PLAYING, loadedEngine.getGameState().gamePhase)
    }
    
    @Test
    fun testSerializationPreservesCardProperties() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val originalState = engine.getGameState()
        
        // Find a card with properties
        val originalCard = originalState.players[0].hand[0]
        
        // Serialize and deserialize
        val serializedState = engine.saveGameState()
        val loadedEngine = GameEngine.loadGameState(serializedState)
        val loadedState = loadedEngine.getGameState()
        
        // Find the same card
        val loadedCard = loadedState.players[0].hand[0]
        
        // Verify card properties are preserved
        assertEquals(originalCard.id, loadedCard.id)
        assertEquals(originalCard.type, loadedCard.type)
        
        val originalValue = originalCard.getIntProperty("value")
        val loadedValue = loadedCard.getIntProperty("value")
        assertEquals(originalValue, loadedValue)
        
        val originalColor = originalCard.getStringProperty("color")
        val loadedColor = loadedCard.getStringProperty("color")
        assertEquals(originalColor, loadedColor)
    }
    
    @Test
    fun testSerializationWithEmptyDeck() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Draw all cards from deck
        val player = engine.getPlayers()[0]
        while (engine.getRemainingDeckSize() > 0) {
            val result = engine.processAction(PlayerAction.DrawCard(player.id))
            if (!result.success) break
        }
        
        // Verify deck is empty
        assertEquals(0, engine.getRemainingDeckSize())
        
        // Serialize and deserialize
        val serializedState = engine.saveGameState()
        val loadedEngine = GameEngine.loadGameState(serializedState)
        
        // Verify empty deck is preserved
        assertEquals(0, loadedEngine.getRemainingDeckSize())
    }
    
    @Test
    fun testSerializationWithFullDiscardPile() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val player = engine.getPlayers()[0]
        
        // Play all cards from hand
        while (player.hand.isNotEmpty()) {
            val card = player.hand[0]
            val result = engine.processAction(PlayerAction.PlayCard(player.id, card.id))
            if (!result.success) break
        }
        
        val discardSize = engine.getDiscardPileSize()
        assertTrue(discardSize > 0)
        
        // Serialize and deserialize
        val serializedState = engine.saveGameState()
        val loadedEngine = GameEngine.loadGameState(serializedState)
        
        // Verify discard pile is preserved
        assertEquals(discardSize, loadedEngine.getDiscardPileSize())
    }
    
    @Test
    fun testAlternativeSerializationMethods() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Test serialization method
        val serializedState1 = engine.saveGameState()
        val serializedState2 = engine.saveGameState()
        
        assertEquals(serializedState1, serializedState2)
        
        // Test loading method
        val loadedEngine1 = GameEngine.loadGameState(serializedState1)
        val loadedEngine2 = GameEngine.loadGameState(serializedState1)
        
        // Should produce equivalent engines
        assertEquals(
            loadedEngine1.getGameState().gameId, 
            loadedEngine2.getGameState().gameId
        )
    }
    
    @Test
    fun testSerializationIdentity() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Serialize, deserialize, then serialize again
        val serialized1 = engine.saveGameState()
        val loadedEngine = GameEngine.loadGameState(serialized1)
        val serialized2 = loadedEngine.saveGameState()
        
        // Second serialization should be identical to first
        assertEquals(serialized1, serialized2)
    }
}