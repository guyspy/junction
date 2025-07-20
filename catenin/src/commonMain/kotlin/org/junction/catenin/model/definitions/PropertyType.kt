package org.junction.catenin.model.definitions

import org.junction.catenin.model.values.*
import kotlin.js.JsExport

/**
 * Defines the type and constraints for a property
 */
@JsExport
enum class PropertyType {
    INT,
    STRING,
    BOOL,
    OBJECT_REF;
    
    companion object {
        fun fromValue(value: PropertyValue): PropertyType {
            return when (value) {
                is IntValue -> INT
                is StringValue -> STRING
                is BoolValue -> BOOL
                is ObjectRefValue -> OBJECT_REF
            }
        }
    }
}