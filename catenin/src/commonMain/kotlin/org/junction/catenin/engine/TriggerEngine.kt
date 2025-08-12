package org.junction.catenin.engine

import org.junction.catenin.core.*
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.triggers.EffectDefinition
import org.junction.catenin.model.triggers.TriggerDefinition
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.js.JsExport

/**
 * Engine for evaluating triggers and generating world updates
 */
@JsExport
class TriggerEngine(
    private val schema: UniversalGameSchema,
    private val effectEngine: EffectEngine,
    private val conditionEvaluator: ConditionEvaluator = ConditionEvaluator()
) {
    
    /**
     * Evaluate all triggers for a world update and generate additional updates
     */
    fun evaluateUpdate(world: GameWorld, update: WorldUpdate): List<WorldUpdate> {
        val triggeredUpdates = mutableListOf<WorldUpdate>()
        
        when (update) {
            is UpdatePropertyUpdate -> {
                // Find object to get its type
                val obj = world.getObject(update.objectId) ?: return emptyList()
                
                // Create a temporary object with the new property value for condition evaluation
                val updatedObj = obj.withProperty(update.propertyName, update.value)
                
                // Find all triggers that might fire for this property change
                val triggers = schema.getTriggersForPropertyChange(obj.type, update.propertyName)
                
                triggers.forEach { trigger ->
                    if (evaluateTriggerCondition(trigger, updatedObj, update.propertyName, update.value, world)) {
                        // Generate updates for each effect
                        trigger.effects.forEach { effect ->
                            val effectUpdates = generateEffectUpdates(effect, updatedObj, world)
                            triggeredUpdates.addAll(effectUpdates)
                        }
                    }
                }
            }
            else -> {
                // Other update types don't trigger effects yet
            }
        }
        
        return triggeredUpdates
    }
    
    /**
     * Check if a trigger condition is satisfied
     */
    private fun evaluateTriggerCondition(
        trigger: TriggerDefinition,
        obj: GameObject,
        propertyName: String,
        newValue: PropertyValue,
        world: GameWorld
    ): Boolean {
        val condition = trigger.`when`
        
        // Check object type matches (if specified)
        if (condition.objectType != null && condition.objectType != obj.type) {
            return false
        }
        
        // Check property name matches (if specified)
        if (condition.propertyChanged != null && condition.propertyChanged != propertyName) {
            return false
        }
        
        // Check new value matches (if specified)
        if (condition.newValue != null) {
            // Compare as PropertyValue for proper type comparison
            val expectedValue = parsePropertyValue(condition.newValue, newValue)
            if (!propertyValuesEqual(newValue, expectedValue)) {
                return false
            }
        }
        
        // Evaluate complex condition expression if present
        if (condition.condition != null) {
            val context = ConditionContext(
                sourceObject = obj,
                targetObject = null,
                world = world,
                metadata = mapOf(
                    "propertyName" to propertyName,
                    "newValue" to newValue
                )
            )
            return conditionEvaluator.evaluate(condition.condition, context)
        }
        
        // If all specified conditions match, trigger fires
        return true
    }
    
    /**
     * Parse a string value into the appropriate PropertyValue type
     */
    private fun parsePropertyValue(value: String, referenceValue: PropertyValue): PropertyValue {
        return when (referenceValue) {
            is IntValue -> IntValue(value.toIntOrNull() ?: 0)
            is StringValue -> StringValue(value)
            is BoolValue -> BoolValue(value.toBoolean())
            is ObjectRefValue -> ObjectRefValue(value)
        }
    }
    
    /**
     * Compare two PropertyValues for equality
     */
    private fun propertyValuesEqual(a: PropertyValue, b: PropertyValue): Boolean {
        return when (a) {
            is IntValue -> b is IntValue && a.value == b.value
            is StringValue -> b is StringValue && a.value == b.value
            is BoolValue -> b is BoolValue && a.value == b.value
            is ObjectRefValue -> b is ObjectRefValue && a.objectId == b.objectId
        }
    }
    
    /**
     * Generate world updates from an effect definition
     */
    private fun generateEffectUpdates(
        effect: EffectDefinition,
        sourceObj: GameObject,
        world: GameWorld
    ): List<WorldUpdate> {
        return effectEngine.executeEffect(effect, sourceObj, world)
    }
}