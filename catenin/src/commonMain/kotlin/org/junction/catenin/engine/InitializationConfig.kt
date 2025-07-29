package org.junction.catenin.engine

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Configuration for game initialization - drives behavior without hardcoded assumptions
 */
@Serializable
@JsExport
data class InitializationConfig(
    /**
     * Type name for participant objects (null if no participants)
     * Examples: "player", "character", "contestant", etc.
     */
    val participantType: String? = null,
    
    /**
     * Property name to store participant ID (if participants are used)
     * Examples: "player_id", "character_name", etc.
     */
    val participantIdProperty: String? = null,
    
    /**
     * List of singleton objects to create on initialization
     * Examples: game boards, turn managers, story state, etc.
     */
    val singletonObjects: List<SingletonObjectConfig> = emptyList(),
    
    /**
     * Instance names that should be auto-created on initialization
     * These are instances defined in the schema that should always exist
     */
    val autoCreateInstances: List<String> = emptyList(),
    
    /**
     * Whether to create all instances defined in the schema
     * If true, ignores autoCreateInstances and creates everything
     */
    val createAllInstances: Boolean = false,
    
    /**
     * Additional property mappings for participants
     * Maps initialization data keys to object properties
     */
    val participantPropertyMapping: Map<String, String> = emptyMap()
) {
    /**
     * Check if this configuration uses participants
     */
    fun hasParticipants(): Boolean = participantType != null
}

/**
 * Configuration for a singleton object to create
 */
@Serializable
@JsExport
data class SingletonObjectConfig(
    /**
     * The object type to create
     */
    val objectType: String,
    
    /**
     * The ID to assign to the object
     */
    val id: String,
    
    /**
     * Property overrides for the object
     */
    val propertyOverrides: Map<String, String> = emptyMap()
)

/**
 * Default configuration that creates nothing - truly universal
 */
@JsExport
object EmptyInitializationConfig {
    val INSTANCE = InitializationConfig()
}

/**
 * Common initialization configurations for different game types
 */
@JsExport
object CommonInitializationConfigs {
    /**
     * Configuration for turn-based games with players
     */
    val TURN_BASED_GAME = InitializationConfig(
        participantType = "player",
        participantIdProperty = "player_id",
        singletonObjects = listOf(
            SingletonObjectConfig("game_state", "game_state")
        )
    )
    
    /**
     * Configuration for narrative games with a protagonist
     */
    val NARRATIVE_GAME = InitializationConfig(
        participantType = null,  // No multiplayer participants
        singletonObjects = listOf(
            SingletonObjectConfig("protagonist", "player"),
            SingletonObjectConfig("story_state", "story")
        )
    )
    
    /**
     * Configuration for card games
     */
    val CARD_GAME = InitializationConfig(
        participantType = "player",
        participantIdProperty = "player_id",
        singletonObjects = listOf(
            SingletonObjectConfig("deck", "main_deck"),
            SingletonObjectConfig("discard_pile", "discard_pile")
        )
    )
}