package org.junction.catenin.universal

import org.junction.catenin.model.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TriggerMatchingTest {

    @Test
    fun testBasicObjectTypeTriggerMatching() {
        // Test: Trigger should match when object type matches the condition
        // This verifies that triggers can be filtered by object type (e.g., only fire for "card" objects)
        val trigger = TriggerDefinition(
            name = "card_trigger",
            condition = TriggerCondition(objectType = "card"),
            effects = emptyList()
        )
        
        val cardObject = GameObject(
            id = "fire_bolt",
            type = "card",
            properties = mapOf("name" to PropertyValue.StringValue("Fire Bolt"))
        )
        
        val playerObject = GameObject(
            id = "player_1", 
            type = "player",
            properties = mapOf("health" to PropertyValue.IntValue(20))
        )
        
        // Should match card objects
        assertTrue(triggerMatches(trigger, cardObject, "name", null, PropertyValue.StringValue("Fire Bolt")))
        
        // Should not match player objects
        assertFalse(triggerMatches(trigger, playerObject, "health", PropertyValue.IntValue(20), PropertyValue.IntValue(15)))
    }

    @Test
    fun testPropertyChangeTriggerMatching() {
        // Test: Trigger should only fire when a specific property changes
        // This ensures triggers can be limited to specific property changes (e.g., only when "health" changes)
        val trigger = TriggerDefinition(
            name = "health_change_trigger",
            condition = TriggerCondition(
                objectType = "player",
                propertyChanged = "health"
            ),
            effects = emptyList()
        )
        
        val player = GameObject(
            id = "player_1",
            type = "player", 
            properties = mapOf("health" to PropertyValue.IntValue(15))
        )
        
        // Should match when health property changes
        assertTrue(triggerMatches(trigger, player, "health", PropertyValue.IntValue(20), PropertyValue.IntValue(15)))
        
        // Should not match when other properties change
        assertFalse(triggerMatches(trigger, player, "mana", PropertyValue.IntValue(5), PropertyValue.IntValue(3)))
    }

    @Test
    fun testSpecificValueTriggerMatching() {
        // Test: Trigger should fire when property changes to a specific value
        // This enables triggers like "when health reaches 0" or "when tapped becomes true"
        val trigger = TriggerDefinition(
            name = "death_trigger",
            condition = TriggerCondition(
                objectType = "player",
                propertyChanged = "health",
                newValue = PropertyValue.IntValue(0)
            ),
            effects = emptyList()
        )
        
        val player = GameObject(
            id = "player_1",
            type = "player",
            properties = mapOf("health" to PropertyValue.IntValue(0))
        )
        
        // Should match when health changes to 0
        assertTrue(triggerMatches(trigger, player, "health", PropertyValue.IntValue(5), PropertyValue.IntValue(0)))
        
        // Should not match when health changes to other values
        assertFalse(triggerMatches(trigger, player, "health", PropertyValue.IntValue(10), PropertyValue.IntValue(5)))
    }

    @Test
    fun testObjectMatcherTriggerMatching() {
        // Test: Trigger should fire when property changes to match an object pattern
        // This enables triggers like "when card moves to battlefield" where battlefield is matched by name
        val trigger = TriggerDefinition(
            name = "enters_battlefield",
            condition = TriggerCondition(
                objectType = "card",
                propertyChanged = "parent",
                newValueMatches = ObjectMatcher(name = "battlefield")
            ),
            effects = emptyList()
        )
        
        val card = GameObject(
            id = "fire_bolt",
            type = "card",
            properties = mapOf("name" to PropertyValue.StringValue("Fire Bolt")),
            parentId = "battlefield_zone"
        )
        
        val battlefieldZone = GameObject(
            id = "battlefield_zone",
            type = "container",
            properties = mapOf("name" to PropertyValue.StringValue("battlefield"))
        )
        
        // Should match when card moves to an object named "battlefield"
        // Note: In real implementation, this would need access to the game world to resolve parent
        assertTrue(triggerMatches(trigger, card, "parent", null, PropertyValue.ObjectRefValue("battlefield_zone")))
    }

    @Test
    fun testConditionExpressionMatching() {
        // Test: Trigger should evaluate custom condition expressions
        // This enables complex triggers like "when Lightning Bolt enters battlefield"
        val trigger = TriggerDefinition(
            name = "lightning_bolt_effect",
            condition = TriggerCondition(
                objectType = "card",
                propertyChanged = "parent",
                condition = "this.properties.name == 'Lightning Bolt'"
            ),
            effects = emptyList()
        )
        
        val lightningBolt = GameObject(
            id = "lightning_1",
            type = "card",
            properties = mapOf("name" to PropertyValue.StringValue("Lightning Bolt"))
        )
        
        val healingSpell = GameObject(
            id = "healing_1", 
            type = "card",
            properties = mapOf("name" to PropertyValue.StringValue("Healing Spell"))
        )
        
        // Should match Lightning Bolt cards
        assertTrue(triggerMatches(trigger, lightningBolt, "parent", null, PropertyValue.ObjectRefValue("battlefield")))
        
        // Should not match other cards
        assertFalse(triggerMatches(trigger, healingSpell, "parent", null, PropertyValue.ObjectRefValue("battlefield")))
    }

    @Test
    fun testComplexTriggerMatching() {
        // Test: Trigger should match when ALL conditions are met
        // This verifies that triggers can have multiple conditions that must all be satisfied
        val trigger = TriggerDefinition(
            name = "complex_trigger",
            condition = TriggerCondition(
                objectType = "card",
                propertyChanged = "parent",
                newValueMatches = ObjectMatcher(name = "battlefield"),
                condition = "this.properties.cost <= 3"
            ),
            effects = emptyList()
        )
        
        val cheapCard = GameObject(
            id = "cheap_card",
            type = "card",
            properties = mapOf(
                "name" to PropertyValue.StringValue("Cheap Spell"),
                "cost" to PropertyValue.IntValue(2)
            )
        )
        
        val expensiveCard = GameObject(
            id = "expensive_card",
            type = "card", 
            properties = mapOf(
                "name" to PropertyValue.StringValue("Expensive Spell"),
                "cost" to PropertyValue.IntValue(5)
            )
        )
        
        // Should match cheap cards entering battlefield
        assertTrue(triggerMatches(trigger, cheapCard, "parent", null, PropertyValue.ObjectRefValue("battlefield")))
        
        // Should not match expensive cards entering battlefield
        assertFalse(triggerMatches(trigger, expensiveCard, "parent", null, PropertyValue.ObjectRefValue("battlefield")))
    }

    @Test
    fun testStateTriggerMatching() {
        // Test: Trigger should fire when object states change, not just properties
        // This verifies that triggers work for both properties and states (like tapped/untapped)
        val trigger = TriggerDefinition(
            name = "tap_trigger",
            condition = TriggerCondition(
                objectType = "card",
                propertyChanged = "states.tapped",
                newValue = PropertyValue.BoolValue(true)
            ),
            effects = emptyList()
        )
        
        val card = GameObject(
            id = "creature_1",
            type = "card",
            states = mapOf("tapped" to PropertyValue.BoolValue(true))
        )
        
        // Should match when card becomes tapped
        assertTrue(triggerMatches(trigger, card, "states.tapped", PropertyValue.BoolValue(false), PropertyValue.BoolValue(true)))
        
        // Should not match when card becomes untapped
        assertFalse(triggerMatches(trigger, card, "states.tapped", PropertyValue.BoolValue(true), PropertyValue.BoolValue(false)))
    }

    // Placeholder method - will be implemented in actual TriggerEngine class
    private fun triggerMatches(
        trigger: TriggerDefinition,
        obj: GameObject,
        changedProperty: String,
        oldValue: PropertyValue?,
        newValue: PropertyValue
    ): Boolean {
        TODO("TriggerEngine.triggerMatches not implemented yet")
    }
}