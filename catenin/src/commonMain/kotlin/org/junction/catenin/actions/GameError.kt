package org.junction.catenin.actions

import kotlinx.serialization.Serializable
import kotlin.js.JsExport
import kotlin.js.JsName

@Serializable
@JsExport
sealed class GameError {
    abstract val code: String
    abstract val message: String
    abstract val context: Map<String, String>
    
    @Serializable
    data class PlayerNotFound(
        val playerId: String
    ) : GameError() {
        override val code = "PLAYER_NOT_FOUND"
        override val message = "Player not found: $playerId"
        override val context = mapOf("playerId" to playerId)
    }
    
    @Serializable
    data class NotPlayerTurn(
        val expectedPlayerId: String,
        val actualPlayerId: String
    ) : GameError() {
        override val code = "NOT_PLAYER_TURN"
        override val message = "Not your turn. Expected: $expectedPlayerId, but current player is: $actualPlayerId"
        override val context = mapOf(
            "expectedPlayerId" to expectedPlayerId,
            "actualPlayerId" to actualPlayerId
        )
    }
    
    @Serializable
    data class CardNotInHand(
        val cardId: String,
        val playerId: String
    ) : GameError() {
        override val code = "CARD_NOT_IN_HAND"
        override val message = "Card $cardId not found in player $playerId's hand"
        override val context = mapOf(
            "cardId" to cardId,
            "playerId" to playerId
        )
    }
    
    @Serializable
    data class DeckEmpty(
        val playerId: String
    ) : GameError() {
        override val code = "DECK_EMPTY"
        override val message = "Cannot draw card - deck is empty"
        override val context = mapOf("playerId" to playerId)
    }
    
    @Serializable
    data class HandFull(
        val playerId: String,
        val currentHandSize: Int,
        val maxHandSize: Int
    ) : GameError() {
        override val code = "HAND_FULL"
        override val message = "Cannot draw card - hand is full ($currentHandSize/$maxHandSize)"
        override val context = mapOf(
            "playerId" to playerId,
            "currentHandSize" to currentHandSize.toString(),
            "maxHandSize" to maxHandSize.toString()
        )
    }
    
    @Serializable
    data class GameAlreadyOver(
        val currentPhase: String
    ) : GameError() {
        override val code = "GAME_ALREADY_OVER"
        override val message = "Game is already over (phase: $currentPhase)"
        override val context = mapOf("currentPhase" to currentPhase)
    }
    
    @Serializable
    data class InvalidGamePhase(
        val expectedPhase: String,
        val actualPhase: String,
        val action: String
    ) : GameError() {
        override val code = "INVALID_GAME_PHASE"
        override val message = "Cannot perform $action in $actualPhase phase (expected: $expectedPhase)"
        override val context = mapOf(
            "expectedPhase" to expectedPhase,
            "actualPhase" to actualPhase,
            "action" to action
        )
    }
    
    @Serializable
    data class GenericError(
        val errorMessage: String,
        val errorCode: String = "GENERIC_ERROR",
        val errorContext: Map<String, String> = emptyMap()
    ) : GameError() {
        override val code = errorCode
        override val message = errorMessage
        override val context = errorContext
    }
}

// Enhanced ValidationResult to use structured errors
@Serializable
@JsExport
data class StructuredValidationResult(
    val isValid: Boolean,
    val errors: Array<GameError>
) {
    companion object {
        fun valid(): StructuredValidationResult = StructuredValidationResult(true, emptyArray())
        
        @JsName("invalidMultiple")
        fun invalid(vararg errors: GameError): StructuredValidationResult = 
            StructuredValidationResult(false, errors.toList().toTypedArray())
        
        @JsName("invalidSingle")
        fun invalid(error: GameError): StructuredValidationResult = 
            StructuredValidationResult(false, arrayOf(error))
        
        // Backward compatibility with string errors
        fun invalidString(vararg messages: String): StructuredValidationResult = 
            StructuredValidationResult(false, messages.map { 
                GameError.GenericError(it) 
            }.toTypedArray())
    }
}

// Extension functions to convert between old and new formats
fun ValidationResult.toStructured(): StructuredValidationResult {
    return if (isValid) {
        StructuredValidationResult.valid()
    } else {
        StructuredValidationResult.invalidString(*errors)
    }
}

fun StructuredValidationResult.toLegacy(): ValidationResult {
    return if (isValid) {
        ValidationResult.valid()
    } else {
        ValidationResult.invalid(*errors.map { it.message }.toTypedArray())
    }
}