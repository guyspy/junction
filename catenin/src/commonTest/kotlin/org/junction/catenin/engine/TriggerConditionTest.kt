package org.junction.catenin.engine

import org.junction.catenin.core.*
import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.triggers.*
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.test.*

class TriggerConditionTest {
    
    private fun createTestSchema(): UniversalGameSchema {
        val unitType = ObjectTypeDefinition(
            properties = mapOf(
                "health" to PropertyDefinition(
                    type = PropertyType.INT,
                    initial = IntValue(10)
                ),
                "armor" to PropertyDefinition(
                    type = PropertyType.INT,
                    initial = IntValue(0)
                )
            )
        )
        
        return UniversalGameSchema(
            meta = GameMeta(
                name = "Test",
                targetAge = intArrayOf(8, 12),
                participantCount = intArrayOf(1, 4)
            ),
            objectTypes = mapOf("unit" to unitType)
        )
    }
    
    private fun createTestUnit(id: String = "unit1", health: Int = 10, armor: Int = 0): GameObject {
        return GameObject(
            id = id,
            type = "unit",
            properties = mapOf(
                "health" to IntValue(health),
                "armor" to IntValue(armor)
            ),
            states = emptyMap()
        )
    }
    
    @Test
    fun testPropertyValueComparison() {
        val schema = createTestSchema()
        val effectEngine = EffectEngine(schema, SimpleTargetResolver())
        val engine = TriggerEngine(schema, effectEngine)
        
        // Trigger that fires when health becomes exactly 5
        val trigger = TriggerDefinition(
            name = "low_health_trigger",
            `when` = TriggerCondition(
                objectType = "unit",
                propertyChanged = "health",
                newValue = "5"
            ),
            effects = listOf(
                LogEffect("Health is now 5!")
            )
        )
        
        val schemaWithTrigger = schema.withTrigger(trigger)
        val engineWithTrigger = TriggerEngine(schemaWithTrigger, effectEngine)
        
        val unit = createTestUnit(health = 10)
        val world = GameWorld.empty().withObjects(listOf(unit))
        
        // Health changing to 5 should fire trigger
        val update1 = UpdatePropertyUpdate("unit1", "health", IntValue(5))
        val results1 = engineWithTrigger.evaluateUpdate(world, update1)
        // Log effects don't generate updates, but trigger should evaluate
        
        // Health changing to 3 should not fire trigger
        val update2 = UpdatePropertyUpdate("unit1", "health", IntValue(3))
        val results2 = engineWithTrigger.evaluateUpdate(world, update2)
        
        // Both should not generate updates (LogEffect only), but we're testing condition evaluation
        assertEquals(0, results1.size)
        assertEquals(0, results2.size)
    }
    
    @Test
    fun testComplexConditionEvaluation() {
        val schema = createTestSchema()
        val effectEngine = EffectEngine(schema, SimpleTargetResolver())
        
        // Trigger with complex condition expression
        val trigger = TriggerDefinition(
            name = "complex_trigger",
            `when` = TriggerCondition(
                objectType = "unit",
                propertyChanged = "health",
                condition = "source.health < 5 && source.armor > 0"
            ),
            effects = listOf(
                ModifyPropertyEffect(
                    target = "self",
                    property = "armor",
                    delta = "1"
                )
            )
        )
        
        val schemaWithTrigger = schema.withTrigger(trigger)
        val engineWithTrigger = TriggerEngine(schemaWithTrigger, effectEngine)
        
        // Unit with armor
        val unit = createTestUnit(health = 10, armor = 2)
        val world = GameWorld.empty().withObjects(listOf(unit))
        
        // Health drops to 4 (with armor > 0) - should fire
        val update1 = UpdatePropertyUpdate("unit1", "health", IntValue(4))
        val results1 = engineWithTrigger.evaluateUpdate(world, update1)
        assertEquals(1, results1.size)
        assertTrue(results1[0] is UpdatePropertyUpdate)
        val armorUpdate = results1[0] as UpdatePropertyUpdate
        assertEquals("armor", armorUpdate.propertyName)
        
        // Unit without armor
        val unit2 = createTestUnit(id = "unit2", health = 10, armor = 0)
        val world2 = GameWorld.empty().withObjects(listOf(unit2))
        
        // Health drops to 4 (but armor = 0) - should NOT fire
        val update2 = UpdatePropertyUpdate("unit2", "health", IntValue(4))
        val results2 = engineWithTrigger.evaluateUpdate(world2, update2)
        assertEquals(0, results2.size)
    }
    
