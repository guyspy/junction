package org.junction.cadherin.model

import kotlinx.serialization.Serializable

@Serializable
data class GameDefinition(
    val meta: GameMeta,
    val cards: Map<String, CardTypeDefinition>,
    val mechanics: GameMechanics? = null,
    val aiHints: AIHints? = null
)

@Serializable
data class GameMeta(
    val name: String,
    val targetAge: List<Int>, // [min, max]
    val playerCount: List<Int>? = null // [min, max]
)

@Serializable
data class CardTypeDefinition(
    val count: Int,
    val properties: Map<String, PropertyDefinition>,
    val events: Map<String, EventDefinition>? = null
)

@Serializable
sealed class PropertyDefinition {
    @Serializable
    data class IntProperty(
        val type: String = "int",
        val min: Int,
        val max: Int
    ) : PropertyDefinition()
    
    @Serializable
    data class EnumProperty(
        val type: String = "enum", 
        val values: List<String>
    ) : PropertyDefinition()
    
    @Serializable
    data class StringProperty(
        val type: String = "string",
        val maxLength: Int? = null
    ) : PropertyDefinition()
}

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
    val winConditions: List<WinCondition>? = null
)

@Serializable
data class SetupMechanics(
    val players: PlayerSetup? = null
)

@Serializable
data class PlayerSetup(
    val health: Int? = null,
    val handSize: Int? = null,
    val initialScore: Int? = null
)

@Serializable
data class WinCondition(
    val type: String,
    val target: Int? = null,
    val maxTurns: Int? = null,
    val message: String
)

@Serializable
data class AIHints(
    val difficultyFactors: List<String>,
    val commonModifications: Map<String, Map<String, Int>>
)