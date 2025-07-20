package org.junction.catenin.model.validation

import kotlin.js.JsExport

/**
 * Validation error with details
 */
@JsExport
data class ValidationError(
    val message: String,
    val path: String? = null,
    val severity: ValidationSeverity = ValidationSeverity.ERROR
)