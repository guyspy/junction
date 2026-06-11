package org.junction.catenin.protocol

import org.junction.catenin.core.GameWorld
import kotlin.js.JsExport

/**
 * Result of a completed game.
 */
@JsExport
data class GameResult(
    val winnerId: String?,
    val reason: String
)

/**
 * Action submitted by a participant (what flows from client to server).
 */
@JsExport
data class PlayerAction(
    val type: String,
    val sourceId: String,
    val targetId: String?,
    val metadata: Map<String, String>
)

/**
 * Wire protocol events (what flows from server to client).
 * Each variant represents a distinct game event that renderers must handle.
 */
@JsExport
sealed class GameEvent {

    data class GameStarted(val world: GameWorld, val options: GameOptions) : GameEvent()

    data class EffectBlockEvent(val block: EffectBlock) : GameEvent()

    data class OptionsUpdated(val options: GameOptions) : GameEvent()

    data class GameEnded(val result: GameResult) : GameEvent()
}

/**
 * Interface that any UI/renderer must implement to display the game.
 * Inspired by Wanderer's IUserInterface.
 */
interface GameRenderer {
    fun onGameStart(world: GameWorld, options: GameOptions)
    fun onEffectBlock(block: EffectBlock)
    fun onOptionsUpdate(options: GameOptions)
    fun onGameEnd(result: GameResult)
}

/**
 * Interface for how the UI sends player actions back to the engine.
 */
interface GameInput {
    fun submitAction(participantId: String, action: PlayerAction)
}
