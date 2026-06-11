package org.junction.catenin.model.values

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Represents all possible property values in the universal game system
 */
@Serializable
@JsExport
sealed class PropertyValue : Comparable<PropertyValue> {
    
    abstract override fun toString(): String
    
    override fun compareTo(other: PropertyValue): Int {
        if (this::class != other::class) {
            throw IllegalArgumentException("Cannot compare different property value types: ${this::class.simpleName} and ${other::class.simpleName}")
        }
        
        return when (this) {
            is IntValue -> this.value.compareTo((other as IntValue).value)
            is StringValue -> this.value.compareTo((other as StringValue).value)
            is BoolValue -> this.value.compareTo((other as BoolValue).value)
            is ObjectRefValue -> this.objectId.compareTo((other as ObjectRefValue).objectId)
        }
    }
    
    // Type checking methods
    fun isInt(): Boolean = this is IntValue
    fun isString(): Boolean = this is StringValue
    fun isBool(): Boolean = this is BoolValue
    fun isObjectRef(): Boolean = this is ObjectRefValue
    
    // Safe conversion methods
    fun asInt(): Int {
        if (this !is IntValue) throw IllegalStateException("PropertyValue is not an IntValue")
        return this.value
    }
    
    fun asString(): String {
        if (this !is StringValue) throw IllegalStateException("PropertyValue is not a StringValue")
        return this.value
    }
    
    fun asBool(): Boolean {
        if (this !is BoolValue) throw IllegalStateException("PropertyValue is not a BoolValue")
        return this.value
    }
    
    fun asObjectRef(): String {
        if (this !is ObjectRefValue) throw IllegalStateException("PropertyValue is not an ObjectRefValue")
        return this.objectId
    }
}