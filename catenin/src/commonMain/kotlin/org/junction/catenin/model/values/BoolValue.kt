package org.junction.catenin.model.values

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Boolean property value
 */
@Serializable
@SerialName("BoolValue")
@JsExport
data class BoolValue(val value: Boolean) : PropertyValue() {
    override fun toString(): String = value.toString()
}