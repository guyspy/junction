package org.junction.catenin.model

import kotlin.js.JsExport

/**
 * Game metadata and configuration
 */
@JsExport
data class GameMeta(
    val name: String,
    val targetAge: IntArray,
    val participantCount: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GameMeta

        if (name != other.name) return false
        if (!targetAge.contentEquals(other.targetAge)) return false
        if (!participantCount.contentEquals(other.participantCount)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + targetAge.contentHashCode()
        result = 31 * result + participantCount.contentHashCode()
        return result
    }
}

/**
 * Predefined object instance with specific property values
 */
@JsExport
data class ObjectInstance(
    val template: String,
    val properties: Map<String, String> = emptyMap(),
    val states: Map<String, String> = emptyMap()
)

/**
 * Trigger condition for when a trigger should fire
 */
@JsExport
data class TriggerCondition(
    val objectType: String? = null,
    val propertyChanged: String? = null,
    val newValue: String? = null,
    val condition: String? = null
)

/**
 * Effect to execute when trigger fires
 */
@JsExport
sealed class EffectDefinition

@JsExport
data class LogEffect(val message: String) : EffectDefinition()

@JsExport
data class ModifyPropertyEffect(
    val target: String,
    val property: String,
    val delta: String? = null,
    val value: String? = null
) : EffectDefinition()

/**
 * Trigger definition with condition and effects
 */
@JsExport
data class TriggerDefinition(
    val name: String? = null,
    val `when`: TriggerCondition,
    val effects: List<EffectDefinition>
)

/**
 * Complete universal game definition parsed from YAML
 */
@JsExport
data class UniversalGameDefinition(
    val meta: GameMeta,
    val objectTypes: Map<String, ObjectDefinition>,
    val instances: Map<String, ObjectInstance> = emptyMap(),
    val triggers: List<TriggerDefinition> = emptyList()
) {
    
    /**
     * Create a new UniversalGameDefinition with an additional object type
     */
    fun withObjectType(name: String, definition: ObjectDefinition): UniversalGameDefinition {
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
    fun getObjectType(name: String): ObjectDefinition? {
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