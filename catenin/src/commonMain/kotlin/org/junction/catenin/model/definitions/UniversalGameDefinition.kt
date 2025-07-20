package org.junction.catenin.model.definitions

import org.junction.catenin.model.objects.ObjectInstance
import org.junction.catenin.model.triggers.TriggerDefinition
import kotlin.js.JsExport

/**
 * Complete universal game definition parsed from YAML
 */
@JsExport
data class UniversalGameDefinition(
    val meta: GameMeta,
    val objectTypes: Map<String, ObjectTypeDefinition>,
    val instances: Map<String, ObjectInstance> = emptyMap(),
    val triggers: List<TriggerDefinition> = emptyList()
) {
    
    /**
     * Create a new UniversalGameDefinition with an additional object type
     */
    fun withObjectType(name: String, definition: ObjectTypeDefinition): UniversalGameDefinition {
        return copy(objectTypes = objectTypes + (name to definition))
    }
    
    /**
     * Create a new UniversalGameDefinition with an additional instance
     */
    fun withInstance(name: String, instance: ObjectInstance): UniversalGameDefinition {
        return copy(instances = instances + (name to instance))
    }
    
    /**
     * Create a new UniversalGameDefinition with an additional trigger
     */
    fun withTrigger(trigger: TriggerDefinition): UniversalGameDefinition {
        return copy(triggers = triggers + trigger)
    }
    
    /**
     * Create a new UniversalGameDefinition with an object type removed
     */
    fun withoutObjectType(name: String): UniversalGameDefinition {
        return copy(objectTypes = objectTypes - name)
    }
    
    /**
     * Create a new UniversalGameDefinition with an instance removed
     */
    fun withoutInstance(name: String): UniversalGameDefinition {
        return copy(instances = instances - name)
    }
    
    /**
     * Get object type definition by name
     */
    fun getObjectType(name: String): ObjectTypeDefinition? {
        return objectTypes[name]
    }
    
    /**
     * Get object instance by name
     */
    fun getInstance(name: String): ObjectInstance? {
        return instances[name]
    }
    
    /**
     * Check if object type exists
     */
    fun hasObjectType(name: String): Boolean {
        return objectTypes.containsKey(name)
    }
    
    /**
     * Check if instance exists
     */
    fun hasInstance(name: String): Boolean {
        return instances.containsKey(name)
    }
    
    /**
     * Get all object type names as a list
     */
    fun getAllObjectTypeNames(): List<String> {
        return objectTypes.keys.toList()
    }
    
    /**
     * Get all instance names as a list
     */
    fun getAllInstanceNames(): List<String> {
        return instances.keys.toList()
    }
    
    /**
     * Get all triggers for a specific object type
     */
    fun getTriggersForObjectType(objectType: String): List<TriggerDefinition> {
        return triggers.filter { trigger ->
            trigger.`when`.objectType == objectType || trigger.`when`.objectType == null
        }
    }
    
    /**
     * Get all triggers that might fire for a property change
     */
    fun getTriggersForPropertyChange(objectType: String, propertyName: String): List<TriggerDefinition> {
        return triggers.filter { trigger ->
            // Must match object type (or be global)
            (trigger.`when`.objectType == objectType || trigger.`when`.objectType == null) &&
            // Must be listening for this property change (or any property change)
            (trigger.`when`.propertyChanged == propertyName || trigger.`when`.propertyChanged == null)
        }
    }
}