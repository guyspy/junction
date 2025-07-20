package org.junction.catenin.model

import kotlin.js.JsExport

/**
 * Represents a game object in the universal game system.
 * All objects have an ID, type, and collections of properties and states.
 * Objects are immutable - all modification operations return new instances.
 */
@JsExport
data class GameObject(
    val id: String,
    val type: String,
    val properties: Map<String, PropertyValue> = emptyMap(),
    val states: Map<String, PropertyValue> = emptyMap()
) {
    
    /**
     * Create a new GameObject with an updated property value
     */
    fun withProperty(name: String, value: PropertyValue): GameObject {
        return copy(properties = properties + (name to value))
    }
    
    /**
     * Create a new GameObject with an updated state value
     */
    fun withState(name: String, value: PropertyValue): GameObject {
        return copy(states = states + (name to value))
    }
    
    /**
     * Create a new GameObject with a property removed
     */
    fun withoutProperty(name: String): GameObject {
        return copy(properties = properties - name)
    }
    
    /**
     * Create a new GameObject with a state removed
     */
    fun withoutState(name: String): GameObject {
        return copy(states = states - name)
    }
    
    /**
     * Get a property value by name
     */
    fun getProperty(name: String): PropertyValue? {
        return properties[name]
    }
    
    /**
     * Get a state value by name
     */
    fun getState(name: String): PropertyValue? {
        return states[name]
    }
    
    /**
     * Check if object has a specific property
     */
    fun hasProperty(name: String): Boolean {
        return properties.containsKey(name)
    }
    
    /**
     * Check if object has a specific state
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
}