package org.junction.catenin.model.objects

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Predefined object instance with specific property values
 */
@Serializable
@JsExport
data class ObjectInstance(
    val objectType: String,
    val properties: Map<String, String> = emptyMap(),
    val states: Map<String, String> = emptyMap()
)