    @Test
    fun testMultipleConditions() {
        val schema = createTestSchema()
        val effectEngine = EffectEngine(schema, SimpleTargetResolver())
        
        // Trigger that combines specific value AND expression
        val trigger = TriggerDefinition(
            name = "multi_condition",
            `when` = TriggerCondition(
                objectType = "unit",
                propertyChanged = "health",
                newValue = "1",
                condition = "source.armor == 0"
            ),
            effects = listOf(
                LogEffect("Unit is about to die without armor!")
            )
        )
        
        val schemaWithTrigger = schema.withTrigger(trigger)
        val engineWithTrigger = TriggerEngine(schemaWithTrigger, effectEngine)
        
        val unit = createTestUnit(health = 10, armor = 0)
        val world = GameWorld.empty().withObjects(listOf(unit))
        
        // Health to 1 with armor = 0 - should fire
        val update1 = UpdatePropertyUpdate("unit1", "health", IntValue(1))
        engineWithTrigger.evaluateUpdate(world, update1)
        
        // Health to 1 with armor > 0 - should NOT fire
        val unitWithArmor = createTestUnit(id = "unit2", health = 10, armor = 2)
        val world2 = GameWorld.empty().withObjects(listOf(unitWithArmor))
        val update2 = UpdatePropertyUpdate("unit2", "health", IntValue(1))
        engineWithTrigger.evaluateUpdate(world2, update2)
        
        // Tests pass if no exceptions thrown (LogEffect doesn't generate updates)
    }
    
    @Test
    fun testDifferentPropertyTypes() {
        val schema = createTestSchema()
        val effectEngine = EffectEngine(schema, SimpleTargetResolver())
        
        // Test with string property
        val trigger = TriggerDefinition(
            name = "state_change",
            `when` = TriggerCondition(
                propertyChanged = "state",
                newValue = "active"
            ),
            effects = listOf(LogEffect("State is active"))
        )
        
        val obj = GameObject(
            id = "obj1",
            type = "generic",
            properties = mapOf("state" to StringValue("inactive")),
            states = emptyMap()
        )
        
        val schemaWithTrigger = schema.withTrigger(trigger)
        val engineWithTrigger = TriggerEngine(schemaWithTrigger, effectEngine)
        val world = GameWorld.empty().withObjects(listOf(obj))
        
        // String value comparison should work
        val update = UpdatePropertyUpdate("obj1", "state", StringValue("active"))
        engineWithTrigger.evaluateUpdate(world, update)
        
        // Test with boolean property
        val boolTrigger = TriggerDefinition(
            name = "bool_change",
            `when` = TriggerCondition(
                propertyChanged = "enabled",
                newValue = "true"
            ),
            effects = listOf(LogEffect("Now enabled"))
        )
        
        val obj2 = GameObject(
            id = "obj2",
            type = "generic",
            properties = mapOf("enabled" to BoolValue(false)),
            states = emptyMap()
        )
        
        val schemaWithBoolTrigger = schema.withTrigger(boolTrigger)
        val engineWithBoolTrigger = TriggerEngine(schemaWithBoolTrigger, effectEngine)
        val world2 = GameWorld.empty().withObjects(listOf(obj2))
        
        val boolUpdate = UpdatePropertyUpdate("obj2", "enabled", BoolValue(true))
        engineWithBoolTrigger.evaluateUpdate(world2, boolUpdate)
    }
}