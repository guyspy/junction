package org.junction.catenin.model.values

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Integer property value
 */
@Serializable
@SerialName("IntValue")
@JsExport
data class IntValue(val value: Int) : PropertyValue() {
    override fun toString(): String = value.toString()
}