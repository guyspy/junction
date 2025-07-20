package org.junction.catenin.core.factory

import org.junction.catenin.model.objects.GameObject
import kotlin.js.JsExport

/**
 * Result of creating a single object, containing both the object and updated factory
 */
@JsExport
data class ObjectCreationResult(
    val gameObject: GameObject,
    val updatedFactory: ObjectFactory
)