package org.junction.catenin.core.initialization

import org.junction.catenin.model.definitions.UniversalGameDefinition
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.PropertyValue
import org.junction.catenin.model.values.IntValue
import org.junction.catenin.model.values.StringValue
import org.junction.catenin.core.factory.*
import org.junction.catenin.core.GameWorld
import kotlin.js.JsExport

/**
 * Handles initial game setup and world creation
 */
@JsExport
class GameInitializer(
    private val definition: UniversalGameDefinition,
    private val factory: ObjectFactory
) {
    
    /**
     * Create initial game world with participants and setup objects
     */
    fun createInitialWorld(participantNames: List<String>): GameWorld {
        validateParticipantCount(participantNames)
        
        var currentFactory = factory
        val allObjects = buildList {
            // Create participant objects if participant type is defined
            val participantResult = createParticipantObjects(participantNames, currentFactory)
            addAll(participantResult.gameObjects)
            currentFactory = participantResult.updatedFactory
            
            // Create world initialization objects (shared objects)
            val worldObjectsResult = createWorldInitializationObjects(currentFactory)
            addAll(worldObjectsResult.gameObjects)
            currentFactory = worldObjectsResult.updatedFactory
            
            // Create participant-specific objects
            val participantSpecificResult = createParticipantSpecificObjects(participantNames, currentFactory)
            addAll(participantSpecificResult.gameObjects)
            currentFactory = participantSpecificResult.updatedFactory
            
            // Create predefined instances
            val instanceResult = createInstanceObjects(currentFactory)
            addAll(instanceResult.gameObjects)
            currentFactory = instanceResult.updatedFactory
        }
        
        return GameWorld.withObjects(allObjects)
    }
    
    /**
     * Validate that participant count is within allowed range
     */
    private fun validateParticipantCount(participantNames: List<String>) {
        val (minParticipants, maxParticipants) = definition.meta.participantCount
        val actualCount = participantNames.size
        
        if (actualCount < minParticipants || actualCount > maxParticipants) {
            throw IllegalArgumentException(
                "Invalid participant count: $actualCount. Expected between $minParticipants and $maxParticipants."
            )
        }
    }
    
    /**
     * Create participant objects (abstract users/seats)
     */
    private fun createParticipantObjects(participantNames: List<String>, startingFactory: ObjectFactory): MultipleObjectCreationResult {
        // Only create participant objects if there's a participant type defined
        if (!definition.hasObjectType("participant")) {
            return MultipleObjectCreationResult(emptyList(), startingFactory)
        }
        
        var currentFactory = startingFactory
        val objects = participantNames.mapIndexed { index, name ->
            val result = currentFactory.createObject(
                type = "participant",
                propertyOverrides = mapOf(
                    "participant_id" to IntValue(index),
                    "name" to StringValue(name)
                ),
                customId = "participant_$index"
            )
            currentFactory = result.updatedFactory
            result.gameObject
        }
        
        return MultipleObjectCreationResult(objects, currentFactory)
    }
    
    /**
     * Create world initialization objects (shared objects created once)
     */
    private fun createWorldInitializationObjects(startingFactory: ObjectFactory): MultipleObjectCreationResult {
        var currentFactory = startingFactory
        val objects = buildList {
            // Create game controller if defined
            if (definition.hasObjectType("game_controller")) {
                val result = currentFactory.createObject(
                    type = "game_controller",
                    propertyOverrides = mapOf(
                        "current_turn" to IntValue(0),
                        "current_player" to IntValue(0)
                    ),
                    customId = "game_controller"
                )
                add(result.gameObject)
                currentFactory = result.updatedFactory
            }
            
            // Create board if defined
            if (definition.hasObjectType("board")) {
                val result = currentFactory.createObject(
                    type = "board",
                    customId = "main_board"
                )
                add(result.gameObject)
                currentFactory = result.updatedFactory
            }
        }
        
        return MultipleObjectCreationResult(objects, currentFactory)
    }
    
    /**
     * Create participant-specific objects (objects created per participant)
     */
    private fun createParticipantSpecificObjects(
        participantNames: List<String>,
        startingFactory: ObjectFactory
    ): MultipleObjectCreationResult {
        var currentFactory = startingFactory
        val objects = buildList {
            participantNames.forEachIndexed { index, name ->
                val substitutions = mapOf(
                    "participant_id" to index.toString(),
                    "participant_name" to name
                )
                
                // Create player state object if defined
                if (definition.hasObjectType("player_state")) {
                    val result = currentFactory.createWithPatterns(
                        type = "player_state",
                        propertyPatterns = mapOf(
                            "participant_id" to "{participant_id}",
                            "name" to "{participant_name}"
                        ),
                        substitutions = substitutions,
                        customId = "player_state_$index"
                    )
                    add(result.gameObject)
                    currentFactory = result.updatedFactory
                }
                
                // Create hand if defined
                if (definition.hasObjectType("hand")) {
                    val result = currentFactory.createWithPatterns(
                        type = "hand",
                        propertyPatterns = mapOf(
                            "owner" to "{participant_id}"
                        ),
                        substitutions = substitutions,
                        customId = "hand_$index"
                    )
                    add(result.gameObject)
                    currentFactory = result.updatedFactory
                }
                
                // Create deck if defined
                if (definition.hasObjectType("deck")) {
                    val result = currentFactory.createWithPatterns(
                        type = "deck",
                        propertyPatterns = mapOf(
                            "owner" to "{participant_id}"
                        ),
                        substitutions = substitutions,
                        customId = "deck_$index"
                    )
                    add(result.gameObject)
                    currentFactory = result.updatedFactory
                }
            }
        }
        
        return MultipleObjectCreationResult(objects, currentFactory)
    }
    
    /**
     * Create objects from predefined instances
     */
    private fun createInstanceObjects(startingFactory: ObjectFactory): MultipleObjectCreationResult {
        var currentFactory = startingFactory
        val objects = definition.getAllInstanceNames().map { instanceName ->
            val result = currentFactory.createFromInstance(instanceName)
            currentFactory = result.updatedFactory
            result.gameObject
        }
        
        return MultipleObjectCreationResult(objects, currentFactory)
    }
    
    /**
     * Create a specific number of objects of a given type
     */
    fun createObjectSet(
        type: String, 
        count: Int, 
        propertyOverrides: Map<String, PropertyValue> = emptyMap(),
        stateOverrides: Map<String, PropertyValue> = emptyMap()
    ): List<GameObject> {
        return factory.createMultiple(
            type = type,
            count = count,
            propertyOverrides = propertyOverrides,
            stateOverrides = stateOverrides
        ).gameObjects
    }
    
    /**
     * Create a deck of cards from instances
     */
    fun createDeckFromInstances(instanceNames: List<String>): List<GameObject> {
        var currentFactory = factory
        return instanceNames.map { instanceName ->
            val result = currentFactory.createFromInstance(instanceName)
            currentFactory = result.updatedFactory
            result.gameObject
        }
    }
    
    /**
     * Create a shuffled deck
     */
    fun createShuffledDeck(
        type: String, 
        count: Int,
        propertyOverrides: Map<String, PropertyValue> = emptyMap()
    ): List<GameObject> {
        val cards = createObjectSet(type, count, propertyOverrides)
        return cards.shuffled()
    }
    
    /**
     * Create participant-specific objects with custom patterns
     */
    fun createParticipantObjectsWithPatterns(
        participantId: Int,
        participantName: String,
        objectConfigs: List<ParticipantObjectConfig>
    ): List<GameObject> {
        val substitutions = mapOf(
            "participant_id" to participantId.toString(),
            "participant_name" to participantName
        )
        
        var currentFactory = factory
        return objectConfigs.map { config ->
            val result = currentFactory.createWithPatterns(
                type = config.type,
                propertyPatterns = config.propertyPatterns,
                statePatterns = config.statePatterns,
                substitutions = substitutions,
                customId = config.customId?.let { pattern ->
                    currentFactory.applySubstitutions(pattern, substitutions)
                }
            )
            currentFactory = result.updatedFactory
            result.gameObject
        }
    }
    
    /**
     * Get setup statistics
     */
    fun getSetupInfo(participantNames: List<String>): SetupInfo {
        return SetupInfo(
            participantCount = participantNames.size,
            availableObjectTypes = definition.getAllObjectTypeNames(),
            availableInstances = definition.getAllInstanceNames(),
            hasParticipantType = definition.hasObjectType("participant"),
            hasGameController = definition.hasObjectType("game_controller"),
            hasBoard = definition.hasObjectType("board")
        )
    }
}