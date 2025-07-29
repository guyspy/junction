package org.junction.catenin.engine

import org.junction.catenin.core.*
import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.triggers.*
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.test.*

class TriggerEngineTest {
    
    private fun createTestSchema(): UniversalGameSchema {
        val meta = GameMeta(
            name = "Test Game",
            targetAge = intArrayOf(8, 12),
            participantCount = intArrayOf(2, 2)
        )
        
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
        
        // Trigger: When health drops to 5 or below, gain 2 armor
        val lowHealthTrigger = TriggerDefinition(
            name = "low_health_armor",
            `when` = TriggerCondition(
                objectType = "unit",
                propertyChanged = "health",
                condition = "value <= 5"
            ),
            effects = listOf(
                ModifyPropertyEffect(
                    target = "self",
                    property = "armor",
                    delta = "2"
                )
            )
        )
        
        // Trigger: Log when any property changes
        val logTrigger = TriggerDefinition(
            name = "property_logger",
            `when` = TriggerCondition(
                objectType = "unit"
            ),
            effects = listOf(
                LogEffect("Property changed: {property} = {value}")
            )
        )
        
        return UniversalGameSchema(
            meta = meta,
            objectTypes = mapOf("unit" to unitType),
            triggers = listOf(lowHealthTrigger, logTrigger)
        )
    }
    
    @Test
    fun testTriggerEvaluationForPropertyChange() {
        val schema = createTestSchema()
        val targetResolver = SimpleTargetResolver()
        val effectEngine = EffectEngine(schema, targetResolver)
        val engine = TriggerEngine(schema, effectEngine)
        
        // Create world with a unit
        val unit = GameObject(
            id = "unit1",
            type = "unit",
            properties = mapOf(
                "health" to IntValue(10),
                "armor" to IntValue(0)
            )
        )
        val world = GameWorld.empty().applyUpdate(AddObjectUpdate(unit))
        
        // Update health to 5 (should trigger)
        val update = UpdatePropertyUpdate("unit1", "health", IntValue(5))
        val triggeredUpdates = engine.evaluateUpdate(world, update)
        
        // Should generate an update to add 2 armor
        assertEquals(1, triggeredUpdates.size)
        val armorUpdate = triggeredUpdates[0] as UpdatePropertyUpdate
        assertEquals("unit1", armorUpdate.objectId)
        assertEquals("armor", armorUpdate.propertyName)
        assertEquals(IntValue(2), armorUpdate.value)
    }
    
    @Test
    fun testTriggerConditionMatching() {
        val schema = createTestSchema()
        val targetResolver = SimpleTargetResolver()
        val effectEngine = EffectEngine(schema, targetResolver)
        val engine = TriggerEngine(schema, effectEngine)
        
        // Create world with a unit
        val unit = GameObject(
            id = "unit1",
            type = "unit",
            properties = mapOf("health" to IntValue(10))
        )
        val world = GameWorld.empty().applyUpdate(AddObjectUpdate(unit))
        
        // Test that triggers are found for the right object type and property
        val triggers = schema.getTriggersForPropertyChange("unit", "health")
        assertEquals(2, triggers.size) // Both triggers should match
        
        // Test with wrong object type
        val wrongTypeTriggers = schema.getTriggersForPropertyChange("building", "health")
        assertEquals(0, wrongTypeTriggers.size)
    }
    
    @Test
    fun testNoTriggersForOtherUpdateTypes() {
        val schema = createTestSchema()
        val targetResolver = SimpleTargetResolver()
        val effectEngine = EffectEngine(schema, targetResolver)
        val engine = TriggerEngine(schema, effectEngine)
        
        val world = GameWorld.empty()
        
        // Add object update should not trigger anything
        val unit = GameObject("unit1", "unit")
        val addUpdate = AddObjectUpdate(unit)
        val triggeredUpdates = engine.evaluateUpdate(world, addUpdate)
        
        assertEquals(0, triggeredUpdates.size)
    }
    
    @Test
    fun testTriggerWithSpecificPropertyCondition() {
        val meta = GameMeta(
            name = "Test Game",
            targetAge = intArrayOf(8, 12),
            participantCount = intArrayOf(2, 2)
        )
        
        val switchType = ObjectTypeDefinition(
            properties = mapOf(
                "state" to PropertyDefinition(
                    type = PropertyType.STRING,
                    initial = StringValue("off")
                )
            )
        )
        
        // Trigger only when state changes to "on"
        val switchTrigger = TriggerDefinition(
            name = "switch_activated",
            `when` = TriggerCondition(
                objectType = "switch",
                propertyChanged = "state",
                newValue = "on"
            ),
            effects = listOf(
                LogEffect("Switch activated!")
            )
        )
        
        val schema = UniversalGameSchema(
            meta = meta,
            objectTypes = mapOf("switch" to switchType),
            triggers = listOf(switchTrigger)
        )
        
        val targetResolver = SimpleTargetResolver()
        val effectEngine = EffectEngine(schema, targetResolver)
        val engine = TriggerEngine(schema, effectEngine)
        
        // Create world with a switch
        val switch = GameObject(
            id = "switch1",
            type = "switch",
            properties = mapOf("state" to StringValue("off"))
        )
        val world = GameWorld.empty().applyUpdate(AddObjectUpdate(switch))
        
        // Changing to "on" should match the trigger
        val updateOn = UpdatePropertyUpdate("switch1", "state", StringValue("on"))
        val triggersOn = schema.getTriggersForPropertyChange("switch", "state")
        assertEquals(1, triggersOn.size)
        
        // The trigger condition should check for newValue = "on"
        assertEquals("on", triggersOn.first().`when`.newValue)
    }
}