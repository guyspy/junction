package org.junction.catenin.core

import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.PropertyValue
import kotlin.js.JsExport

/**
 * Concrete update types
 */
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