package org.junction.catenin.model.objects

import org.junction.catenin.model.values.PropertyValue
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
    fun withProperty(name: String, value: PropertyValue): GameObject = copy(properties = properties + (name to value))
    
    /**
     * Create a new GameObject with an updated state value
     */
    fun withState(name: String, value: PropertyValue): GameObject = copy(states = states + (name to value))
    
    /**
     * Create a new GameObject with a property removed
     */
    fun withoutProperty(name: String): GameObject = copy(properties = properties - name)
    
    /**
     * Create a new GameObject with a state removed
     */
    fun withoutState(name: String): GameObject = copy(states = states - name)
    
    /**
     * Get a property value by name
     */
    fun getProperty(name: String): PropertyValue? = properties[name]
    
    /**
     * Get a state value by name
     */
    fun getState(name: String): PropertyValue? = states[name]
    
    /**
     * Check if object has a specific property
     */
    fun hasProperty(name: String): Boolean = properties.containsKey(name)
    
    /**
     * Check if object has a specific state
     */
    fun hasState(name: String): Boolean = states.containsKey(name)
    
    /**
     * Get all property names as a list
     */
    fun getAllPropertyNames(): List<String> = properties.keys.toList()
    
    /**
     * Get all state names as a list
     */
    fun getAllStateNames(): List<String> = states.keys.toList()
}