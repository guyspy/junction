package org.junction.catenin.model.definitions

import org.junction.catenin.model.values.*
import kotlin.js.JsExport

/**
 * Definition of a property with type constraints and default values
 */
@JsExport
data class PropertyDefinition(
    val type: PropertyType,
    val initial: PropertyValue? = null,
    val min: PropertyValue? = null,
    val max: PropertyValue? = null
) {
    
    init {
        // Validate that initial value matches type
        initial?.let { value ->
            if (PropertyType.fromValue(value) != type) {
                throw IllegalArgumentException("Initial value type ${PropertyType.fromValue(value)} does not match property type $type")
            }
        }
        
        // Validate that constraints match type (for numeric types)
        min?.let { value ->
            if (PropertyType.fromValue(value) != type) {
                throw IllegalArgumentException("Min constraint type ${PropertyType.fromValue(value)} does not match property type $type")
            }
        }
        
        max?.let { value ->
            if (PropertyType.fromValue(value) != type) {
                throw IllegalArgumentException("Max constraint type ${PropertyType.fromValue(value)} does not match property type $type")
            }
        }
        
        // Validate min <= max for comparable types
        if (min != null && max != null && type in listOf(PropertyType.INT)) {
            if (min.compareTo(max) > 0) {
                throw IllegalArgumentException("Min value $min cannot be greater than max value $max")
            }
        }
    }
    
    /**
     * Check if a value is valid for this property definition
     */
    fun isValid(value: PropertyValue): Boolean {
        // Check type compatibility
        if (PropertyType.fromValue(value) != type) {
            return false
        }
        
        // Check constraints for numeric types
        if (type == PropertyType.INT) {
            min?.let { minVal ->
                if (value.compareTo(minVal) < 0) return false
            }
            max?.let { maxVal ->
                if (value.compareTo(maxVal) > 0) return false
            }
        }
        
        return true
    }
    
    /**
     * Get the default value for this property
     */
    fun getDefaultValue(): PropertyValue {
        return initial ?: when (type) {
            PropertyType.INT -> IntValue(0)
            PropertyType.STRING -> StringValue("")
            PropertyType.BOOL -> BoolValue(false)
            PropertyType.OBJECT_REF -> ObjectRefValue("")
        }
    }
}