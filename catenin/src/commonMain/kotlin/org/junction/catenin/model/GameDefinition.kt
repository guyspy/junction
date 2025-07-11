package org.junction.catenin.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlin.js.JsExport

@Serializable
@JsExport
data class GameDefinition(
    val meta: GameMeta,
    val cards: Map<String, CardTypeDefinition>,
    val mechanics: GameMechanics? = null,
    @SerialName("ai_hints")
    val aiHints: AIHints? = null
)

@Serializable
data class GameMeta(
    val name: String,
    @SerialName("target_age")
    val targetAge: List<Int>, // [min, max]
    @SerialName("player_count")
    val playerCount: List<Int>? = null // [min, max]
)

@Serializable
data class CardTypeDefinition(
    val count: Int,
    val properties: Map<String, PropertyDefinition>,
    val events: Map<String, EventDefinition>? = null
)

@Serializable
data class PropertyDefinition(
    val type: String,
    val min: Int? = null,
    val max: Int? = null,
    val values: List<String>? = null,
    val maxLength: Int? = null
)

@Serializable
data class EventDefinition(
    val action: String,
    val target: String? = null,
    val amount: String? = null,
    val condition: String? = null
)

@Serializable
data class GameMechanics(
    val setup: SetupMechanics? = null,
    @SerialName("win_conditions")
    val winConditions: List<WinCondition>? = null
)

@Serializable
data class SetupMechanics(
    val players: PlayerSetup? = null
)

@Serializable
data class PlayerSetup(
    val health: Int? = null,
    @SerialName("hand_size")
    val handSize: Int? = null,
    @SerialName("initial_score")
    val initialScore: Int? = null
)

@Serializable
data class WinCondition(
    val type: String,
    val target: Int? = null,
    @SerialName("max_turns")
    val maxTurns: Int? = null,
    val message: String
)

@Serializable
data class AIHints(
    @SerialName("difficulty_factors")
    val difficultyFactors: List<String>,
    @SerialName("common_modifications")
    val commonModifications: Map<String, Map<String, Int>>
)