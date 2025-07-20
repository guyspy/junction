package org.junction.catenin.core.initialization

import kotlin.js.JsExport

/**
 * Configuration for creating participant-specific objects
 */
@JsExport
data class ParticipantObjectConfig(
    val type: String,
    val propertyPatterns: Map<String, String> = emptyMap(),
    val statePatterns: Map<String, String> = emptyMap(),
    val customId: String? = null
)