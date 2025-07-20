package org.junction.catenin.model.values

import kotlin.js.JsExport

/**
 * Reference to another object by ID
 */
@JsExport
data class ObjectRefValue(val objectId: String) : PropertyValue() {
    override fun toString(): String = objectId
}