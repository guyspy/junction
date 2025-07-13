package org.junction.catenin.core

import org.junction.catenin.actions.PlayerAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GameEngineQueryTest {
    
    private fun createTestGameYaml() = """
        meta:
          name: "Query Test Game"
          target_age: [8, 12]
        
        cards:
          attack_card:
            count: 6
            properties:
              damage:
                type: int
                min: 2
                max: 4
              element:
                type: enum
                values: [fire, water]
          
          heal_card:
            count: 4
            properties:
              healing:
                type: int
                min: 1
                max: 3
        
        mechanics:
          setup:
            players:
              health: 20
              hand_size: 3
    """.trimIndent()
    
    @Test
    fun testCanPlayerDrawCard() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        val bob = engine.getPlayers()[1]
        
        // Alice should be able to draw (it's her turn and deck has cards)
        assertTrue(engine.canPlayerDrawCard(alice.id))
        
        // Bob should not be able to draw (not his turn)
        assertFalse(engine.canPlayerDrawCard(bob.id))
        
        // Non-existent player should not be able to draw
        assertFalse(engine.canPlayerDrawCard("invalid_player"))
    }
    
    @Test
    fun testCanPlayerDrawCardWithEmptyDeck() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        
        // Draw all cards from deck
        while (engine.getRemainingDeckSize() > 0) {
            val result = engine.processAction(PlayerAction.DrawCard(alice.id))
            if (!result.success) break
        }
        
        // Should not be able to draw when deck is empty
        assertFalse(engine.canPlayerDrawCard(alice.id))
    }
    
    @Test
    fun testCanPlayerPlayCard() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        val bob = engine.getPlayers()[1]
        
        // Alice should be able to play cards from her hand
        alice.hand.forEach { card ->
            assertTrue(engine.canPlayerPlayCard(alice.id, card.id))
        }
        
        // Bob should not be able to play cards (not his turn)
        bob.hand.forEach { card ->
            assertFalse(engine.canPlayerPlayCard(bob.id, card.id))
        }
        
        // Alice should not be able to play cards not in her hand
        assertFalse(engine.canPlayerPlayCard(alice.id, "non_existent_card"))
        
        // Non-existent player should not be able to play
        if (alice.hand.isNotEmpty()) {
            assertFalse(engine.canPlayerPlayCard("invalid_player", alice.hand[0].id))
        }
    }
    
    @Test
    fun testCanPlayerEndTurn() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        val bob = engine.getPlayers()[1]
        
        // Alice should be able to end her turn
        assertTrue(engine.canPlayerEndTurn(alice.id))
        
        // Bob should not be able to end turn (not his turn)
        assertFalse(engine.canPlayerEndTurn(bob.id))
        
        // Non-existent player should not be able to end turn
        assertFalse(engine.canPlayerEndTurn("invalid_player"))
    }
    
    @Test
    fun testGetPlayableCards() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        val bob = engine.getPlayers()[1]
        
        // Alice should be able to play all her cards (it's her turn)
        val alicePlayableCards = engine.getPlayableCards(alice.id)
        assertEquals(alice.hand.size, alicePlayableCards.size)
        
        alice.hand.forEach { card ->
            assertTrue(alicePlayableCards.contains(card))
        }
        
        // Bob should not be able to play any cards (not his turn)
        val bobPlayableCards = engine.getPlayableCards(bob.id)
        assertEquals(0, bobPlayableCards.size)
        
        // Non-existent player should have no playable cards
        val invalidPlayableCards = engine.getPlayableCards("invalid_player")
        assertEquals(0, invalidPlayableCards.size)
    }
    
    @Test
    fun testGetPlayerHandSize() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        val bob = engine.getPlayers()[1]
        
        // Check initial hand sizes
        assertEquals(3, engine.getPlayerHandSize(alice.id))
        assertEquals(3, engine.getPlayerHandSize(bob.id))
        
        // Draw a card and check hand size
        engine.processAction(PlayerAction.DrawCard(alice.id))
        assertEquals(4, engine.getPlayerHandSize(alice.id))
        assertEquals(3, engine.getPlayerHandSize(bob.id)) // Bob unchanged
        
        // Play a card and check hand size
        if (alice.hand.isNotEmpty()) {
            engine.processAction(PlayerAction.PlayCard(alice.id, alice.hand[0].id))
            assertEquals(3, engine.getPlayerHandSize(alice.id))
        }
        
        // Non-existent player should return 0
        assertEquals(0, engine.getPlayerHandSize("invalid_player"))
    }
    
    @Test
    fun testGetPlayerHealth() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        val bob = engine.getPlayers()[1]
        
        // Check initial health
        assertEquals(20, engine.getPlayerHealth(alice.id))
        assertEquals(20, engine.getPlayerHealth(bob.id))
        
        // Note: Health changes would require event system (Day 3)
        // For now just test that the method works correctly
        
        // Non-existent player should return 0
        assertEquals(0, engine.getPlayerHealth("invalid_player"))
    }
    
    @Test
    fun testGetPlayerScore() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        val bob = engine.getPlayers()[1]
        
        // Check initial scores
        assertEquals(0, engine.getPlayerScore(alice.id))
        assertEquals(0, engine.getPlayerScore(bob.id))
        
        // Note: Score changes would require event system (Day 3)
        // For now just test that the method works correctly
        
        // Non-existent player should return 0
        assertEquals(0, engine.getPlayerScore("invalid_player"))
    }
    
    @Test
    fun testIsPlayerAlive() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        val bob = engine.getPlayers()[1]
        
        // All players should be alive initially
        assertTrue(engine.isPlayerAlive(alice.id))
        assertTrue(engine.isPlayerAlive(bob.id))
        
        // Note: Player death would require event system (Day 3)
        // For now just test that the method works correctly
        
        // Non-existent player should return false
        assertFalse(engine.isPlayerAlive("invalid_player"))
    }
    
    @Test
    fun testIsPlayerTurn() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        val bob = engine.getPlayers()[1]
        
        // Initially it should be Alice's turn (player 0)
        assertTrue(engine.isPlayerTurn(alice.id))
        assertFalse(engine.isPlayerTurn(bob.id))
        
        // End Alice's turn
        engine.processAction(PlayerAction.EndTurn(alice.id))
        
        // Now it should be Bob's turn
        assertFalse(engine.isPlayerTurn(alice.id))
        assertTrue(engine.isPlayerTurn(bob.id))
        
        // Non-existent player should return false
        assertFalse(engine.isPlayerTurn("invalid_player"))
    }
    
    @Test
    fun testGetRemainingDeckSize() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Initial deck size should be total cards minus dealt cards
        // 10 total cards - 6 dealt (3 to each player) = 4 remaining
        assertEquals(4, engine.getRemainingDeckSize())
        
        // Draw a card
        val alice = engine.getPlayers()[0]
        engine.processAction(PlayerAction.DrawCard(alice.id))
        
        // Deck should be smaller
        assertEquals(3, engine.getRemainingDeckSize())
    }
    
    @Test
    fun testGetDiscardPileSize() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Initially discard pile should be empty
        assertEquals(0, engine.getDiscardPileSize())
        
        // Play a card
        val alice = engine.getPlayers()[0]
        if (alice.hand.isNotEmpty()) {
            engine.processAction(PlayerAction.PlayCard(alice.id, alice.hand[0].id))
            
            // Discard pile should have one card
            assertEquals(1, engine.getDiscardPileSize())
        }
    }
    
    @Test
    fun testGetTurnNumber() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Initially should be turn 1
        assertEquals(1, engine.getTurnNumber())
        
        // End Alice's turn (still turn 1, Bob's turn now)
        val alice = engine.getPlayers()[0]
        engine.processAction(PlayerAction.EndTurn(alice.id))
        
        // Still turn 1 - only increments when we cycle back to first player
        assertEquals(1, engine.getTurnNumber())
        
        // End Bob's turn (back to Alice, should advance to turn 2)
        val bob = engine.getPlayers()[1]
        engine.processAction(PlayerAction.EndTurn(bob.id))
        
        // Now should advance to turn 2
        assertEquals(2, engine.getTurnNumber())
    }
    
    @Test
    fun testGetGamePhase() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        
        // Should start in PLAYING phase
        assertEquals("PLAYING", engine.getGamePhase())
        
        // Note: Phase changes would require win conditions (Day 5)
        // For now just test that the method works correctly
    }
    
    @Test
    fun testQueryMethodsAfterMultipleActions() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val alice = engine.getPlayers()[0]
        val bob = engine.getPlayers()[1]
        
        // Perform a sequence of actions
        engine.processAction(PlayerAction.DrawCard(alice.id))
        engine.processAction(PlayerAction.PlayCard(alice.id, alice.hand[0].id))
        engine.processAction(PlayerAction.EndTurn(alice.id))
        
        // Verify state after actions
        assertEquals(1, engine.getDiscardPileSize())
        assertEquals(3, engine.getRemainingDeckSize())
        assertEquals(1, engine.getTurnNumber()) // Still turn 1 - only Bob has had a turn
        assertTrue(engine.isPlayerTurn(bob.id))
        assertFalse(engine.isPlayerTurn(alice.id))
        
        // Alice should have 3 cards (started with 3, drew 1, played 1)
        assertEquals(3, engine.getPlayerHandSize(alice.id))
        
        // Bob should be able to act now
        assertTrue(engine.canPlayerDrawCard(bob.id))
        assertTrue(engine.canPlayerEndTurn(bob.id))
        
        bob.hand.forEach { card ->
            assertTrue(engine.canPlayerPlayCard(bob.id, card.id))
        }
    }
    
    @Test
    fun testQueryMethodConsistencyWithUIState() {
        val engine = GameEngine.fromYaml(createTestGameYaml(), listOf("Alice", "Bob"))
        val uiState = engine.getUIState()
        
        // Query methods should be consistent with UI state
        assertEquals(uiState.turnNumber, engine.getTurnNumber())
        assertEquals(uiState.deckSize, engine.getRemainingDeckSize())
        assertEquals(uiState.discardPileSize, engine.getDiscardPileSize())
        assertEquals(uiState.gamePhase, engine.getGamePhase())
        
        val currentPlayer = engine.getCurrentPlayer()
        assertEquals(uiState.currentPlayerId, currentPlayer.id)
        assertTrue(engine.isPlayerTurn(currentPlayer.id))
    }
}