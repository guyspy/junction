package org.junction.catenin.model.triggers

import kotlin.js.JsExport

@JsExport
data class LogEffect(val message: String) : EffectDefinition()