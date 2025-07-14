package org.junction.catenin.core

import org.junction.catenin.model.*
import kotlin.js.JsExport

/**
 * Factory for creating GameObject instances from universal definitions
 */
@JsExport
class ObjectFactory(private val definition: UniversalGameDefinition) {
    
    private var nextId = 1
    
    /**
     * Create a new object instance of the given type
     */
    fun createObject(type: String, customProperties: Map<String, PropertyValue> = emptyMap()): GameObject {
        val objectDef = definition.objectTypes[type] 
            ?: throw IllegalArgumentException("Unknown object type: $type")
        
        val id = generateId(type)
        
        // Initialize properties with defaults from definition
        val properties = mutableMapOf<String, PropertyValue>()
        objectDef.properties.forEach { (name, propDef) ->
            propDef.initial?.let { properties[name] = it }
        }
        // Override with custom properties
        properties.putAll(customProperties)
        
        // Initialize states with defaults from definition
        val states = mutableMapOf<String, PropertyValue>()
        objectDef.states.forEach { (name, stateDef) ->
            states[name] = stateDef.initial
        }
        
        return GameObject(
            id = id,
            type = type,
            properties = properties,
            states = states
        )
    }
    
    /**
     * Create an object instance from a predefined ObjectInstance
     */
    fun createFromInstance(instanceId: String): GameObject {
        val instanceDef = definition.instances[instanceId] 
            ?: throw IllegalArgumentException("Unknown instance: $instanceId")
        
        return createObject(instanceDef.template, instanceDef.properties)
    }
    
    /**
     * Create all predefined instances from the definition
     */
    fun createAllInstances(): List<GameObject> {
        return definition.instances.map { (instanceId, instanceDef) ->
            val id = instanceId // Use the predefined ID
            val objectDef = definition.objectTypes[instanceDef.template]
                ?: throw IllegalArgumentException("Unknown object type: ${instanceDef.template}")
            
            // Initialize properties with defaults from object definition
            val properties = mutableMapOf<String, PropertyValue>()
            objectDef.properties.forEach { (name, propDef) ->
                propDef.initial?.let { properties[name] = it }
            }
            // Override with instance-specific properties
            properties.putAll(instanceDef.properties)
            
            // Initialize states with defaults from object definition
            val states = mutableMapOf<String, PropertyValue>()
            objectDef.states.forEach { (name, stateDef) ->
                states[name] = stateDef.initial
            }
            // Override with instance-specific states
            states.putAll(instanceDef.states)
            
            GameObject(
                id = id,
                type = instanceDef.template,
                properties = properties,
                states = states
            )
        }
    }
    
    /**
     * Create objects for initial game setup based on meta configuration
     * Note: participantNames represent abstract participants/seats, not necessarily player objects
     */
    fun createInitialSetup(participantNames: List<String>): List<GameObject> {
        val objects = mutableListOf<GameObject>()
        
        // Create player objects if player object type is defined (optional)
        // These represent in-game player characters/avatars linked to abstract participants
        if ("player" in definition.objectTypes) {
            participantNames.forEachIndexed { index, name ->
                val playerObject = createObject("player", mapOf(
                    "name" to PropertyValue.StringValue(name),
                    "participant_id" to PropertyValue.IntValue(index)
                ))
                objects.add(playerObject)
            }
        }
        
        // Create all predefined instances
        objects.addAll(createAllInstances())
        
        return objects
    }
    
    private fun generateId(type: String): String {
        return "${type}_${nextId++}"
    }
}