package org.junction.catenin.model

import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.*
import org.junction.catenin.model.triggers.*
import org.junction.catenin.model.values.*

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UniversalGameDefinitionTest {
    
    private fun createBasicGameMeta(): GameMeta {
        return GameMeta(
            name = "Test Game",
            targetAge = intArrayOf(8, 12),
            participantCount = intArrayOf(2, 4)
        )
    }
    
    private fun createBasicObjectTypeDefinition(): ObjectTypeDefinition {
        return ObjectTypeDefinition(
            properties = mapOf(
                "health" to PropertyDefinition(PropertyType.INT, IntValue(100)),
                "name" to PropertyDefinition(PropertyType.STRING, StringValue("Default"))
            ),
            states = mapOf(
                "activated" to PropertyDefinition(PropertyType.BOOL, BoolValue(false))
            )
        )
    }
    
    @Test
    fun testBasicGameDefinition() {
        val meta = createBasicGameMeta()
        val objectTypes = mapOf("creature" to createBasicObjectTypeDefinition())
        
        val definition = UniversalGameDefinition(
            meta = meta,
            objectTypes = objectTypes
        )
        
        assertEquals(meta, definition.meta)
        assertEquals(1, definition.objectTypes.size)
        assertEquals(0, definition.instances.size)
        assertEquals(0, definition.triggers.size)
        
        assertTrue(definition.hasObjectType("creature"))
        assertFalse(definition.hasObjectType("unknown"))
        
        assertNotNull(definition.getObjectType("creature"))
        assertNull(definition.getObjectType("unknown"))
        
        assertEquals(listOf("creature"), definition.getAllObjectTypeNames())
        assertEquals(emptyList(), definition.getAllInstanceNames())
    }
    
    @Test
    fun testWithObjectType() {
        val meta = createBasicGameMeta()
        val definition = UniversalGameDefinition(meta, emptyMap())
        
        val creatureType = createBasicObjectTypeDefinition()
        val newDefinition = definition.withObjectType("creature", creatureType)
        
        // Original should be unchanged
        assertEquals(0, definition.objectTypes.size)
        assertFalse(definition.hasObjectType("creature"))
        
        // New definition should have the object type
        assertEquals(1, newDefinition.objectTypes.size)
        assertTrue(newDefinition.hasObjectType("creature"))
        assertEquals(creatureType, newDefinition.getObjectType("creature"))
    }
    
    @Test
    fun testWithInstance() {
        val meta = createBasicGameMeta()
        val objectTypes = mapOf("creature" to createBasicObjectTypeDefinition())
        val definition = UniversalGameDefinition(meta, objectTypes)
        
        val instance = ObjectInstance(
            objectType = "creature",
            properties = mapOf("name" to "Goblin"),
            states = mapOf("activated" to "true")
        )
        
        val newDefinition = definition.withInstance("goblin_warrior", instance)
        
        // Original should be unchanged
        assertEquals(0, definition.instances.size)
        assertFalse(definition.hasInstance("goblin_warrior"))
        
        // New definition should have the instance
        assertEquals(1, newDefinition.instances.size)
        assertTrue(newDefinition.hasInstance("goblin_warrior"))
        assertEquals(instance, newDefinition.getInstance("goblin_warrior"))
    }
    
    @Test
    fun testWithTrigger() {
        val meta = createBasicGameMeta()
        val objectTypes = mapOf("creature" to createBasicObjectTypeDefinition())
        val definition = UniversalGameDefinition(meta, objectTypes)
        
        val trigger = TriggerDefinition(
            name = "health_changed",
            `when` = TriggerCondition(
                objectType = "creature",
                propertyChanged = "health"
            ),
            effects = listOf(LogEffect("Health changed!"))
        )
        
        val newDefinition = definition.withTrigger(trigger)
        
        // Original should be unchanged
        assertEquals(0, definition.triggers.size)
        
        // New definition should have the trigger
        assertEquals(1, newDefinition.triggers.size)
        assertEquals(trigger, newDefinition.triggers[0])
    }
    
    @Test
    fun testWithoutObjectType() {
        val meta = createBasicGameMeta()
        val objectTypes = mapOf(
            "creature" to createBasicObjectTypeDefinition(),
            "spell" to ObjectTypeDefinition()
        )
        val definition = UniversalGameDefinition(meta, objectTypes)
        
        val newDefinition = definition.withoutObjectType("spell")
        
        // Original should be unchanged
        assertEquals(2, definition.objectTypes.size)
        assertTrue(definition.hasObjectType("spell"))
        
        // New definition should not have the object type
        assertEquals(1, newDefinition.objectTypes.size)
        assertFalse(newDefinition.hasObjectType("spell"))
        assertTrue(newDefinition.hasObjectType("creature"))
    }
    
    @Test
    fun testWithoutInstance() {
        val meta = createBasicGameMeta()
        val objectTypes = mapOf("creature" to createBasicObjectTypeDefinition())
        val instances = mapOf(
            "goblin" to ObjectInstance("creature"),
            "orc" to ObjectInstance("creature")
        )
        val definition = UniversalGameDefinition(meta, objectTypes, instances)
        
        val newDefinition = definition.withoutInstance("orc")
        
        // Original should be unchanged
        assertEquals(2, definition.instances.size)
        assertTrue(definition.hasInstance("orc"))
        
        // New definition should not have the instance
        assertEquals(1, newDefinition.instances.size)
        assertFalse(newDefinition.hasInstance("orc"))
        assertTrue(newDefinition.hasInstance("goblin"))
    }
    
    @Test
    fun testGetTriggersForObjectType() {
        val meta = createBasicGameMeta()
        val objectTypes = mapOf(
            "creature" to createBasicObjectTypeDefinition(),
            "spell" to ObjectTypeDefinition()
        )
        
        val triggers = listOf(
            TriggerDefinition(
                name = "creature_health_changed",
                `when` = TriggerCondition(objectType = "creature", propertyChanged = "health"),
                effects = listOf(LogEffect("Creature health changed"))
            ),
            TriggerDefinition(
                name = "spell_cast",
                `when` = TriggerCondition(objectType = "spell", propertyChanged = "cast"),
                effects = listOf(LogEffect("Spell cast"))
            ),
            TriggerDefinition(
                name = "global_trigger",
                `when` = TriggerCondition(propertyChanged = "any_property"),
                effects = listOf(LogEffect("Global trigger"))
            )
        )
        
        val definition = UniversalGameDefinition(meta, objectTypes, emptyMap(), triggers)
        
        val creatureTriggers = definition.getTriggersForObjectType("creature")
        assertEquals(2, creatureTriggers.size)
        assertTrue(creatureTriggers.any { it.name == "creature_health_changed" })
        assertTrue(creatureTriggers.any { it.name == "global_trigger" })
        
        val spellTriggers = definition.getTriggersForObjectType("spell")
        assertEquals(2, spellTriggers.size)
        assertTrue(spellTriggers.any { it.name == "spell_cast" })
        assertTrue(spellTriggers.any { it.name == "global_trigger" })
        
        val unknownTriggers = definition.getTriggersForObjectType("unknown")
        assertEquals(1, unknownTriggers.size)
        assertTrue(unknownTriggers.any { it.name == "global_trigger" })
    }
    
    @Test
    fun testGetTriggersForPropertyChange() {
        val meta = createBasicGameMeta()
        val objectTypes = mapOf("creature" to createBasicObjectTypeDefinition())
        
        val triggers = listOf(
            TriggerDefinition(
                name = "health_trigger",
                `when` = TriggerCondition(objectType = "creature", propertyChanged = "health"),
                effects = listOf(LogEffect("Health changed"))
            ),
            TriggerDefinition(
                name = "name_trigger",
                `when` = TriggerCondition(objectType = "creature", propertyChanged = "name"),
                effects = listOf(LogEffect("Name changed"))
            ),
            TriggerDefinition(
                name = "any_creature_property",
                `when` = TriggerCondition(objectType = "creature"),
                effects = listOf(LogEffect("Any creature property changed"))
            ),
            TriggerDefinition(
                name = "global_health_trigger",
                `when` = TriggerCondition(propertyChanged = "health"),
                effects = listOf(LogEffect("Any object health changed"))
            )
        )
        
        val definition = UniversalGameDefinition(meta, objectTypes, emptyMap(), triggers)
        
        val healthTriggers = definition.getTriggersForPropertyChange("creature", "health")
        assertEquals(3, healthTriggers.size)
        assertTrue(healthTriggers.any { it.name == "health_trigger" })
        assertTrue(healthTriggers.any { it.name == "any_creature_property" })
        assertTrue(healthTriggers.any { it.name == "global_health_trigger" })
        
        val nameTriggers = definition.getTriggersForPropertyChange("creature", "name")
        assertEquals(2, nameTriggers.size)
        assertTrue(nameTriggers.any { it.name == "name_trigger" })
        assertTrue(nameTriggers.any { it.name == "any_creature_property" })
        
        val unknownTriggers = definition.getTriggersForPropertyChange("unknown", "health")
        assertEquals(1, unknownTriggers.size)
        assertTrue(unknownTriggers.any { it.name == "global_health_trigger" })
    }
    
    @Test
    fun testGameMetaEquality() {
        val meta1 = GameMeta("Test", intArrayOf(8, 12), intArrayOf(2, 4))
        val meta2 = GameMeta("Test", intArrayOf(8, 12), intArrayOf(2, 4))
        val meta3 = GameMeta("Different", intArrayOf(8, 12), intArrayOf(2, 4))
        
        assertEquals(meta1, meta2)
        assertEquals(meta1.hashCode(), meta2.hashCode())
        assertFalse(meta1.equals(meta3))
    }
    
    @Test
    fun testObjectInstance() {
        val instance = ObjectInstance(
            objectType = "creature",
            properties = mapOf("name" to "Goblin", "health" to "50"),
            states = mapOf("activated" to "true")
        )
        
        assertEquals("creature", instance.objectType)
        assertEquals("Goblin", instance.properties["name"])
        assertEquals("50", instance.properties["health"])
        assertEquals("true", instance.states["activated"])
    }
    
    @Test
    fun testTriggerCondition() {
        val condition = TriggerCondition(
            objectType = "creature",
            propertyChanged = "health",
            newValue = "0",
            condition = "this.health <= 0"
        )
        
        assertEquals("creature", condition.objectType)
        assertEquals("health", condition.propertyChanged)
        assertEquals("0", condition.newValue)
        assertEquals("this.health <= 0", condition.condition)
    }
    
    @Test
    fun testEffectDefinitions() {
        val logEffect = LogEffect("Test message")
        assertEquals("Test message", logEffect.message)
        
        val modifyEffect = ModifyPropertyEffect(
            target = "this",
            property = "health",
            delta = "-10"
        )
        assertEquals("this", modifyEffect.target)
        assertEquals("health", modifyEffect.property)
        assertEquals("-10", modifyEffect.delta)
        assertNull(modifyEffect.value)
        
        val setValueEffect = ModifyPropertyEffect(
            target = "this",
            property = "health",
            value = "0"
        )
        assertEquals("0", setValueEffect.value)
        assertNull(setValueEffect.delta)
    }
}