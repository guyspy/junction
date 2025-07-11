package org.junction.catenin.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull

class GameStateTest {
    
    private fun createTestGameDefinition(): GameDefinition {
        return GameDefinition(
            meta = GameMeta(name = "Test Game", targetAge = listOf(8, 12)),
            cards = mapOf(
                "test_card" to CardTypeDefinition(
                    count = 5,
                    properties = mapOf(
                        "value" to PropertyDefinition(type = "int", min = 1, max = 5)
                    )
                )
            )
        )
    }
    
    private fun createTestPlayers(): Array<Player> {
        return arrayOf(
            Player(id = "player_0", name = "Alice", health = 10),
            Player(id = "player_1", name = "Bob", health = 10)
        )
    }
    
    private fun createTestCards(): Array<Card> {
        return arrayOf(
            Card("card_1", "test_card", mapOf("value" to CardPropertyValue.IntValue(3))),
            Card("card_2", "test_card", mapOf("value" to CardPropertyValue.IntValue(1))),
            Card("card_3", "test_card", mapOf("value" to CardPropertyValue.IntValue(5)))
        )
    }
    
    @Test
    fun testGameStateCreation() {
        val definition = createTestGameDefinition()
        val players = createTestPlayers()
        val deck = createTestCards()
        val discard = emptyArray<Card>()
        
        val gameState = GameState(
            gameId = "test_game",
            definition = definition,
            players = players,
            deck = deck,
            discardPile = discard,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.PLAYING,
            turnNumber = 1
        )
        
        assertEquals("test_game", gameState.gameId)
        assertEquals(definition, gameState.definition)
        assertEquals(2, gameState.players.size)
        assertEquals(3, gameState.deck.size)
        assertEquals(0, gameState.discardPile.size)
        assertEquals(0, gameState.currentPlayerIndex)
        assertEquals(GamePhase.PLAYING, gameState.gamePhase)
        assertEquals(1, gameState.turnNumber)
    }
    
    @Test
    fun testGetCurrentPlayer() {
        val gameState = GameState(
            gameId = "test",
            definition = createTestGameDefinition(),
            players = createTestPlayers(),
            deck = createTestCards(),
            discardPile = emptyArray(),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.PLAYING,
            turnNumber = 1
        )
        
        val currentPlayer = gameState.getCurrentPlayer()
        assertEquals("player_1", currentPlayer.id)
        assertEquals("Bob", currentPlayer.name)
    }
    
    @Test
    fun testGetPlayer() {
        val gameState = GameState(
            gameId = "test",
            definition = createTestGameDefinition(),
            players = createTestPlayers(),
            deck = createTestCards(),
            discardPile = emptyArray(),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.PLAYING,
            turnNumber = 1
        )
        
        val alice = gameState.getPlayer("player_0")
        assertEquals("Alice", alice?.name)
        
        val bob = gameState.getPlayer("player_1")
        assertEquals("Bob", bob?.name)
        
        val notFound = gameState.getPlayer("invalid_player")
        assertNull(notFound)
    }
    
    @Test
    fun testIsGameOver() {
        val gameState = GameState(
            gameId = "test",
            definition = createTestGameDefinition(),
            players = createTestPlayers(),
            deck = createTestCards(),
            discardPile = emptyArray(),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.PLAYING,
            turnNumber = 1
        )
        
        assertFalse(gameState.isGameOver())
        
        val finishedGame = gameState.withGamePhase(GamePhase.FINISHED)
        assertTrue(finishedGame.isGameOver())
    }
    
    @Test
    fun testWithNextPlayer() {
        val gameState = GameState(
            gameId = "test",
            definition = createTestGameDefinition(),
            players = createTestPlayers(),
            deck = createTestCards(),
            discardPile = emptyArray(),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.PLAYING,
            turnNumber = 1
        )
        
        // Test advancing to next player
        val nextState = gameState.withNextPlayer()
        assertEquals(1, nextState.currentPlayerIndex)
        assertEquals(1, nextState.turnNumber) // Turn doesn't advance until back to player 0
        
        // Test wrapping around to player 0 (should advance turn)
        val wrapState = nextState.withNextPlayer()
        assertEquals(0, wrapState.currentPlayerIndex)
        assertEquals(2, wrapState.turnNumber) // Now turn advances
    }
    
    @Test
    fun testWithRemovedFromDeck() {
        val gameState = GameState(
            gameId = "test",
            definition = createTestGameDefinition(),
            players = createTestPlayers(),
            deck = createTestCards(),
            discardPile = emptyArray(),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.PLAYING,
            turnNumber = 1
        )
        
        assertEquals(3, gameState.getDeckSize())
        
        val newState = gameState.withRemovedFromDeck(1)
        assertEquals(2, newState.getDeckSize())
        
        val emptyDeck = gameState.withRemovedFromDeck(3)
        assertEquals(0, emptyDeck.getDeckSize())
    }
    
    @Test
    fun testWithAddedToDiscard() {
        val gameState = GameState(
            gameId = "test",
            definition = createTestGameDefinition(),
            players = createTestPlayers(),
            deck = createTestCards(),
            discardPile = emptyArray(),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.PLAYING,
            turnNumber = 1
        )
        
        assertEquals(0, gameState.getDiscardSize())
        
        val cardsToAdd = arrayOf(
            Card("discard_1", "test_card", mapOf("value" to CardPropertyValue.IntValue(2)))
        )
        
        val newState = gameState.withAddedToDiscard(cardsToAdd)
        assertEquals(1, newState.getDiscardSize())
        
        val moreCards = arrayOf(
            Card("discard_2", "test_card", mapOf("value" to CardPropertyValue.IntValue(4))),
            Card("discard_3", "test_card", mapOf("value" to CardPropertyValue.IntValue(1)))
        )
        
        val finalState = newState.withAddedToDiscard(moreCards)
        assertEquals(3, finalState.getDiscardSize())
    }
    
    @Test
    fun testWithGamePhase() {
        val gameState = GameState(
            gameId = "test",
            definition = createTestGameDefinition(),
            players = createTestPlayers(),
            deck = createTestCards(),
            discardPile = emptyArray(),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.SETUP,
            turnNumber = 1
        )
        
        assertEquals(GamePhase.SETUP, gameState.gamePhase)
        
        val playingState = gameState.withGamePhase(GamePhase.PLAYING)
        assertEquals(GamePhase.PLAYING, playingState.gamePhase)
        
        val finishedState = playingState.withGamePhase(GamePhase.FINISHED)
        assertEquals(GamePhase.FINISHED, finishedState.gamePhase)
        assertTrue(finishedState.isGameOver())
    }
    
    @Test
    fun testHelperMethods() {
        val gameState = GameState(
            gameId = "test",
            definition = createTestGameDefinition(),
            players = createTestPlayers(),
            deck = createTestCards(),
            discardPile = arrayOf(
                Card("discard_1", "test_card", mapOf("value" to CardPropertyValue.IntValue(2)))
            ),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.PLAYING,
            turnNumber = 1
        )
        
        assertEquals(3, gameState.getDeckSize())
        assertEquals(1, gameState.getDiscardSize())
    }
    
    @Test
    fun testGamePhaseEnum() {
        val setupPhase = GamePhase.SETUP
        val playingPhase = GamePhase.PLAYING
        val finishedPhase = GamePhase.FINISHED
        
        assertEquals("SETUP", setupPhase.name)
        assertEquals("PLAYING", playingPhase.name)
        assertEquals("FINISHED", finishedPhase.name)
    }
}