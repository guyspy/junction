package org.junction.catenin.engine

import org.junction.catenin.core.GameWorld
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.*
import kotlin.js.JsExport

/**
 * Evaluates condition expressions for triggers
 * 
 * Supports expressions like:
 * - "source.health < 5"
 * - "target.type == 'unit'"
 * - "source.health > 0 && source.armor < 3"
 * - "world.hasObject('game_state')"
 */
@JsExport
class ConditionEvaluator {
    
    /**
     * Evaluate a condition expression
     */
    fun evaluate(
        expression: String,
        context: ConditionContext
    ): Boolean {
        if (expression.isBlank()) return true
        
        return try {
            evaluateExpression(expression.trim(), context)
        } catch (e: Exception) {
            // If evaluation fails, condition is false
            false
        }
    }
    
    /**
     * Parse and evaluate an expression
     */
    private fun evaluateExpression(expr: String, context: ConditionContext): Boolean {
        // Handle logical operators first (left-to-right evaluation)
        return when {
            expr.contains(" && ") -> {
                val parts = expr.split(" && ", limit = 2)
                evaluateExpression(parts[0].trim(), context) && 
                evaluateExpression(parts[1].trim(), context)
            }
            expr.contains(" || ") -> {
                val parts = expr.split(" || ", limit = 2)
                evaluateExpression(parts[0].trim(), context) || 
                evaluateExpression(parts[1].trim(), context)
            }
            expr.startsWith("!") -> {
                !evaluateExpression(expr.substring(1).trim(), context)
            }
            else -> evaluateComparison(expr, context)
        }
    }
    
    /**
     * Evaluate a comparison expression
     */
    private fun evaluateComparison(expr: String, context: ConditionContext): Boolean {
        // Check for comparison operators
        val operators = listOf(" == ", " != ", " < ", " > ", " <= ", " >= ")
        
        for (op in operators) {
            if (expr.contains(op)) {
                val parts = expr.split(op, limit = 2)
                if (parts.size == 2) {
                    val leftValue = resolveValue(parts[0].trim(), context)
                    val rightValue = resolveValue(parts[1].trim(), context)
                    
                    return when (op) {
                        " == " -> compareValues(leftValue, rightValue) == 0
                        " != " -> compareValues(leftValue, rightValue) != 0
                        " < " -> compareValues(leftValue, rightValue) < 0
                        " > " -> compareValues(leftValue, rightValue) > 0
                        " <= " -> compareValues(leftValue, rightValue) <= 0
                        " >= " -> compareValues(leftValue, rightValue) >= 0
                        else -> false
                    }
                }
            }
        }
        
        // If no operator, evaluate as boolean
        return resolveBooleanValue(expr, context)
    }
    
    /**
     * Resolve a value from an expression part
     */
    private fun resolveValue(expr: String, context: ConditionContext): Any? {
        return when {
            // String literal
            expr.startsWith("'") && expr.endsWith("'") -> {
                expr.substring(1, expr.length - 1)
            }
            
            // Number literal
            expr.toIntOrNull() != null -> {
                expr.toInt()
            }
            
            // Boolean literal
            expr == "true" -> true
            expr == "false" -> false
            
            // Property access
            expr.contains(".") -> {
                resolvePropertyAccess(expr, context)
            }
            
            // Simple identifier (might be object reference)
            else -> {
                resolveIdentifier(expr, context)
            }
        }
    }
    
    /**
     * Resolve property access like "source.health" or "world.getObject('id')"
     */
    private fun resolvePropertyAccess(expr: String, context: ConditionContext): Any? {
        val parts = expr.split(".", limit = 2)
        val objectName = parts[0]
        val propertyPath = parts[1]
        
        val obj = when (objectName) {
            "source" -> context.sourceObject
            "target" -> context.targetObject
            "world" -> return resolveWorldAccess(propertyPath, context)
            else -> context.world.getObject(objectName)
        }
        
        return obj?.let { resolveObjectProperty(it, propertyPath) }
    }
    
    /**
     * Resolve world method calls
     */
    private fun resolveWorldAccess(expr: String, context: ConditionContext): Any? {
        return when {
            expr.startsWith("hasObject(") && expr.endsWith(")") -> {
                val id = extractStringParameter(expr.substring(10, expr.length - 1))
                context.world.hasObject(id)
            }
            expr.startsWith("getObject(") && expr.endsWith(")") -> {
                val id = extractStringParameter(expr.substring(10, expr.length - 1))
                context.world.getObject(id)
            }
            else -> null
        }
    }
    
    /**
     * Extract string parameter from quotes
     */
    private fun extractStringParameter(param: String): String {
        val trimmed = param.trim()
        return if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            trimmed
        }
    }
    
    /**
     * Resolve object property or method
     */
    private fun resolveObjectProperty(obj: GameObject, propertyPath: String): Any? {
        return when {
            // Direct property access
            obj.hasProperty(propertyPath) -> {
                propertyValueToComparable(obj.getProperty(propertyPath))
            }
            // State access
            obj.hasState(propertyPath) -> {
                propertyValueToComparable(obj.getState(propertyPath))
            }
            // Type access
            propertyPath == "type" -> obj.type
            propertyPath == "id" -> obj.id
            else -> null
        }
    }
    
    /**
     * Convert PropertyValue to comparable value
     */
    private fun propertyValueToComparable(value: PropertyValue?): Any? {
        return when (value) {
            is IntValue -> value.value
            is StringValue -> value.value
            is BoolValue -> value.value
            is ObjectRefValue -> value.objectId
            null -> null
        }
    }
    
    /**
     * Resolve simple identifier
     */
    private fun resolveIdentifier(expr: String, context: ConditionContext): Any? {
        return when (expr) {
            "source" -> context.sourceObject
            "target" -> context.targetObject
            "world" -> context.world
            else -> null
        }
    }
    
    /**
     * Resolve boolean value
     */
    private fun resolveBooleanValue(expr: String, context: ConditionContext): Boolean {
        val value = resolveValue(expr, context)
        return when (value) {
            is Boolean -> value
            is Int -> value != 0
            is String -> value.isNotEmpty()
            is GameObject -> true
            null -> false
            else -> false
        }
    }
    
    /**
     * Compare two values
     */
    private fun compareValues(left: Any?, right: Any?): Int {
        return when {
            left == null && right == null -> 0
            left == null -> -1
            right == null -> 1
            left is Int && right is Int -> left.compareTo(right)
            left is String && right is String -> left.compareTo(right)
            left is Boolean && right is Boolean -> left.compareTo(right)
            left.toString() == right.toString() -> 0
            else -> left.toString().compareTo(right.toString())
        }
    }
}

/**
 * Context for condition evaluation
 */
@JsExport
data class ConditionContext(
    val sourceObject: GameObject,
    val targetObject: GameObject? = null,
    val world: GameWorld,
    val metadata: Map<String, Any> = emptyMap()
)