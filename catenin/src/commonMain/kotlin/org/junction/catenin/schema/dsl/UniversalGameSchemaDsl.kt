package org.junction.catenin.schema.dsl

import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.ObjectInstance
import org.junction.catenin.model.triggers.TriggerDefinition
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema

/**
 * DSL for building UniversalGameSchema in a type-safe, fluent way
 */
class UniversalGameSchemaDslBuilder {
    private var meta: GameMeta? = null
    private val objectTypes = mutableMapOf<String, ObjectTypeDefinition>()
    private val instances = mutableMapOf<String, ObjectInstance>()
    private val triggers = mutableListOf<TriggerDefinition>()
    
    fun meta(block: GameMetaDslBuilder.() -> Unit) {
        meta = GameMetaDslBuilder().apply(block).build()
    }
    
    fun objectTypes(block: ObjectTypesDslBuilder.() -> Unit) {
        objectTypes.putAll(ObjectTypesDslBuilder().apply(block).build())
    }
    
    fun instances(block: InstancesDslBuilder.() -> Unit) {
        instances.putAll(InstancesDslBuilder().apply(block).build())
    }
    
    fun triggers(block: TriggersDslBuilder.() -> Unit) {
        triggers.addAll(TriggersDslBuilder().apply(block).build())
    }
    
    fun build(): UniversalGameSchema {
        return UniversalGameSchema(
            meta = meta ?: throw IllegalStateException("Meta information is required"),
            objectTypes = objectTypes.toMap(),
            instances = instances.toMap(),
            triggers = triggers.toList()
        )
    }
}

/**
 * Game metadata DSL builder
 */
class GameMetaDslBuilder {
    var name: String? = null
    var targetAge: IntArray? = null
    var participantCount: IntArray? = null
    
    fun build(): GameMeta {
        return GameMeta(
            name = name ?: throw IllegalStateException("Game name is required"),
            targetAge = targetAge ?: throw IllegalStateException("Target age is required"),
            participantCount = participantCount ?: throw IllegalStateException("Participant count is required")
        )
    }
}

/**
 * Object types DSL builder
 */
class ObjectTypesDslBuilder {
    private val objectTypes = mutableMapOf<String, ObjectTypeDefinition>()
    
    fun objectType(name: String, block: ObjectTypeDefinitionDslBuilder.() -> Unit) {
        objectTypes[name] = ObjectTypeDefinitionDslBuilder().apply(block).build()
    }
    
    
    fun build(): Map<String, ObjectTypeDefinition> = objectTypes.toMap()
}

/**
 * Object type definition DSL builder
 */
class ObjectTypeDefinitionDslBuilder {
    private val properties = mutableMapOf<String, PropertyDefinition>()
    private val states = mutableMapOf<String, PropertyDefinition>()
    
    fun properties(block: PropertiesDslBuilder.() -> Unit) {
        properties.putAll(PropertiesDslBuilder().apply(block).build())
    }
    
    fun states(block: PropertiesDslBuilder.() -> Unit) {
        states.putAll(PropertiesDslBuilder().apply(block).build())
    }
    
    fun build(): ObjectTypeDefinition {
        return ObjectTypeDefinition(
            properties = properties.toMap(),
            states = states.toMap()
        )
    }
}

/**
 * Properties DSL builder (used for both properties and states)
 */
class PropertiesDslBuilder {
    private val properties = mutableMapOf<String, PropertyDefinition>()
    
    // Type-safe property builders
    fun int(name: String, initial: Int? = null, min: Int? = null, max: Int? = null) {
        properties[name] = PropertyDefinition(
            type = PropertyType.INT,
            initial = initial?.let { IntValue(it) },
            min = min?.let { IntValue(it) },
            max = max?.let { IntValue(it) }
        )
    }
    
    fun string(name: String, initial: String? = null) {
        properties[name] = PropertyDefinition(
            type = PropertyType.STRING,
            initial = initial?.let { StringValue(it) }
        )
    }
    
    fun bool(name: String, initial: Boolean? = null) {
        properties[name] = PropertyDefinition(
            type = PropertyType.BOOL,
            initial = initial?.let { BoolValue(it) }
        )
    }
    
    fun objectRef(name: String, initial: String? = null) {
        properties[name] = PropertyDefinition(
            type = PropertyType.OBJECT_REF,
            initial = initial?.let { ObjectRefValue(it) }
        )
    }
    
    // Generic property builder
    fun property(name: String, type: PropertyType, initial: PropertyValue? = null, min: PropertyValue? = null, max: PropertyValue? = null) {
        properties[name] = PropertyDefinition(type, initial, min, max)
    }
    
    fun build(): Map<String, PropertyDefinition> = properties.toMap()
}

/**
 * Instances DSL builder
 */
class InstancesDslBuilder {
    private val instances = mutableMapOf<String, ObjectInstance>()
    
    fun instance(name: String, objectType: String, block: InstanceDslBuilder.() -> Unit = {}) {
        instances[name] = InstanceDslBuilder(objectType).apply(block).build()
    }
    
    
    fun build(): Map<String, ObjectInstance> = instances.toMap()
}

/**
 * Instance DSL builder
 */
class InstanceDslBuilder(private val objectType: String) {
    private val properties = mutableMapOf<String, String>()
    private val states = mutableMapOf<String, String>()
    
    fun properties(block: StringMapDslBuilder.() -> Unit) {
        properties.putAll(StringMapDslBuilder().apply(block).build())
    }
    
    fun states(block: StringMapDslBuilder.() -> Unit) {
        states.putAll(StringMapDslBuilder().apply(block).build())
    }
    
    fun build(): ObjectInstance {
        return ObjectInstance(
            objectType = objectType,
            properties = properties.toMap(),
            states = states.toMap()
        )
    }
}

/**
 * String map DSL builder (for instance properties/states)
 */
class StringMapDslBuilder {
    private val map = mutableMapOf<String, String>()
    
    infix fun String.to(value: Any) {
        map[this] = value.toString()
    }
    
    fun build(): Map<String, String> = map.toMap()
}

/**
 * Triggers DSL builder (basic structure for future implementation)
 */
class TriggersDslBuilder {
    private val triggers = mutableListOf<TriggerDefinition>()
    
    // TODO: Implement trigger DSL when needed
    fun build(): List<TriggerDefinition> = triggers.toList()
}

/**
 * Top-level DSL function to create a UniversalGameSchema
 */
fun universalGameSchema(block: UniversalGameSchemaDslBuilder.() -> Unit): UniversalGameSchema {
    return UniversalGameSchemaDslBuilder().apply(block).build()
}