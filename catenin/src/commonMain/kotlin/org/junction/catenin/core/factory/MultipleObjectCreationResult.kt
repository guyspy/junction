package org.junction.catenin.core.factory

import org.junction.catenin.model.objects.GameObject
import kotlin.js.JsExport

/**
 * Result of creating multiple objects, containing all objects and updated factory
 */
@JsExport
data class MultipleObjectCreationResult(
    val gameObjects: List<GameObject>,
    val updatedFactory: ObjectFactory
)