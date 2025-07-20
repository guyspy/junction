package org.junction.catenin.model.objects

import kotlin.js.JsExport

/**
 * Predefined object instance with specific property values
 */
@JsExport
data class ObjectInstance(
    val objectType: String,
    val properties: Map<String, String> = emptyMap(),
    val states: Map<String, String> = emptyMap()
)