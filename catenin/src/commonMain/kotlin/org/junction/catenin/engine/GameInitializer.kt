package org.junction.catenin.engine

import org.junction.catenin.core.GameWorld
import org.junction.catenin.factory.ObjectFactory
import org.junction.catenin.model.definitions.GameMeta
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.js.JsExport

/**
 * Initializes a game world from schema configuration
 */
@JsExport
class GameInitializer(
    private val schema: UniversalGameSchema,
    private val config: InitializationConfig = EmptyInitializationConfig.INSTANCE
) {
    private val objectFactory = ObjectFactory(schema)
    
    /**
     * Create an initial game world based on schema setup configuration
     */
    fun createInitialWorld(participantIds: List<String> = emptyList()): GameWorld {
        val objects = mutableListOf<GameObject>()
        
        // Create participant objects if configured
        if (config.hasParticipants() && config.participantType != null) {
            if (schema.hasObjectType(config.participantType)) {
                participantIds.forEach { id ->
                    val propertyOverrides = mutableMapOf<String, String>()
                    
                    // Add participant ID if property is configured
                    if (config.participantIdProperty != null) {
                        propertyOverrides[config.participantIdProperty] = id
                    }
                    
                    // Add any additional mapped properties
                    config.participantPropertyMapping.forEach { (key, propName) ->
                        // In a real implementation, we'd have participant data to map
                        // For now, just use the ID as the value
                        propertyOverrides[propName] = id
                    }
                    
                    val participant = objectFactory.createObject(
                        objectType = config.participantType,
                        id = id,
                        propertyOverrides = propertyOverrides
                    )
                    objects.add(participant)
                }
            }
        }
        
        // Create singleton objects
        config.singletonObjects.forEach { singletonConfig ->
            if (schema.hasObjectType(singletonConfig.objectType)) {
                val obj = objectFactory.createObject(
                    objectType = singletonConfig.objectType,
                    id = singletonConfig.id,
                    propertyOverrides = singletonConfig.propertyOverrides
                )
                objects.add(obj)
            }
        }
        
        // Create instances based on configuration
        val instancesToCreate = when {
            config.createAllInstances -> schema.instances.keys
            else -> config.autoCreateInstances
        }
        
        instancesToCreate.forEach { instanceName ->
            if (schema.hasInstance(instanceName)) {
                val obj = objectFactory.createFromInstance(instanceName)
                objects.add(obj)
            }
        }
        
        // Return world with all initial objects
        return GameWorld.empty().withObjects(objects)
    }
    
    /**
     * Validate that the game can be initialized with given participants
     */
    fun validateParticipantCount(count: Int): Boolean {
        val meta = schema.meta
        return count >= meta.participantCount[0] && count <= meta.participantCount[1]
    }
    
    /**
     * Get minimum participant count from schema
     */
    fun getMinParticipants(): Int = schema.meta.participantCount[0]
    
    /**
     * Get maximum participant count from schema
     */
    fun getMaxParticipants(): Int = schema.meta.participantCount[1]
}