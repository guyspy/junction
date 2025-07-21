package org.junction.catenin.model.triggers

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
@JsExport
data class LogEffect(val message: String) : EffectDefinition()