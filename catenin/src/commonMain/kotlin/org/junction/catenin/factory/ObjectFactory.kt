package org.junction.catenin.factory

import org.junction.catenin.model.definitions.PropertyType
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.js.JsExport

/**
 * Domain-agnostic factory for creating game objects from schema definitions
 */
@JsExport
class ObjectFactory(private val schema: UniversalGameSchema) {
    private var nextId = 1
    
    /**
     * Create an object from an object type definition
     */
    fun createObject(
        objectType: String,
        id: String? = null,
        propertyOverrides: Map<String, String> = emptyMap()
    ): GameObject {
        val typeDef = schema.getObjectType(objectType)
            ?: throw IllegalArgumentException("Unknown object type: $objectType")
        
        // Generate ID if not provided
        val objectId = id ?: generateId(objectType)
        
        // Build properties from definition + overrides
        val properties = mutableMapOf<String, PropertyValue>()
        typeDef.properties.forEach { (name, propDef) ->
            // Use override if provided, otherwise use initial value from definition
            val value = propertyOverrides[name]?.let { parsePropertyValue(it, propDef.type) }
                ?: propDef.initial
                ?: propDef.getDefaultValue()
            properties[name] = value
        }
        
        // Build states from definition
        val states = mutableMapOf<String, PropertyValue>()
        typeDef.states.forEach { (name, stateDef) ->
            // Use initial value from definition or default
            val value = stateDef.initial ?: stateDef.getDefaultValue()
            states[name] = value
        }
        
        return GameObject(
            id = objectId,
            type = objectType,
            properties = properties,
            states = states
        )
    }
    
    /**
     * Create an object from a predefined instance
     */
    fun createFromInstance(
        instanceName: String,
        id: String? = null
    ): GameObject {
        val instance = schema.getInstance(instanceName)
            ?: throw IllegalArgumentException("Unknown instance: $instanceName")
        
        // Use instance properties as overrides
        return createObject(
            objectType = instance.objectType,
            id = id,
            propertyOverrides = instance.properties
        )
    }
    
    /**
     * Generate a unique ID for an object type
     */
    private fun generateId(objectType: String): String {
        return "${objectType}_${nextId++}"
    }
    
    /**
     * Parse a string value into the appropriate PropertyValue type
     */
    private fun parsePropertyValue(value: String, type: PropertyType): PropertyValue {
        return when (type) {
            PropertyType.INT -> IntValue(value.toIntOrNull() ?: 0)
            PropertyType.STRING -> StringValue(value)
            PropertyType.BOOL -> BoolValue(value.toBoolean())
            PropertyType.OBJECT_REF -> ObjectRefValue(value)
        }
    }
}