package org.junction.catenin.model.definitions

import org.junction.catenin.model.values.PropertyValue
import org.junction.catenin.model.objects.GameObject
import kotlin.js.JsExport

/**
 * Defines an object type with its properties and states schema
 */
@JsExport
data class ObjectTypeDefinition(
    val properties: Map<String, PropertyDefinition> = emptyMap(),
    val states: Map<String, PropertyDefinition> = emptyMap()
) {
    
    /**
     * Create a new ObjectDefinition with an additional property
     */
    fun withProperty(name: String, definition: PropertyDefinition): ObjectTypeDefinition {
        return copy(properties = properties + (name to definition))
    }
    
    /**
     * Create a new ObjectDefinition with an additional state
     */
    fun withState(name: String, definition: PropertyDefinition): ObjectTypeDefinition {
        return copy(states = states + (name to definition))
    }
    
    /**
     * Create a new ObjectDefinition with a property removed
     */
    fun withoutProperty(name: String): ObjectTypeDefinition {
        return copy(properties = properties - name)
    }
    
    /**
     * Create a new ObjectDefinition with a state removed
     */
    fun withoutState(name: String): ObjectTypeDefinition {
        return copy(states = states - name)
    }
    
    /**
     * Get property definition by name
     */
    fun getPropertyDefinition(name: String): PropertyDefinition? {
        return properties[name]
    }
    
    /**
     * Get state definition by name
     */
    fun getStateDefinition(name: String): PropertyDefinition? {
        return states[name]
    }
    
    /**
     * Check if object type has a specific property definition
     */
    fun hasProperty(name: String): Boolean {
        return properties.containsKey(name)
    }
    
    /**
     * Check if object type has a specific state definition
     */
    fun hasState(name: String): Boolean {
        return states.containsKey(name)
    }
    
    /**
     * Get all property names as a list
     */
    fun getAllPropertyNames(): List<String> {
        return properties.keys.toList()
    }
    
    /**
     * Get all state names as a list
     */
    fun getAllStateNames(): List<String> {
        return states.keys.toList()
    }
    
    /**
     * Validate a GameObject against this object definition
     */
    fun validateObject(obj: GameObject): List<String> {
        val errors = mutableListOf<String>()
        
        // Check that all defined properties have valid values
        for ((propName, propDef) in properties) {
            val value = obj.getProperty(propName)
            if (value == null) {
                // Property is missing, but that's allowed (will use default)
                continue
            }
            if (!propDef.isValid(value)) {
                errors.add("Property '$propName' has invalid value: $value")
            }
        }
        
        // Check that all defined states have valid values
        for ((stateName, stateDef) in states) {
            val value = obj.getState(stateName)
            if (value == null) {
                // State is missing, but that's allowed (will use default)
                continue
            }
            if (!stateDef.isValid(value)) {
                errors.add("State '$stateName' has invalid value: $value")
            }
        }
        
        return errors
    }
    
    /**
     * Create a GameObject instance from this definition with default values
     */
    fun createObject(id: String, type: String, propertyOverrides: Map<String, PropertyValue> = emptyMap(), stateOverrides: Map<String, PropertyValue> = emptyMap()): GameObject {
        // Start with default values for all defined properties
        val finalProperties = properties.mapValues { (_, propDef) ->
            propDef.getDefaultValue()
        }.toMutableMap()
        
        // Apply overrides
        finalProperties.putAll(propertyOverrides)
        
        // Start with default values for all defined states
        val finalStates = states.mapValues { (_, stateDef) ->
            stateDef.getDefaultValue()
        }.toMutableMap()
        
        // Apply overrides
        finalStates.putAll(stateOverrides)
        
        return GameObject(
            id = id,
            type = type,
            properties = finalProperties,
            states = finalStates
        )
    }
}