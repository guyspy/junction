package org.junction.catenin.core

import org.junction.catenin.model.objects.GameObject
import kotlin.js.JsExport

/**
 * Simple, synchronous game world that can only be modified through events
 */
@JsExport
class GameWorld(
    private val objects: Map<String, GameObject> = emptyMap(),
    private val nextObjectId: Int = 1
) {
    /**
     * Apply an update to the world, returning the new world state
     */
    fun applyUpdate(update: WorldUpdate): GameWorld = when (update) {
        is AddObjectUpdate -> copy(
            objects = objects + (update.obj.id to update.obj)
        )
        is RemoveObjectUpdate -> copy(
            objects = objects - update.objectId
        )
        is UpdatePropertyUpdate -> {
            val obj = objects[update.objectId]
            if (obj != null) {
                val updatedObj = obj.withProperty(update.propertyName, update.value)
                copy(objects = objects + (update.objectId to updatedObj))
            } else {
                this
            }
        }
        is UpdateStateUpdate -> {
            val obj = objects[update.objectId]
            if (obj != null) {
                val updatedObj = obj.withState(update.stateName, update.value)
                copy(objects = objects + (update.objectId to updatedObj))
            } else {
                this
            }
        }
        is ReplaceObjectUpdate -> {
            if (objects.containsKey(update.objectId)) {
                copy(objects = objects + (update.objectId to update.newObject))
            } else {
                this
            }
        }
    }
    
    /**
     * Get an object by ID
     */
    fun getObject(id: String): GameObject? = objects[id]
    
    /**
     * Get all objects
     */
    fun getAllObjects(): List<GameObject> = objects.values.toList()
    
    /**
     * Get objects by type
     */
    fun getObjectsByType(type: String): List<GameObject> = 
        objects.values.filter { it.type == type }
    
    /**
     * Check if object exists
     */
    fun hasObject(id: String): Boolean = objects.containsKey(id)
    
    /**
     * Get the next available object ID
     */
    fun getNextObjectId(): Int = nextObjectId
    
    /**
     * Create a copy with updated values
     */
    private fun copy(
        objects: Map<String, GameObject> = this.objects,
        nextObjectId: Int = this.nextObjectId
    ): GameWorld = GameWorld(objects, nextObjectId)
    
    /**
     * Create a new world with objects added
     */
    fun withObjects(newObjects: List<GameObject>): GameWorld = 
        copy(objects = objects + newObjects.associateBy { it.id })
    
    companion object {
        /**
         * Create an empty game world
         */
        fun empty(): GameWorld = GameWorld()
    }
}

/**
 * Sealed class for world update operations
 */
@JsExport
sealed class WorldUpdate