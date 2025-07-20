package org.junction.catenin.model.values

import kotlin.js.JsExport

/**
 * Integer property value
 */
@JsExport
data class IntValue(val value: Int) : PropertyValue() {
    override fun toString(): String = value.toString()
}