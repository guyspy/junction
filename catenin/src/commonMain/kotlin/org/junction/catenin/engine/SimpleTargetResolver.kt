package org.junction.catenin.engine

import org.junction.catenin.core.GameWorld
import org.junction.catenin.model.objects.GameObject
import kotlin.js.JsExport

/**
 * Simple implementation of target resolver with basic target types
 */
@JsExport
class SimpleTargetResolver : TargetResolver {
    
    override fun resolveTargets(
        targetSpec: String,
        sourceObj: GameObject,
        world: GameWorld
    ): List<GameObject> {
        return when (targetSpec) {
            "self" -> listOf(sourceObj)
            
            "all" -> world.getAllObjects()
            
            else -> {
                // Check if it's a specific object ID
                val obj = world.getObject(targetSpec)
                if (obj != null) {
                    listOf(obj)
                } else if (targetSpec.startsWith("type:")) {
                    // Target all objects of a specific type
                    val typeName = targetSpec.substring(5)
                    world.getObjectsByType(typeName)
                } else {
                    // Unknown target spec
                    emptyList()
                }
            }
        }
    }
}