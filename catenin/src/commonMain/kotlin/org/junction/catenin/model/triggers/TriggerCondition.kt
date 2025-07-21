package org.junction.catenin.model.triggers

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Trigger condition for when a trigger should fire
 */
@Serializable
@JsExport
data class TriggerCondition(
    val objectType: String? = null,
    val propertyChanged: String? = null,
    val newValue: String? = null,
    val condition: String? = null
)