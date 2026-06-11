package org.junction.catenin.model.values

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * String property value
 */
@Serializable
@SerialName("StringValue")
@JsExport
data class StringValue(val value: String) : PropertyValue() {
    override fun toString(): String = value
}