package org.junction.catenin.schema

import org.junction.catenin.engine.EmptyInitializationConfig
import org.junction.catenin.engine.InitializationConfig
import org.junction.catenin.model.definitions.GameMeta
import org.junction.catenin.model.definitions.ObjectTypeDefinition
import org.junction.catenin.model.objects.ObjectInstance
import org.junction.catenin.model.triggers.TriggerDefinition
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Complete universal game definition parsed from YAML
 */
@Serializable
@JsExport
data class UniversalGameSchema(
    val meta: GameMeta,
    val objectTypes: Map<String, ObjectTypeDefinition>,
    val instances: Map<String, ObjectInstance> = emptyMap(),
    val triggers: List<TriggerDefinition> = emptyList(),
    val initialization: InitializationConfig = EmptyInitializationConfig.INSTANCE
) {

    /**
     * Create a new UniversalGameSchema with an additional object type
     */
    fun withObjectType(name: String, definition: ObjectTypeDefinition): UniversalGameSchema = copy(objectTypes = objectTypes + (name to definition))

    /**
     * Create a new UniversalGameSchema with an additional instance
     */
    fun withInstance(name: String, instance: ObjectInstance): UniversalGameSchema = copy(instances = instances + (name to instance))

    /**
     * Create a new UniversalGameSchema with an additional trigger
     */
    fun withTrigger(trigger: TriggerDefinition): UniversalGameSchema = copy(triggers = triggers + trigger)

    /**
     * Create a new UniversalGameSchema with initialization configuration
     */
    fun withInitialization(initialization: InitializationConfig): UniversalGameSchema = copy(initialization = initialization)

    /**
     * Create a new UniversalGameSchema with an object type removed
     */
    fun withoutObjectType(name: String): UniversalGameSchema = copy(objectTypes = objectTypes - name)

    /**
     * Create a new UniversalGameSchema with an instance removed
     */
    fun withoutInstance(name: String): UniversalGameSchema = copy(instances = instances - name)

    /**
     * Get object type definition by name
     */
    fun getObjectType(name: String): ObjectTypeDefinition? = objectTypes[name]

    /**
     * Get object instance by name
     */
    fun getInstance(name: String): ObjectInstance? = instances[name]

    /**
     * Check if object type exists
     */
    fun hasObjectType(name: String): Boolean = objectTypes.containsKey(name)

    /**
     * Check if instance exists
     */
    fun hasInstance(name: String): Boolean = instances.containsKey(name)

    /**
     * Get all object type names as a list
     */
    fun getAllObjectTypeNames(): List<String> = objectTypes.keys.toList()

    /**
     * Get all instance names as a list
     */
    fun getAllInstanceNames(): List<String> = instances.keys.toList()

    /**
     * Get all triggers for a specific object type
     */
    fun getTriggersForObjectType(objectType: String): List<TriggerDefinition> =
        triggers.filter { trigger ->
            trigger.`when`.objectType == objectType || trigger.`when`.objectType == null
        }

    /**
     * Get all triggers that might fire for a property change
     */
    fun getTriggersForPropertyChange(objectType: String, propertyName: String): List<TriggerDefinition> =
        triggers.filter { trigger ->
            // Must match object type (or be global)
            (trigger.`when`.objectType == objectType || trigger.`when`.objectType == null) &&
            // Must be listening for this property change (or any property change)
            (trigger.`when`.propertyChanged == propertyName || trigger.`when`.propertyChanged == null)
        }
}