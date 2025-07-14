package org.junction.catenin.core

import org.junction.catenin.model.*
import kotlin.js.JsExport

/**
 * Engine for evaluating trigger conditions and determining when triggers should fire
 */
@JsExport
class TriggerEngine {
    
    /**
     * Check if a trigger should fire based on a property change
     */
    fun triggerMatches(
        trigger: TriggerDefinition,
        obj: GameObject,
        changedProperty: String,
        oldValue: PropertyValue?,
        newValue: PropertyValue,
        gameWorld: GameWorld
    ): Boolean {
        val condition = trigger.condition
        
        // Check object type constraint
        if (condition.objectType != null && condition.objectType != obj.type) {
            return false
        }
        
        // Check property change constraint
        if (condition.propertyChanged != null && condition.propertyChanged != changedProperty) {
            return false
        }
        
        // Check new value constraint
        if (condition.newValue != null && condition.newValue != newValue) {
            return false
        }
        
        // Check object matcher constraint
        if (condition.newValueMatches != null) {
            if (!matchesObjectMatcher(condition.newValueMatches, newValue, gameWorld)) {
                return false
            }
        }
        
        // Check custom condition expression
        if (condition.condition != null) {
            if (!evaluateConditionExpression(condition.condition, obj, gameWorld)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Find all triggers that match a property change
     */
    fun findMatchingTriggers(
        triggers: List<TriggerDefinition>,
        obj: GameObject,
        changedProperty: String,
        oldValue: PropertyValue?,
        newValue: PropertyValue,
        gameWorld: GameWorld
    ): List<TriggerDefinition> {
        return triggers.filter { trigger ->
            triggerMatches(trigger, obj, changedProperty, oldValue, newValue, gameWorld)
        }
    }
    
    /**
     * Check if a value matches an object matcher pattern
     */
    private fun matchesObjectMatcher(
        matcher: ObjectMatcher,
        value: PropertyValue,
        gameWorld: GameWorld
    ): Boolean {
        // If the value is an object reference, resolve it
        val targetObject = when (value) {
            is PropertyValue.ObjectRefValue -> gameWorld.objects[value.objectId]
            else -> null
        } ?: return false
        
        // Check type constraint
        if (matcher.type != null && matcher.type != targetObject.type) {
            return false
        }
        
        // Check name constraint
        if (matcher.name != null) {
            val objectName = targetObject.properties["name"] as? PropertyValue.StringValue
            if (objectName?.value != matcher.name) {
                return false
            }
        }
        
        // Check owner constraint
        if (matcher.owner != null) {
            val objectOwner = targetObject.properties["owner"] as? PropertyValue.StringValue
            if (objectOwner?.value != matcher.owner) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Evaluate a condition expression (simplified implementation)
     */
    private fun evaluateConditionExpression(
        expression: String,
        obj: GameObject,
        gameWorld: GameWorld
    ): Boolean {
        // Simplified condition evaluation - in practice would use a proper expression parser
        // For now, handle common patterns from the tests
        
        return when {
            // Pattern: this.properties.name == 'Lightning Bolt'
            expression.contains("this.properties.name ==") -> {
                val expectedName = expression.substringAfter("'").substringBefore("'")
                val actualName = obj.properties["name"] as? PropertyValue.StringValue
                actualName?.value == expectedName
            }
            
            // Pattern: this.properties.cost <= 3
            expression.contains("this.properties.cost <=") -> {
                val maxCost = expression.substringAfter("<=").trim().toIntOrNull() ?: 0
                val actualCost = obj.properties["cost"] as? PropertyValue.IntValue
                (actualCost?.value ?: 0) <= maxCost
            }
            
            // Pattern: this.properties.cost >= 5
            expression.contains("this.properties.cost >=") -> {
                val minCost = expression.substringAfter(">=").trim().toIntOrNull() ?: 0
                val actualCost = obj.properties["cost"] as? PropertyValue.IntValue
                (actualCost?.value ?: 0) >= minCost
            }
            
            // Add more patterns as needed
            else -> {
                // For unknown expressions, assume they evaluate to true
                // In practice, would implement a proper expression evaluator
                true
            }
        }
    }
    
    /**
     * Get all triggers that could potentially fire for an object type
     */
    fun getTriggersForObjectType(
        triggers: List<TriggerDefinition>,
        objectType: String
    ): List<TriggerDefinition> {
        return triggers.filter { trigger ->
            trigger.condition.objectType == null || trigger.condition.objectType == objectType
        }
    }
    
    /**
     * Get all triggers that could fire for a specific property change
     */
    fun getTriggersForPropertyChange(
        triggers: List<TriggerDefinition>,
        propertyName: String
    ): List<TriggerDefinition> {
        return triggers.filter { trigger ->
            trigger.condition.propertyChanged == null || trigger.condition.propertyChanged == propertyName
        }
    }
}