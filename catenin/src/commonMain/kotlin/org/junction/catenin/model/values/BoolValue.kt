package org.junction.catenin.model.values

import kotlin.js.JsExport

/**
 * Boolean property value
 */
@JsExport
data class BoolValue(val value: Boolean) : PropertyValue() {
    override fun toString(): String = value.toString()
}