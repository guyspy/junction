package org.junction.catenin.actions

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
@JsExport
sealed class PlayerAction {
    abstract val playerId: String
    
    @Serializable
    data class DrawCard(override val playerId: String) : PlayerAction()
    
    @Serializable
    data class PlayCard(
        override val playerId: String, 
        val cardId: String
    ) : PlayerAction()
    
    @Serializable
    data class EndTurn(override val playerId: String) : PlayerAction()
}

@Serializable
@JsExport
data class ActionResult(
    val success: Boolean,
    val type: ActionType,
    val effects: Array<GameEffect>,
    val validationErrors: Array<String>
) {
    companion object {
        fun success(type: ActionType, effects: Array<GameEffect> = emptyArray()): ActionResult =
            ActionResult(true, type, effects, emptyArray())
        
        fun failure(type: ActionType, errors: Array<String>): ActionResult =
            ActionResult(false, type, emptyArray(), errors)
    }
}

@Serializable
@JsExport
enum class ActionType {
    DRAW_CARD,
    PLAY_CARD,
    END_TURN,
    GAME_EVENT
}

@Serializable
@JsExport
data class GameEffect(
    val type: EffectType,
    val targetPlayerId: String,
    val sourceCardId: String? = null,
    val amount: Int? = null,
    val description: String
)

@Serializable
@JsExport
enum class EffectType {
    CARD_DRAWN,
    CARD_PLAYED,
    DAMAGE_DEALT,
    HEALTH_RESTORED,
    TURN_ENDED,
    GAME_OVER
}

@Serializable
@JsExport
data class ValidationResult(
    val isValid: Boolean,
    val errors: Array<String>
) {
    companion object {
        fun valid(): ValidationResult = ValidationResult(true, emptyArray())
        fun invalid(vararg errors: String): ValidationResult = ValidationResult(false, errors.toList().toTypedArray())
    }
}