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
    val triggers: List<TriggerDefinition> = emptyList()
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
    val properties: Map<String, PropertyValue> = emptyMap(),
    val states: Map<String, PropertyValue> = emptyMap(),
    val triggers: List<TriggerDefinition> = emptyList()
)

/**
 * Defines a property that objects can have
 */
@Serializable
@JsExport
data class PropertyDefinition(
    val type: PropertyType,
    val initial: PropertyValue? = null,
    val min: PropertyValue? = null,
    val max: PropertyValue? = null,
    val values: List<String>? = null // for enum types
)

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
    val initial: PropertyValue
)

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
    val newValue: PropertyValue? = null,
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
    val value: PropertyValue? = null
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
    val properties: Map<String, PropertyValue> = emptyMap(),
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
    val property_match: Map<String, PropertyValue>? = null // match by property values
)