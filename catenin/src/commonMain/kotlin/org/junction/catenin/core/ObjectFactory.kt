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
            propDef.getInitialValue()?.let { properties[name] = it }
        }
        // Override with custom properties
        properties.putAll(customProperties)
        
        // Initialize states with defaults from definition
        val states = mutableMapOf<String, PropertyValue>()
        objectDef.states.forEach { (name, stateDef) ->
            states[name] = stateDef.getInitialValue()
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
        
        val baseObject = createObject(instanceDef.template, convertStringMapToPropertyValues(instanceDef.template, instanceDef.properties))
        
        // Override the generated ID with the instance ID
        return baseObject.copy(id = instanceId)
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
                propDef.getInitialValue()?.let { properties[name] = it }
            }
            // Override with instance-specific properties
            properties.putAll(convertStringMapToPropertyValues(instanceDef.template, instanceDef.properties))
            
            // Initialize states with defaults from object definition
            val states = mutableMapOf<String, PropertyValue>()
            objectDef.states.forEach { (name, stateDef) ->
                states[name] = stateDef.getInitialValue()
            }
            // Override with instance-specific states  
            states.putAll(convertStringMapToPropertyValuesForStates(instanceDef.template, instanceDef.states))
            
            GameObject(
                id = id,
                type = instanceDef.template,
                properties = properties,
                states = states
            )
        }
    }
    
    /**
     * Create objects for initial game setup based on setup configuration
     * Note: participantNames represent abstract participants/seats, not necessarily player objects
     */
    fun createInitialSetup(participantNames: List<String>): List<GameObject> {
        val objects = mutableListOf<GameObject>()
        
        val setup = definition.setup 
            ?: throw IllegalStateException("Game definition must have setup configuration")
        
        // Execute world initialization
        setup.worldInitialization.forEach { instruction ->
            objects.addAll(executeCreateObjectsInstruction(instruction.createObjects))
        }
        
        // Execute participant initialization for each participant
        participantNames.forEachIndexed { index, name ->
            setup.participantInitialization.forEach { instruction ->
                objects.addAll(executeCreateObjectsInstruction(
                    instruction.createObjects,
                    participantId = index,
                    participantName = name
                ))
            }
        }
        
        return objects
    }
    
    /**
     * Execute a create objects instruction, optionally with participant context
     */
    private fun executeCreateObjectsInstruction(
        rule: CreateObjectsRule,
        participantId: Int? = null,
        participantName: String? = null
    ): List<GameObject> {
        val objects = mutableListOf<GameObject>()
        
        repeat(rule.count) {
            val obj = if (rule.instanceSource != null) {
                // Create from predefined instance
                createFromInstance(rule.instanceSource)
            } else {
                // Create from template with custom properties
                val customProperties = convertStringMapToPropertyValues(rule.template, rule.properties)
                createObject(rule.template, customProperties)
            }
            
            // Handle parent assignment with participant substitution
            rule.parent?.let { parentPattern ->
                val resolvedParent = resolveParticipantPattern(parentPattern, participantId, participantName)
                // TODO: Implement parent assignment logic when we have containment system
            }
            
            objects.add(obj)
        }
        
        return objects
    }
    
    /**
     * Resolve participant patterns like "deck_{participant_id}" to actual values
     */
    private fun resolveParticipantPattern(
        pattern: String,
        participantId: Int?,
        participantName: String?
    ): String {
        return pattern
            .replace("{participant_id}", participantId?.toString() ?: "")
            .replace("{participant_name}", participantName ?: "")
    }
    
    private fun generateId(type: String): String {
        return "${type}_${nextId++}"
    }
    
    /**
     * Convert string map to PropertyValue map using object type definitions
     */
    private fun convertStringMapToPropertyValues(objectType: String, stringMap: Map<String, String>): Map<String, PropertyValue> {
        val objectDef = definition.objectTypes[objectType] ?: return emptyMap()
        val result = mutableMapOf<String, PropertyValue>()
        
        stringMap.forEach { (name, value) ->
            val propDef = objectDef.properties[name]
            if (propDef != null) {
                result[name] = when (propDef.type) {
                    PropertyType.INT -> PropertyValue.IntValue(value.toInt())
                    PropertyType.STRING -> PropertyValue.StringValue(value)
                    PropertyType.BOOL -> PropertyValue.BoolValue(value.toBoolean())
                    PropertyType.OBJECT_REF -> PropertyValue.ObjectRefValue(value)
                }
            }
        }
        
        return result
    }
    
    /**
     * Convert string map to PropertyValue map for states using state definitions
     */
    private fun convertStringMapToPropertyValuesForStates(objectType: String, stringMap: Map<String, String>): Map<String, PropertyValue> {
        val objectDef = definition.objectTypes[objectType] ?: return emptyMap()
        val result = mutableMapOf<String, PropertyValue>()
        
        stringMap.forEach { (name, value) ->
            val stateDef = objectDef.states[name]
            if (stateDef != null) {
                result[name] = when (stateDef.type) {
                    PropertyType.INT -> PropertyValue.IntValue(value.toInt())
                    PropertyType.STRING -> PropertyValue.StringValue(value)
                    PropertyType.BOOL -> PropertyValue.BoolValue(value.toBoolean())
                    PropertyType.OBJECT_REF -> PropertyValue.ObjectRefValue(value)
                }
            }
        }
        
        return result
    }
}