package org.junction.catenin.engine

import org.junction.catenin.core.*
import org.junction.catenin.model.definitions.PropertyType
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.triggers.*
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.js.JsExport

/**
 * Engine for executing effects and generating world updates
 */
@JsExport
class EffectEngine(
    private val schema: UniversalGameSchema,
    private val targetResolver: TargetResolver
) {
    
    /**
     * Generate world updates from an effect definition
     */
    fun executeEffect(
        effect: EffectDefinition,
        sourceObj: GameObject,
        world: GameWorld
    ): List<WorldUpdate> {
        return when (effect) {
            is LogEffect -> {
                // Log effects don't generate world updates
                // Applications can observe state changes and generate their own logs
                emptyList()
            }
            
            is ModifyPropertyEffect -> {
                executeModifyPropertyEffect(effect, sourceObj, world)
            }
            
            else -> {
                // Unknown effect type
                emptyList()
            }
        }
    }
    
    /**
     * Execute a property modification effect
     */
    private fun executeModifyPropertyEffect(
        effect: ModifyPropertyEffect,
        sourceObj: GameObject,
        world: GameWorld
    ): List<WorldUpdate> {
        val updates = mutableListOf<WorldUpdate>()
        
        // Resolve targets
        val targets = targetResolver.resolveTargets(effect.target, sourceObj, world)
        
        targets.forEach { targetObj ->
            // Get current property value
            val currentValue = targetObj.getProperty(effect.property)
            
            if (currentValue != null) {
                // Calculate new value
                val newValue = when {
                    effect.delta != null -> {
                        // Apply delta to current value
                        applyDelta(currentValue, effect.delta)
                    }
                    effect.value != null -> {
                        // Set to specific value
                        parsePropertyValue(effect.value, getPropertyType(currentValue))
                    }
                    else -> {
                        // No change
                        currentValue
                    }
                }
                
                if (newValue != currentValue) {
                    updates.add(UpdatePropertyUpdate(targetObj.id, effect.property, newValue))
                }
            }
        }
        
        return updates
    }
    
    /**
     * Apply a delta to a property value
     */
    private fun applyDelta(currentValue: PropertyValue, deltaStr: String): PropertyValue {
        return when (currentValue) {
            is IntValue -> {
                val delta = deltaStr.toIntOrNull() ?: 0
                IntValue(currentValue.value + delta)
            }
            is StringValue -> {
                // String concatenation
                StringValue(currentValue.value + deltaStr)
            }
            is BoolValue -> {
                // Toggle if delta is "toggle"
                if (deltaStr == "toggle") {
                    BoolValue(!currentValue.value)
                } else {
                    currentValue
                }
            }
            is ObjectRefValue -> {
                // Can't apply delta to object ref
                currentValue
            }
        }
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
    
    /**
     * Get property type from a value
     */
    private fun getPropertyType(value: PropertyValue): PropertyType {
        return when (value) {
            is IntValue -> PropertyType.INT
            is StringValue -> PropertyType.STRING
            is BoolValue -> PropertyType.BOOL
            is ObjectRefValue -> PropertyType.OBJECT_REF
        }
    }
}

/**
 * Interface for resolving effect targets
 */
@JsExport
interface TargetResolver {
    fun resolveTargets(
        targetSpec: String,
        sourceObj: GameObject,
        world: GameWorld
    ): List<GameObject>
}