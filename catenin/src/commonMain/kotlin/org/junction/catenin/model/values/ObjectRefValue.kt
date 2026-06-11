package org.junction.catenin.model.values

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Reference to another object by ID
 */
@Serializable
@SerialName("ObjectRefValue")
@JsExport
data class ObjectRefValue(val objectId: String) : PropertyValue() {
    override fun toString(): String = objectId
}