package org.junction.catenin.core

import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.PropertyValue

/**
 * Concrete update types
 */
data class AddObjectUpdate(val obj: GameObject) : WorldUpdate()

data class RemoveObjectUpdate(val objectId: String) : WorldUpdate()

data class UpdatePropertyUpdate(
    val objectId: String,
    val propertyName: String,
    val value: PropertyValue
) : WorldUpdate()

data class UpdateStateUpdate(
    val objectId: String,
    val stateName: String,
    val value: PropertyValue
) : WorldUpdate()

data class ReplaceObjectUpdate(
    val objectId: String,
    val newObject: GameObject
) : WorldUpdate()