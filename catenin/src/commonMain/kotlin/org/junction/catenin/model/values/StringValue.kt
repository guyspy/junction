package org.junction.catenin.model.values

import kotlin.js.JsExport

/**
 * String property value
 */
@JsExport
data class StringValue(val value: String) : PropertyValue() {
    override fun toString(): String = value
}