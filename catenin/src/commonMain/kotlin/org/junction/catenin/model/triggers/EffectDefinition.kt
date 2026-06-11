package org.junction.catenin.model.triggers

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Effect to execute when trigger fires
 */
@Serializable
@JsExport
sealed class EffectDefinition