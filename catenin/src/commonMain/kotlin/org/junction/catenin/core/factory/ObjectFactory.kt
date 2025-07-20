package org.junction.catenin.core.factory

import org.junction.catenin.model.definitions.UniversalGameDefinition
import org.junction.catenin.model.definitions.ObjectTypeDefinition
import org.junction.catenin.model.definitions.PropertyType
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.objects.ObjectInstance
import org.junction.catenin.model.values.PropertyValue
import org.junction.catenin.model.values.IntValue
import org.junction.catenin.model.values.StringValue
import org.junction.catenin.model.values.BoolValue
import org.junction.catenin.model.values.ObjectRefValue
import kotlin.js.JsExport

/**
 * Factory for creating game objects from definitions and instances
 * Immutable factory that returns new instances with updated ID counters
 */
@JsExport
data class ObjectFactory(
    private val definition: UniversalGameDefinition,
    private val nextObjectId: Int = 1
) {
    
    /**
     * Create an object from an object type definition
     * Returns the created object and an updated factory with incremented ID counter
     */
    fun createObject(
        type: String, 
        propertyOverrides: Map<String, PropertyValue> = emptyMap(),
        stateOverrides: Map<String, PropertyValue> = emptyMap(),
        customId: String? = null
    ): ObjectCreationResult {
        val objectType = definition.getObjectType(type)
            ?: throw IllegalArgumentException("Unknown object type: $type")
        
        val (objectId, updatedFactory) = if (customId != null) {
            customId to this
        } else {
            val id = generateObjectId(type)
            id to copy(nextObjectId = nextObjectId + 1)
        }
        
        val gameObject = objectType.createObject(
            id = objectId,
            type = type,
            propertyOverrides = propertyOverrides,
            stateOverrides = stateOverrides
        )
        
        return ObjectCreationResult(gameObject, updatedFactory)
    }
    
    /**
     * Create an object from a predefined instance
     * Returns the created object and an updated factory with incremented ID counter
     */
    fun createFromInstance(instanceId: String, customId: String? = null): ObjectCreationResult {
        val instance = definition.getInstance(instanceId)
            ?: throw IllegalArgumentException("Unknown instance: $instanceId")
        
        val objectType = definition.getObjectType(instance.objectType)
            ?: throw IllegalArgumentException("Unknown object type for instance '$instanceId': ${instance.objectType}")
        
        // Convert string property values to PropertyValue objects
        val propertyOverrides = instance.properties.mapValues { (propertyName, stringValue) ->
            val propertyDef = objectType.getPropertyDefinition(propertyName)
                ?: throw IllegalArgumentException("Unknown property '$propertyName' for instance '$instanceId'")
            parsePropertyValue(stringValue, propertyDef.type)
        }
        
        // Convert string state values to PropertyValue objects
        val stateOverrides = instance.states.mapValues { (stateName, stringValue) ->
            val stateDef = objectType.getStateDefinition(stateName)
                ?: throw IllegalArgumentException("Unknown state '$stateName' for instance '$instanceId'")
            parsePropertyValue(stringValue, stateDef.type)
        }
        
        val (objectId, updatedFactory) = if (customId != null) {
            customId to this
        } else {
            val id = generateObjectId(instance.objectType)
            id to copy(nextObjectId = nextObjectId + 1)
        }
        
        val gameObject = objectType.createObject(
            id = objectId,
            type = instance.objectType,
            propertyOverrides = propertyOverrides,
            stateOverrides = stateOverrides
        )
        
        return ObjectCreationResult(gameObject, updatedFactory)
    }
    
    /**
     * Create initial setup objects for participants
     * Returns the created objects and an updated factory with incremented ID counter
     */
    fun createInitialSetup(participantNames: List<String>): MultipleObjectCreationResult {
        if (!definition.hasObjectType("participant")) {
            return MultipleObjectCreationResult(emptyList(), this)
        }
        
        var currentFactory = this
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
     * Create multiple objects of the same type
     * Returns the created objects and an updated factory with incremented ID counter
     */
    fun createMultiple(
        type: String,
        count: Int,
        propertyOverrides: Map<String, PropertyValue> = emptyMap(),
        stateOverrides: Map<String, PropertyValue> = emptyMap()
    ): MultipleObjectCreationResult {
        var currentFactory = this
        val objects = (0 until count).map { index ->
            val result = currentFactory.createObject(
                type = type,
                propertyOverrides = propertyOverrides,
                stateOverrides = stateOverrides
                // No custom ID - let the factory generate unique IDs
            )
            currentFactory = result.updatedFactory
            result.gameObject
        }
        
        return MultipleObjectCreationResult(objects, currentFactory)
    }
    
    /**
     * Create objects with property pattern substitution (e.g., for participant-specific objects)
     * Returns the created object and an updated factory with incremented ID counter
     */
    fun createWithPatterns(
        type: String,
        propertyPatterns: Map<String, String>,
        statePatterns: Map<String, String> = emptyMap(),
        substitutions: Map<String, String>,
        customId: String? = null
    ): ObjectCreationResult {
        val objectType = definition.getObjectType(type)
            ?: throw IllegalArgumentException("Unknown object type: $type")
        
        // Apply pattern substitutions to property values
        val propertyOverrides = propertyPatterns.mapValues { (propertyName, pattern) ->
            val substitutedValue = applySubstitutions(pattern, substitutions)
            val propertyDef = objectType.getPropertyDefinition(propertyName)
                ?: throw IllegalArgumentException("Unknown property '$propertyName' for type '$type'")
            parsePropertyValue(substitutedValue, propertyDef.type)
        }
        
        // Apply pattern substitutions to state values
        val stateOverrides = statePatterns.mapValues { (stateName, pattern) ->
            val substitutedValue = applySubstitutions(pattern, substitutions)
            val stateDef = objectType.getStateDefinition(stateName)
                ?: throw IllegalArgumentException("Unknown state '$stateName' for type '$type'")
            parsePropertyValue(substitutedValue, stateDef.type)
        }
        
        // Apply pattern substitution to customId if provided
        val finalCustomId = customId?.let { 
            applySubstitutions(it, substitutions) 
        }
        
        return createObject(
            type = type,
            propertyOverrides = propertyOverrides,
            stateOverrides = stateOverrides,
            customId = finalCustomId
        )
    }
    
    /**
     * Generate a unique object ID (does not mutate the factory)
     */
    private fun generateObjectId(type: String, index: Int? = null): String {
        return if (index != null) {
            "${type}_${index}_$nextObjectId"
        } else {
            "${type}_$nextObjectId"
        }
    }
    
    /**
     * Parse a string value to the appropriate PropertyValue type
     */
    private fun parsePropertyValue(stringValue: String, type: PropertyType): PropertyValue {
        return when (type) {
            PropertyType.INT -> {
                val intValue = stringValue.toIntOrNull()
                    ?: throw IllegalArgumentException("Cannot parse '$stringValue' as INT")
                IntValue(intValue)
            }
            PropertyType.STRING -> StringValue(stringValue)
            PropertyType.BOOL -> {
                val boolValue = when (stringValue.lowercase()) {
                    "true" -> true
                    "false" -> false
                    else -> throw IllegalArgumentException("Cannot parse '$stringValue' as BOOL")
                }
                BoolValue(boolValue)
            }
            PropertyType.OBJECT_REF -> ObjectRefValue(stringValue)
        }
    }
    
    /**
     * Apply pattern substitutions to a string (e.g., {participant_id} -> "0")
     * Uses functional fold instead of imperative loop
     */
    fun applySubstitutions(pattern: String, substitutions: Map<String, String>): String {
        return substitutions.entries.fold(pattern) { result, (key, value) ->
            result.replace("{$key}", value)
        }
    }
    
    /**
     * Create a new factory with reset ID counter (useful for testing)
     */
    fun resetIdCounter(): ObjectFactory {
        return copy(nextObjectId = 1)
    }
    
    /**
     * Get the current object ID counter value
     */
    fun getCurrentIdCounter(): Int {
        return nextObjectId
    }
}