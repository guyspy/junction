package org.junction.catenin.model

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Runtime instance of an object in the game world
 * Everything is a GameObject - players, cards, zones, etc.
 */
@Serializable
@JsExport
data class GameObject(
    val id: String,
    val type: String, // references ObjectDefinition
    val properties: Map<String, PropertyValue> = emptyMap(),
    val states: Map<String, PropertyValue> = emptyMap(),
    val parentId: String? = null, // for containment hierarchy
    val zoneIds: Map<String, String> = emptyMap() // zone name -> zone object id
) {
    
    fun getProperty(name: String): PropertyValue? = properties[name]
    
    fun getState(name: String): PropertyValue? = states[name]
    
    fun withProperty(name: String, value: PropertyValue): GameObject {
        return copy(properties = properties + (name to value))
    }
    
    fun withState(name: String, value: PropertyValue): GameObject {
        return copy(states = states + (name to value))
    }
    
    fun withParent(parentId: String?): GameObject {
        return copy(parentId = parentId)
    }
}

/**
 * Represents a property change event
 */
@Serializable
@JsExport
data class PropertyChangeEvent(
    val objectId: String,
    val propertyPath: String, // e.g., "properties.health" or "states.tapped"
    val oldValue: PropertyValue?,
    val newValue: PropertyValue,
    val timestamp: Long = 0L // for ordering/replay
)

/**
 * The complete state of the game world
 * All objects and their relationships
 */
@Serializable
@JsExport
data class ObjectGraph(
    val objects: Map<String, GameObject> = emptyMap(),
    val definition: UniversalGameDefinition,
    val currentPhase: String? = null,
    val currentPlayer: String? = null,
    val turnNumber: Int = 1
) {
    
    fun getObject(id: String): GameObject? = objects[id]
    
    fun withObject(gameObject: GameObject): ObjectGraph {
        return copy(objects = objects + (gameObject.id to gameObject))
    }
    
    fun withoutObject(id: String): ObjectGraph {
        return copy(objects = objects - id)
    }
    
    fun getObjectsByType(type: String): List<GameObject> {
        return objects.values.filter { it.type == type }
    }
    
    fun getObjectsInZone(zoneId: String): List<GameObject> {
        return objects.values.filter { it.parentId == zoneId }
    }
    
    fun withPropertyChange(objectId: String, propertyPath: String, newValue: PropertyValue): ObjectGraph {
        val obj = getObject(objectId) ?: return this
        
        val updatedObject = when {
            propertyPath.startsWith("properties.") -> {
                val propName = propertyPath.removePrefix("properties.")
                obj.withProperty(propName, newValue)
            }
            propertyPath.startsWith("states.") -> {
                val stateName = propertyPath.removePrefix("states.")
                obj.withState(stateName, newValue)
            }
            propertyPath == "parent" -> {
                val parentId = (newValue as? PropertyValue.StringValue)?.value
                obj.withParent(parentId)
            }
            else -> obj
        }
        
        return withObject(updatedObject)
    }
}