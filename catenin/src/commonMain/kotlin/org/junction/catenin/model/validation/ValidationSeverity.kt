package org.junction.catenin.model.validation

import kotlin.js.JsExport

/**
 * Severity levels for validation issues
 */
@JsExport
enum class ValidationSeverity {
    WARNING,
    ERROR
}