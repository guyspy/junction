package org.junction.catenin.core

import org.junction.catenin.model.objects.GameObject

/**
 * Internal implementation with shared logic
 */
internal class GameWorldImpl(
    internal val objects: Map<String, GameObject> = emptyMap()
) {
    fun applyUpdateInternal(update: WorldUpdate): GameWorldImpl = transform { world ->
        when (update) {
            is AddObjectUpdate -> world.copy(
                objects = world.objects + (update.obj.id to update.obj)
            )
            is RemoveObjectUpdate -> world.copy(
                objects = world.objects - update.objectId
            )
            is UpdatePropertyUpdate -> {
                val obj = world.objects[update.objectId]
                if (obj != null) {
                    val updatedObj = obj.withProperty(update.propertyName, update.value)
                    world.copy(objects = world.objects + (update.objectId to updatedObj))
                } else {
                    world
                }
            }
            is UpdateStateUpdate -> {
                val obj = world.objects[update.objectId]
                if (obj != null) {
                    val updatedObj = obj.withState(update.stateName, update.value)
                    world.copy(objects = world.objects + (update.objectId to updatedObj))
                } else {
                    world
                }
            }
            is ReplaceObjectUpdate -> {
                if (world.objects.containsKey(update.objectId)) {
                    world.copy(objects = world.objects + (update.objectId to update.newObject))
                } else {
                    world
                }
            }
        }
    }
    
    private fun transform(block: (GameWorldImpl) -> GameWorldImpl): GameWorldImpl {
        return block(this)
    }
    
    private fun copy(objects: Map<String, GameObject> = this.objects): GameWorldImpl {
        return GameWorldImpl(objects)
    }
    
    companion object {
        fun empty(): GameWorldImpl = GameWorldImpl()
    }
}