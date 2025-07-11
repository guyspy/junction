package org.junction.catenin.events

import org.junction.catenin.model.*
import org.junction.catenin.actions.GameEffect
import kotlin.js.JsExport

/**
 * Interface for handling game events like card effects, turn events, etc.
 * This will be the foundation for Day 3's event system implementation.
 */
@JsExport
interface GameEventHandler {
    
    /**
     * Handle events when a card is played
     * @param card The card that was played
     * @param player The player who played the card
     * @param gameState Current game state for context
     * @return Array of effects that should be applied to the game
     */
    fun onCardPlayed(card: Card, player: Player, gameState: GameState): Array<GameEffect>
    
    /**
     * Handle events when a card is drawn
     * @param card The card that was drawn
     * @param player The player who drew the card
     * @param gameState Current game state for context
     * @return Array of effects that should be applied to the game
     */
    fun onCardDrawn(card: Card, player: Player, gameState: GameState): Array<GameEffect>
    
    /**
     * Handle events when a player's turn starts
     * @param player The player whose turn is starting
     * @param gameState Current game state for context
     * @return Array of effects that should be applied to the game
     */
    fun onTurnStarted(player: Player, gameState: GameState): Array<GameEffect>
    
    /**
     * Handle events when a player's turn ends
     * @param player The player whose turn is ending
     * @param gameState Current game state for context
     * @return Array of effects that should be applied to the game
     */
    fun onTurnEnded(player: Player, gameState: GameState): Array<GameEffect>
    
    /**
     * Handle events when the game starts
     * @param gameState Initial game state
     * @return Array of effects that should be applied to the game
     */
    fun onGameStarted(gameState: GameState): Array<GameEffect>
    
    /**
     * Handle events when the game ends
     * @param gameState Final game state
     * @param winner The winning player (if any)
     * @return Array of effects that should be applied to the game
     */
    fun onGameEnded(gameState: GameState, winner: Player?): Array<GameEffect>
}

/**
 * Default implementation that handles no events
 * Useful as a base class or for testing
 */
@JsExport
class NoOpEventHandler : GameEventHandler {
    override fun onCardPlayed(card: Card, player: Player, gameState: GameState): Array<GameEffect> = emptyArray()
    override fun onCardDrawn(card: Card, player: Player, gameState: GameState): Array<GameEffect> = emptyArray()
    override fun onTurnStarted(player: Player, gameState: GameState): Array<GameEffect> = emptyArray()
    override fun onTurnEnded(player: Player, gameState: GameState): Array<GameEffect> = emptyArray()
    override fun onGameStarted(gameState: GameState): Array<GameEffect> = emptyArray()
    override fun onGameEnded(gameState: GameState, winner: Player?): Array<GameEffect> = emptyArray()
}

/**
 * Composite event handler that delegates to multiple handlers
 * Useful for combining different event handling behaviors
 */
@JsExport
class CompositeEventHandler(
    private val handlers: Array<GameEventHandler>
) : GameEventHandler {
    
    override fun onCardPlayed(card: Card, player: Player, gameState: GameState): Array<GameEffect> {
        return handlers.flatMap { handler ->
            handler.onCardPlayed(card, player, gameState).toList()
        }.toTypedArray()
    }
    
    override fun onCardDrawn(card: Card, player: Player, gameState: GameState): Array<GameEffect> {
        return handlers.flatMap { handler ->
            handler.onCardDrawn(card, player, gameState).toList()
        }.toTypedArray()
    }
    
    override fun onTurnStarted(player: Player, gameState: GameState): Array<GameEffect> {
        return handlers.flatMap { handler ->
            handler.onTurnStarted(player, gameState).toList()
        }.toTypedArray()
    }
    
    override fun onTurnEnded(player: Player, gameState: GameState): Array<GameEffect> {
        return handlers.flatMap { handler ->
            handler.onTurnEnded(player, gameState).toList()
        }.toTypedArray()
    }
    
    override fun onGameStarted(gameState: GameState): Array<GameEffect> {
        return handlers.flatMap { handler ->
            handler.onGameStarted(gameState).toList()
        }.toTypedArray()
    }
    
    override fun onGameEnded(gameState: GameState, winner: Player?): Array<GameEffect> {
        return handlers.flatMap { handler ->
            handler.onGameEnded(gameState, winner).toList()
        }.toTypedArray()
    }
}

/**
 * Event context that provides additional information for event handlers
 * This will be useful for more complex event handling in Day 3+
 */
@JsExport
data class EventContext(
    val triggerCard: Card?,
    val triggerPlayer: Player?,
    val targetPlayer: Player?,
    val gameState: GameState,
    val additionalData: Map<String, String> = emptyMap()
)

/**
 * Result of processing events
 * Contains both effects to apply and any state changes
 */
@JsExport
data class EventResult(
    val effects: Array<GameEffect>,
    val newGameState: GameState?,
    val shouldContinue: Boolean = true
)