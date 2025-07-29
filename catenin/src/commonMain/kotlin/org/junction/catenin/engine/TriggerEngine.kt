package org.junction.catenin.engine

import org.junction.catenin.core.*
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.triggers.EffectDefinition
import org.junction.catenin.model.triggers.TriggerDefinition
import org.junction.catenin.model.values.PropertyValue
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.js.JsExport

/**
 * Engine for evaluating triggers and generating world updates
 */
@JsExport
class TriggerEngine(
    private val schema: UniversalGameSchema,
    private val effectEngine: EffectEngine
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
                
                // Find all triggers that might fire for this property change
                val triggers = schema.getTriggersForPropertyChange(obj.type, update.propertyName)
                
                triggers.forEach { trigger ->
                    if (evaluateTriggerCondition(trigger, obj, update.propertyName, update.value)) {
                        // Generate updates for each effect
                        trigger.effects.forEach { effect ->
                            val effectUpdates = generateEffectUpdates(effect, obj, world)
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
        newValue: PropertyValue
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
            // Simple string comparison for now
            if (newValue.toString() != condition.newValue) {
                return false
            }
        }
        
        // TODO: Evaluate complex condition expressions
        // For now, if all specified conditions match, trigger fires
        return true
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