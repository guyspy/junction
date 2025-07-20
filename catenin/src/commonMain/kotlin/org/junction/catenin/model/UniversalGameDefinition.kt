package org.junction.catenin.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlin.js.JsExport

/**
 * Universal game definition based on object/property/trigger paradigm
 * Everything in the game is an object with properties that can change
 */
@Serializable
@JsExport
data class UniversalGameDefinition(
    val meta: GameMeta,
    @SerialName("object_types")
    val objectTypes: Map<String, ObjectDefinition>,
    val instances: Map<String, ObjectInstance> = emptyMap(),
    val triggers: List<TriggerDefinition> = emptyList(),
    val setup: SetupDefinition? = null,
    @SerialName("runtime_spawning")
    val runtimeSpawning: List<RuntimeSpawningRule> = emptyList()
)

@Serializable
@JsExport
data class GameMeta(
    val name: String,
    @SerialName("target_age")
    val targetAge: List<Int>, // [min, max]
    @SerialName("participant_count")
    val participantCount: List<Int>? = null // [min, max] - abstract participants/seats
)

/**
 * Defines a type of object (like "player", "card", "container")
 */
@Serializable
@JsExport
data class ObjectDefinition(
    val properties: Map<String, PropertyDefinition> = emptyMap(),
    val states: Map<String, StateDefinition> = emptyMap(),
    val triggers: List<TriggerDefinition> = emptyList()
)

/**
 * Instance of an object type with specific property values
 */
@Serializable
@JsExport
data class ObjectInstance(
    val template: String, // object type to use as template
    val properties: Map<String, String> = emptyMap(), // Store as strings
    val states: Map<String, String> = emptyMap(), // Store as strings
    val triggers: List<TriggerDefinition> = emptyList()
)

/**
 * Defines a property that objects can have
 */
@Serializable
@JsExport
data class PropertyDefinition(
    val type: PropertyType,
    val initial: String? = null,  // Store as string, parse based on type
    val min: String? = null,
    val max: String? = null,
    val values: List<String>? = null // for enum types
) {
    /**
     * Parse the initial value based on the property type
     */
    fun getInitialValue(): PropertyValue? {
        return initial?.let { parseValue(it, type) }
    }
    
    /**
     * Parse the min value based on the property type
     */
    fun getMinValue(): PropertyValue? {
        return min?.let { parseValue(it, type) }
    }
    
    /**
     * Parse the max value based on the property type
     */
    fun getMaxValue(): PropertyValue? {
        return max?.let { parseValue(it, type) }
    }
    
    private fun parseValue(value: String, propertyType: PropertyType): PropertyValue? {
        return try {
            when (propertyType) {
                PropertyType.INT -> PropertyValue.IntValue(value.toInt())
                PropertyType.STRING -> PropertyValue.StringValue(value)
                PropertyType.BOOL -> PropertyValue.BoolValue(value.toBoolean())
                PropertyType.OBJECT_REF -> PropertyValue.ObjectRefValue(value)
            }
        } catch (e: Exception) {
            // Handle parsing errors gracefully
            println("Warning: Could not parse '$value' as $propertyType: ${e.message}")
            null
        }
    }
}

@Serializable
@JsExport
enum class PropertyType {
    INT, STRING, BOOL, OBJECT_REF
}

@Serializable
@JsExport
sealed class PropertyValue {
    @Serializable
    data class IntValue(val value: Int) : PropertyValue()
    
    @Serializable
    data class StringValue(val value: String) : PropertyValue()
    
    @Serializable
    data class BoolValue(val value: Boolean) : PropertyValue()
    
    @Serializable
    data class ObjectRefValue(val objectId: String) : PropertyValue()
}

/**
 * Defines a state that objects can have (like tapped/untapped)
 */
