package org.junction.catenin.model.validation

import kotlin.js.JsExport

/**
 * Result of validation containing errors and warnings
 */
@JsExport
data class ValidationResult(
    val issues: List<ValidationError>,
    val isValid: Boolean = issues.none { it.severity == ValidationSeverity.ERROR }
) {
    
    /**
     * Get only the error-level validation issues
     */
    fun getErrors(): List<ValidationError> {
        return issues.filter { it.severity == ValidationSeverity.ERROR }
    }
    
    /**
     * Get only the warning-level validation issues
     */
    fun getWarnings(): List<ValidationError> {
        return issues.filter { it.severity == ValidationSeverity.WARNING }
    }
    
    companion object {
        fun valid(): ValidationResult = ValidationResult(emptyList())
        
        fun error(message: String, path: String? = null): ValidationResult =
            ValidationResult(listOf(ValidationError(message, path, ValidationSeverity.ERROR)))
            
        fun warning(message: String, path: String? = null): ValidationResult =
            ValidationResult(listOf(ValidationError(message, path, ValidationSeverity.WARNING)))
    }
}