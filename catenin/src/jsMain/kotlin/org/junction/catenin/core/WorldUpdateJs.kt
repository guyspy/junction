package org.junction.catenin.core

import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.PropertyValue
import kotlin.js.JsExport

/**
 * JavaScript exports for WorldUpdate classes
 */

@JsExport
fun createAddObjectUpdate(obj: GameObject): WorldUpdate = AddObjectUpdate(obj)

@JsExport
fun createRemoveObjectUpdate(objectId: String): WorldUpdate = RemoveObjectUpdate(objectId)

@JsExport
fun createUpdatePropertyUpdate(
    objectId: String,
    propertyName: String,
    value: PropertyValue
): WorldUpdate = UpdatePropertyUpdate(objectId, propertyName, value)

@JsExport
fun createUpdateStateUpdate(
    objectId: String,
    stateName: String,
    value: PropertyValue
): WorldUpdate = UpdateStateUpdate(objectId, stateName, value)

@JsExport
fun createReplaceObjectUpdate(
    objectId: String,
    newObject: GameObject
): WorldUpdate = ReplaceObjectUpdate(objectId, newObject)