@Serializable
@JsExport
data class StateDefinition(
    val type: PropertyType,
    val initial: String  // Store as string, parse based on type
) {
    /**
     * Parse the initial value based on the state type
     */
    fun getInitialValue(): PropertyValue {
        return when (type) {
            PropertyType.INT -> PropertyValue.IntValue(initial.toInt())
            PropertyType.STRING -> PropertyValue.StringValue(initial)
            PropertyType.BOOL -> PropertyValue.BoolValue(initial.toBoolean())
            PropertyType.OBJECT_REF -> PropertyValue.ObjectRefValue(initial)
        }
    }
}

/**
 * Defines when something should happen (trigger condition + effects)
 */
@Serializable
@JsExport
data class TriggerDefinition(
    val name: String? = null,
    @SerialName("when")
    val condition: TriggerCondition,
    val effects: List<EffectDefinition>
)

/**
 * Condition for when a trigger should fire
 */
@Serializable
@JsExport
data class TriggerCondition(
    @SerialName("object_type")
    val objectType: String? = null,
    @SerialName("property_changed")
    val propertyChanged: String? = null,
    @SerialName("new_value")
    val newValue: String? = null, // Store as string
    @SerialName("new_value_matches")
    val newValueMatches: ObjectMatcher? = null,
    val condition: String? = null // expression like "this.parent.name == 'battlefield'"
)

/**
 * Matches objects based on criteria
 */
@Serializable
@JsExport
data class ObjectMatcher(
    val type: String? = null,
    val name: String? = null,
    val owner: String? = null
)

/**
 * Effect to execute when trigger fires
 */
@Serializable
@JsExport
data class EffectDefinition(
    val log: String? = null,
    @SerialName("modify_property")
    val modifyProperty: ModifyPropertyEffect? = null,
    @SerialName("change_parent")
    val changeParent: ChangeParentEffect? = null,
    @SerialName("create_object")
    val createObject: CreateObjectEffect? = null,
    @SerialName("destroy_object") 
    val destroyObject: DestroyObjectEffect? = null
)

/**
 * Modify a property of an object
 */
@Serializable
@JsExport
data class ModifyPropertyEffect(
    val target: TargetDefinition,
    val property: String,
    val delta: String? = null,
    val value: String? = null // Store as string
)

/**
 * Change the parent of an object (for containment)
 */
@Serializable
@JsExport
data class ChangeParentEffect(
    val target: TargetDefinition,
    val new_parent: TargetDefinition? = null // null = remove from parent
)

/**
 * Create a new object instance
 */
@Serializable
@JsExport
data class CreateObjectEffect(
    val template: String,
    val id: String? = null, // null = auto-generate
    val properties: Map<String, String> = emptyMap(), // Store as strings
    val parent: TargetDefinition? = null
)

/**
 * Destroy an object instance
 */
@Serializable
@JsExport
data class DestroyObjectEffect(
    val target: TargetDefinition
)

/**
 * References to targets for effects
 */
@Serializable
@JsExport
data class TargetDefinition(
    val type: String? = null,
    val relation: String? = null, // "opponent", "self", etc.
    val id: String? = null,
    val property_match: Map<String, String>? = null // match by property values (stored as strings)
)

/**
 * Setup definition for game initialization
 */
@Serializable
@JsExport
data class SetupDefinition(
    @SerialName("world_initialization")
    val worldInitialization: List<CreateObjectsInstruction> = emptyList(),
    @SerialName("participant_initialization")
    val participantInitialization: List<CreateObjectsInstruction> = emptyList()
)

/**
 * Instruction to create objects during initialization
 */
@Serializable
@JsExport
data class CreateObjectsInstruction(
    @SerialName("create_objects")
    val createObjects: CreateObjectsRule
)

/**
 * Rule for creating objects
 */
@Serializable
@JsExport
data class CreateObjectsRule(
    val count: Int,
    val template: String,
    val properties: Map<String, String> = emptyMap(), // Store as strings
    @SerialName("instance_source")
    val instanceSource: String? = null, // Use predefined instance as template
    val parent: String? = null // Parent object ID pattern
)

/**
 * Runtime spawning rule - property-driven object creation
 */
@Serializable
@JsExport
data class RuntimeSpawningRule(
    val name: String,
    @SerialName("when")
    val condition: TriggerCondition,
    val effects: List<EffectDefinition>
)