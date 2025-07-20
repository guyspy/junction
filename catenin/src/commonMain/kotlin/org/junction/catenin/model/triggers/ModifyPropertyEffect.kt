package org.junction.catenin.model.triggers

import kotlin.js.JsExport

@JsExport
data class ModifyPropertyEffect(
    val target: String,
    val property: String,
    val delta: String? = null,
    val value: String? = null
) : EffectDefinition()