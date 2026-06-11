package org.junction.catenin.engine

import org.junction.catenin.core.GameWorld
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.*
import kotlin.test.*

class ConditionEvaluatorTest {
    
    private val evaluator = ConditionEvaluator()
    
    private fun createTestObject(
        id: String = "test",
        type: String = "unit",
        properties: Map<String, PropertyValue> = emptyMap()
    ): GameObject {
        return GameObject(
            id = id,
            type = type,
            properties = properties,
            states = emptyMap()
        )
    }
    
    private fun createTestContext(
        sourceObject: GameObject = createTestObject(),
        targetObject: GameObject? = null,
        world: GameWorld = GameWorld.empty()
    ): ConditionContext {
        return ConditionContext(
            sourceObject = sourceObject,
            targetObject = targetObject,
            world = world
        )
    }
    
    @Test
    fun testEmptyConditionReturnsTrue() {
        val context = createTestContext()
        assertTrue(evaluator.evaluate("", context))
        assertTrue(evaluator.evaluate("  ", context))
    }
    
    @Test
    fun testPropertyComparison() {
        val source = createTestObject(
            properties = mapOf(
                "health" to IntValue(5),
                "armor" to IntValue(2)
            )
        )
        val context = createTestContext(sourceObject = source)
        
        // Numeric comparisons
        assertTrue(evaluator.evaluate("source.health == 5", context))
        assertTrue(evaluator.evaluate("source.health > 3", context))
        assertTrue(evaluator.evaluate("source.health < 10", context))
        assertTrue(evaluator.evaluate("source.health >= 5", context))
        assertTrue(evaluator.evaluate("source.health <= 5", context))
        assertFalse(evaluator.evaluate("source.health == 10", context))
        assertFalse(evaluator.evaluate("source.health < 3", context))
    }
    
    @Test
    fun testStringComparison() {
        val source = createTestObject(
            type = "spell",
            properties = mapOf(
                "element" to StringValue("fire")
            )
        )
        val context = createTestContext(sourceObject = source)
        
        assertTrue(evaluator.evaluate("source.type == 'spell'", context))
        assertTrue(evaluator.evaluate("source.element == 'fire'", context))
        assertFalse(evaluator.evaluate("source.element == 'water'", context))
        assertTrue(evaluator.evaluate("source.element != 'water'", context))
    }
    
    @Test
    fun testBooleanProperties() {
        val source = createTestObject(
            properties = mapOf(
                "active" to BoolValue(true),
                "tapped" to BoolValue(false)
            )
        )
        val context = createTestContext(sourceObject = source)
        
        assertTrue(evaluator.evaluate("source.active", context))
        assertTrue(evaluator.evaluate("source.active == true", context))
        assertFalse(evaluator.evaluate("source.tapped", context))
        assertTrue(evaluator.evaluate("source.tapped == false", context))
    }
    
    @Test
    fun testLogicalOperators() {
        val source = createTestObject(
            properties = mapOf(
                "health" to IntValue(5),
                "armor" to IntValue(2)
            )
        )
        val context = createTestContext(sourceObject = source)
        
        // AND
        assertTrue(evaluator.evaluate("source.health > 3 && source.armor > 1", context))
        assertFalse(evaluator.evaluate("source.health > 3 && source.armor > 5", context))
        
        // OR
        assertTrue(evaluator.evaluate("source.health > 10 || source.armor > 1", context))
        assertFalse(evaluator.evaluate("source.health > 10 || source.armor > 5", context))
        
        // NOT
        assertTrue(evaluator.evaluate("!source.health > 10", context))
        assertFalse(evaluator.evaluate("!source.health > 3", context))
    }
    
    @Test
    fun testTargetObjectAccess() {
        val source = createTestObject(
            id = "attacker",
            properties = mapOf("attack" to IntValue(3))
        )
        val target = createTestObject(
            id = "defender",
            properties = mapOf("defense" to IntValue(2))
        )
        val context = createTestContext(
            sourceObject = source,
            targetObject = target
        )
        
        assertTrue(evaluator.evaluate("source.attack > target.defense", context))
        assertTrue(evaluator.evaluate("target.id == 'defender'", context))
    }
    
    @Test
    fun testWorldAccess() {
        val obj1 = createTestObject(id = "unit1")
        val obj2 = createTestObject(id = "unit2")
        val world = GameWorld.empty()
            .withObjects(listOf(obj1, obj2))
        
        val context = createTestContext(
            sourceObject = obj1,
            world = world
        )
        
        assertTrue(evaluator.evaluate("world.hasObject('unit1')", context))
        assertTrue(evaluator.evaluate("world.hasObject('unit2')", context))
        assertFalse(evaluator.evaluate("world.hasObject('unit3')", context))
    }
    
    @Test
    fun testComplexExpressions() {
        val source = createTestObject(
            type = "unit",
            properties = mapOf(
                "health" to IntValue(5),
                "armor" to IntValue(2),
                "poisoned" to BoolValue(true)
            )
        )
        val context = createTestContext(sourceObject = source)
        
        // Complex conditions
        assertTrue(evaluator.evaluate(
            "source.type == 'unit' && source.health < 10 && source.poisoned",
            context
        ))
        
        assertTrue(evaluator.evaluate(
            "(source.health < 3 || source.armor < 3) && source.poisoned",
            context
        ))
    }
    
    @Test
    fun testMissingProperties() {
        val source = createTestObject()
        val context = createTestContext(sourceObject = source)
        
        // Missing properties should evaluate to false/null
        assertFalse(evaluator.evaluate("source.missing == 5", context))
        assertFalse(evaluator.evaluate("source.missing", context))
        assertTrue(evaluator.evaluate("source.missing != 5", context))
    }
    
    @Test
    fun testInvalidExpressions() {
        val context = createTestContext()
        
        // Invalid expressions should return false
        assertFalse(evaluator.evaluate("invalid expression", context))
        assertFalse(evaluator.evaluate("source.health >", context))
        assertFalse(evaluator.evaluate("== 5", context))
    }
    
    @Test
    fun testObjectReference() {
        val source = createTestObject(
            properties = mapOf(
                "owner" to ObjectRefValue("player1")
            )
        )
        val context = createTestContext(sourceObject = source)
        
        assertTrue(evaluator.evaluate("source.owner == 'player1'", context))
        assertFalse(evaluator.evaluate("source.owner == 'player2'", context))
    }
}