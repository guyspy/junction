package org.junction.catenin.core

import org.junction.catenin.model.GameObject
import org.junction.catenin.model.PropertyValue
import kotlin.js.JsExport

/**
 * Result of generating an object ID
 */
@JsExport
data class ObjectIdResult(
    val id: String,
    val updatedWorld: GameWorld
)

/**
 * Represents the complete state of the game world containing all objects
 */
@JsExport
data class GameWorld(
    val objects: Map<String, GameObject> = emptyMap(),
    val nextObjectId: Int = 1
) {
    
    /**
     * Add or update an object in the world
     */
    fun withObject(obj: GameObject): GameWorld {
        return copy(objects = objects + (obj.id to obj))
    }
    
    /**
     * Remove an object from the world
     */
    fun withoutObject(objectId: String): GameWorld {
        return copy(objects = objects - objectId)
    }
    
    /**
     * Update object properties
     */
    fun updateObjectProperties(objectId: String, newProperties: Map<String, PropertyValue>): GameWorld {
        val obj = objects[objectId] ?: return this
        val updatedObj = obj.copy(properties = newProperties)
        return withObject(updatedObj)
    }
    
    /**
     * Update object states
     */
    fun updateObjectStates(objectId: String, newStates: Map<String, PropertyValue>): GameWorld {
        val obj = objects[objectId] ?: return this
        val updatedObj = obj.copy(states = newStates)
        return withObject(updatedObj)
    }
    
    /**
     * Change object parent (for containment)
     */
    fun changeObjectParent(objectId: String, newParentId: String?): GameWorld {
        val obj = objects[objectId] ?: return this
        val updatedObj = obj.copy(parentId = newParentId)
        return withObject(updatedObj)
    }
    
    /**
     * Generate next unique object ID
     */
    fun generateObjectId(typePrefix: String): ObjectIdResult {
        val id = "${typePrefix}_${nextObjectId}"
        return ObjectIdResult(id, copy(nextObjectId = nextObjectId + 1))
    }
    
    /**
     * Get all objects of a specific type
     */
    fun getObjectsByType(type: String): List<GameObject> {
        return objects.values.filter { it.type == type }
    }
    
    /**
     * Get all objects with a specific parent
     */
    fun getObjectsByParent(parentId: String): List<GameObject> {
        return objects.values.filter { it.parentId == parentId }
    }
    
    /**
     * Get all objects matching property criteria
     */
    fun getObjectsByProperty(property: String, value: PropertyValue): List<GameObject> {
        return objects.values.filter { it.properties[property] == value }
    }
    
    /**
     * Get all objects matching state criteria
     */
    fun getObjectsByState(state: String, value: PropertyValue): List<GameObject> {
        return objects.values.filter { it.states[state] == value }
    }
}