package org.junction.catenin.model.triggers

import kotlin.js.JsExport

/**
 * Trigger definition with condition and effects
 */
@JsExport
data class TriggerDefinition(
    val name: String? = null,
    val `when`: TriggerCondition,
    val effects: List<EffectDefinition>
)