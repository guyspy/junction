package org.junction.catenin.model.triggers

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Trigger condition for when a trigger should fire
 * 
 * Examples:
 * - Fire when any unit's health changes: {objectType: "unit", propertyChanged: "health"}
 * - Fire when health drops to 5: {propertyChanged: "health", newValue: "5"}
 * - Fire with complex condition: {condition: "source.health < 5 && source.armor > 0"}
 */
@Serializable
@JsExport
data class TriggerCondition(
    val objectType: String? = null,
    val propertyChanged: String? = null,
    val newValue: String? = null,  // String for YAML serialization, converted to PropertyValue for comparison
    val condition: String? = null   // Expression evaluated by ConditionEvaluator
)