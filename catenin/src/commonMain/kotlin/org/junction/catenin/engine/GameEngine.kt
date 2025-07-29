package org.junction.catenin.engine

import org.junction.catenin.core.*
import org.junction.catenin.factory.ObjectFactory
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.js.JsExport

/**
 * Main game engine that orchestrates all components
 */
@JsExport
class GameEngine(
    private val schema: UniversalGameSchema,
    private val initializationConfig: InitializationConfig = EmptyInitializationConfig.INSTANCE,
    private val world: GameWorld = GameWorld.empty()
) {
    // Initialize all sub-engines
    private val objectFactory = ObjectFactory(schema)
    private val gameInitializer = GameInitializer(schema, initializationConfig)
    private val targetResolver = SimpleTargetResolver()
    private val effectEngine = EffectEngine(schema, targetResolver)
    private val triggerEngine = TriggerEngine(schema, effectEngine)
    
    private var currentWorld = world
    
    /**
     * Initialize a new game with participants
     */
    fun initializeGame(participantIds: List<String>): GameEngine {
        if (!gameInitializer.validateParticipantCount(participantIds.size)) {
            throw IllegalArgumentException(
                "Invalid participant count: ${participantIds.size}. " +
                "Expected ${gameInitializer.getMinParticipants()}-${gameInitializer.getMaxParticipants()}"
            )
        }
        
        currentWorld = gameInitializer.createInitialWorld(participantIds)
        return this
    }
    
    /**
     * Apply an update to the game world, triggering any cascading effects
     */
    fun applyUpdate(update: WorldUpdate): GameEngine {
        // Apply the initial update
        var newWorld = currentWorld.applyUpdate(update)
        
        // Evaluate triggers for this update
        val triggeredUpdates = triggerEngine.evaluateUpdate(currentWorld, update)
        
        // Apply all triggered updates
        triggeredUpdates.forEach { triggeredUpdate ->
            newWorld = newWorld.applyUpdate(triggeredUpdate)
        }
        
        currentWorld = newWorld
        return this
    }
    
    /**
     * Apply multiple updates in sequence
     */
    fun applyUpdates(updates: List<WorldUpdate>): GameEngine {
        updates.forEach { update ->
            applyUpdate(update)
        }
        return this
    }
    
    /**
     * Get the current game world state
     */
    fun getWorld(): GameWorld = currentWorld
    
    /**
     * Create a new object from schema
     */
    fun createObject(
        objectType: String,
        id: String? = null,
        propertyOverrides: Map<String, String> = emptyMap()
    ): GameObject {
        return objectFactory.createObject(objectType, id, propertyOverrides)
    }
    
    /**
     * Create an object from a predefined instance
     */
    fun createFromInstance(
        instanceName: String,
        id: String? = null
    ): GameObject {
        return objectFactory.createFromInstance(instanceName, id)
    }
    
    /**
     * Add an object to the world
     */
    fun addObject(obj: GameObject): GameEngine {
        return applyUpdate(AddObjectUpdate(obj))
    }
    
    /**
     * Remove an object from the world
     */
    fun removeObject(objectId: String): GameEngine {
        return applyUpdate(RemoveObjectUpdate(objectId))
    }
    
    /**
     * Update an object's property
     */
    fun updateProperty(
        objectId: String,
        propertyName: String,
        value: org.junction.catenin.model.values.PropertyValue
    ): GameEngine {
        return applyUpdate(UpdatePropertyUpdate(objectId, propertyName, value))
    }
    
    /**
     * Update an object's state
     */
    fun updateState(
        objectId: String,
        stateName: String,
        value: org.junction.catenin.model.values.PropertyValue
    ): GameEngine {
        return applyUpdate(UpdateStateUpdate(objectId, stateName, value))
    }
    
    /**
     * Get the game schema
     */
    fun getSchema(): UniversalGameSchema = schema
    
    /**
     * Get the minimum number of participants
     */
    fun getMinParticipants(): Int = gameInitializer.getMinParticipants()
    
    /**
     * Get the maximum number of participants
     */
    fun getMaxParticipants(): Int = gameInitializer.getMaxParticipants()
    
    companion object {
        /**
         * Create a game engine from a schema
         */
        fun fromSchema(
            schema: UniversalGameSchema,
            initializationConfig: InitializationConfig = EmptyInitializationConfig.INSTANCE
        ): GameEngine {
            return GameEngine(schema, initializationConfig)
        }
    }
}