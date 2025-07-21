package org.junction.catenin.core

import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.PropertyValue
import kotlin.js.JsExport

/**
 * Immutable game world state containing all game objects
 */
@JsExport
data class GameWorld(
    val objects: Map<String, GameObject> = emptyMap(),
    val nextObjectId: Int = 1
) {

    /**
     * Create a new GameWorld with an object added
     */
    fun withObject(obj: GameObject): GameWorld = copy(objects = objects + (obj.id to obj))

    /**
     * Create a new GameWorld with multiple objects added
     */
    fun withObjects(newObjects: List<GameObject>): GameWorld =
        copy(objects = objects + newObjects.associateBy { it.id })

    /**
     * Create a new GameWorld with an object removed
     */
    fun withoutObject(objectId: String): GameWorld = copy(objects = objects - objectId)

    /**
     * Create a new GameWorld with multiple objects removed
     */
    fun withoutObjects(objectIds: List<String>): GameWorld = copy(objects = objects - objectIds.toSet())

    /**
     * Create a new GameWorld with an object updated
     */
    fun updateObject(objectId: String, transform: (GameObject) -> GameObject): GameWorld {
        val obj = requireObject(objectId)
        val updatedObject = transform(obj)
        
        // Ensure the object ID matches
        val finalObject = if (updatedObject.id != objectId) {
            updatedObject.copy(id = objectId)
        } else {
            updatedObject
        }

        return copy(objects = objects + (objectId to finalObject))
    }

    /**
     * Create a new GameWorld with an object's property updated
     */
    fun updateObjectProperty(objectId: String, propertyName: String, value: PropertyValue): GameWorld =
        updateObject(objectId) { it.withProperty(propertyName, value) }

    /**
     * Create a new GameWorld with an object's state updated
     */
    fun updateObjectState(objectId: String, stateName: String, value: PropertyValue): GameWorld =
        updateObject(objectId) { it.withState(stateName, value) }

    /**
     * Get an object by ID
     */
    fun getObject(objectId: String): GameObject? = objects[objectId]

    /**
     * Get an object by ID (throws if not found)
     */
    fun requireObject(objectId: String): GameObject = objects[objectId]
        ?: throw IllegalArgumentException("Object with ID '$objectId' does not exist in the world")

    /**
     * Check if an object exists
     */
    fun hasObject(objectId: String): Boolean = objects.containsKey(objectId)

    /**
     * Get all objects as a list
     */
    fun getAllObjects(): List<GameObject> = objects.values.toList()

    /**
     * Get all object IDs as a list
     */
    fun getAllObjectIds(): List<String> = objects.keys.toList()

    /**
     * Get objects by type
     */
    fun getObjectsByType(type: String): List<GameObject> = objects.values.filter { it.type == type }

    /**
     * Get objects by property value
     */
    fun getObjectsByProperty(propertyName: String, value: PropertyValue): List<GameObject> =
        objects.values.filter { obj -> obj.getProperty(propertyName) == value }

    /**
     * Get objects by state value
     */
    fun getObjectsByState(stateName: String, value: PropertyValue): List<GameObject> =
        objects.values.filter { obj -> obj.getState(stateName) == value }

    /**
     * Find objects matching a predicate
     */
    fun findObjects(predicate: (GameObject) -> Boolean): List<GameObject> = objects.values.filter(predicate)

    /**
     * Find the first object matching a predicate
     */
    fun findObject(predicate: (GameObject) -> Boolean): GameObject? = objects.values.firstOrNull(predicate)

    /**
     * Count objects in the world
     */
    fun getObjectCount(): Int = objects.size

    /**
     * Count objects by type
     */
    fun getObjectCountByType(type: String): Int = objects.values.count { it.type == type }

    /**
     * Check if the world is empty
     */
    fun isEmpty(): Boolean = objects.isEmpty()

    /**
     * Get all objects with a specific property defined
     */
    fun getObjectsWithProperty(propertyName: String): List<GameObject> =
        objects.values.filter { it.hasProperty(propertyName) }

    /**
     * Get all objects with a specific state defined
     */
    fun getObjectsWithState(stateName: String): List<GameObject> =
        objects.values.filter { it.hasState(stateName) }


    /**
     * Apply a batch of updates atomically
     */
    fun applyUpdates(updates: List<WorldUpdate>): GameWorld {
        var result = this

        for (update in updates) {
            result = when (update) {
                is AddObjectUpdate -> result.withObject(update.obj)
                is RemoveObjectUpdate -> result.withoutObject(update.objectId)
                is UpdatePropertyUpdate -> result.updateObjectProperty(
                    update.objectId,
                    update.propertyName,
                    update.value
                )

                is UpdateStateUpdate -> result.updateObjectState(
                    update.objectId,
                    update.stateName,
                    update.value
                )

                is ReplaceObjectUpdate -> result.updateObject(update.objectId) { update.newObject }
            }
        }

        return result
    }

    /**
     * Create a snapshot of the current world state for debugging
     */
    fun createSnapshot(): WorldSnapshot = WorldSnapshot(
        objectCount = objects.size,
        objectsByType = objects.values.groupBy { it.type }.mapValues { it.value.size },
        objectIds = objects.keys.toList().sorted()
    )


    companion object {
        /**
         * Create an empty game world
         */
        fun empty(): GameWorld = GameWorld()

        /**
         * Create a game world with initial objects
         */
        fun withObjects(objects: List<GameObject>): GameWorld {
            return GameWorld().withObjects(objects)
        }
    }
}

/**
 * Sealed class for world update operations
 */
@JsExport
sealed class WorldUpdate

@JsExport
data class AddObjectUpdate(val obj: GameObject) : WorldUpdate()

@JsExport
data class RemoveObjectUpdate(val objectId: String) : WorldUpdate()

@JsExport
data class UpdatePropertyUpdate(
    val objectId: String,
    val propertyName: String,
    val value: PropertyValue
) : WorldUpdate()

@JsExport
data class UpdateStateUpdate(
    val objectId: String,
    val stateName: String,
    val value: PropertyValue
) : WorldUpdate()

@JsExport
data class ReplaceObjectUpdate(
    val objectId: String,
    val newObject: GameObject
) : WorldUpdate()

/**
 * Snapshot of world state for debugging and analysis
 */
@JsExport
data class WorldSnapshot(
    val objectCount: Int,
    val objectsByType: Map<String, Int>,
    val objectIds: List<String>
)