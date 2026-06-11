package org.junction.catenin.model.triggers

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Trigger definition with condition and effects
 */
@Serializable
@JsExport
data class TriggerDefinition(
    val name: String? = null,
    val `when`: TriggerCondition,
    val effects: List<EffectDefinition>
)