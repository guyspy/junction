package org.junction.catenin.model

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
@JsExport
data class GameState(
    val gameId: String,
    val definition: GameDefinition,
    val players: Array<Player>,
    val deck: Array<Card>,
    val discardPile: Array<Card>,
    val currentPlayerIndex: Int,
    val gamePhase: GamePhase,
    val turnNumber: Int
) {
    fun getCurrentPlayer(): Player = players[currentPlayerIndex]
    
    fun getPlayer(playerId: String): Player? = players.find { it.id == playerId }
    
    fun isGameOver(): Boolean = gamePhase == GamePhase.FINISHED
    
    // JavaScript-friendly copy methods for immutable updates
    fun withNextPlayer(): GameState {
        val nextIndex = (currentPlayerIndex + 1) % players.size
        val nextTurn = if (nextIndex == 0) turnNumber + 1 else turnNumber
        return copy(currentPlayerIndex = nextIndex, turnNumber = nextTurn)
    }
    
    fun withRemovedFromDeck(count: Int): GameState {
        return copy(deck = deck.drop(count).toTypedArray())
    }
    
    fun withAddedToDiscard(cards: Array<Card>): GameState {
        return copy(discardPile = discardPile + cards)
    }
    
    fun withGamePhase(newPhase: GamePhase): GameState {
        return copy(gamePhase = newPhase)
    }
    
    // Helper to get deck size
    fun getDeckSize(): Int = deck.size
    
    // Helper to get discard pile size  
    fun getDiscardSize(): Int = discardPile.size
    
    // Update a specific player and return new game state
    fun withUpdatedPlayer(updatedPlayer: Player): GameState {
        val newPlayers = players.map { player ->
            if (player.id == updatedPlayer.id) updatedPlayer else player
        }.toTypedArray()
        return copy(players = newPlayers)
    }
}

@Serializable
@JsExport
enum class GamePhase {
    SETUP,
    PLAYING,
    FINISHED
}