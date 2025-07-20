package org.junction.catenin.core.factory

import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.*
import org.junction.catenin.model.values.*
import org.junction.catenin.core.factory.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ObjectFactoryTest {
    
    private fun createTestGameDefinition(): UniversalGameDefinition {
        val creatureType = ObjectTypeDefinition(
            properties = mapOf(
                "health" to PropertyDefinition(PropertyType.INT, IntValue(100), IntValue(0), IntValue(200)),
                "name" to PropertyDefinition(PropertyType.STRING, StringValue("Default Creature")),
                "participant_id" to PropertyDefinition(PropertyType.INT, IntValue(-1))
            ),
            states = mapOf(
                "activated" to PropertyDefinition(PropertyType.BOOL, BoolValue(false)),
                "position" to PropertyDefinition(PropertyType.STRING, StringValue("none"))
            )
        )
        
        val participantType = ObjectTypeDefinition(
            properties = mapOf(
                "participant_id" to PropertyDefinition(PropertyType.INT, IntValue(0)),
                "name" to PropertyDefinition(PropertyType.STRING, StringValue("Player"))
            )
        )
        
        val cardType = ObjectTypeDefinition(
            properties = mapOf(
                "cost" to PropertyDefinition(PropertyType.INT, IntValue(1)),
                "damage" to PropertyDefinition(PropertyType.INT, IntValue(1)),
                "name" to PropertyDefinition(PropertyType.STRING, StringValue("Basic Card"))
            )
        )
        
        val instances = mapOf(
            "fire_spell" to ObjectInstance(
                objectType = "card",
                properties = mapOf(
                    "name" to "Fire Spell",
                    "cost" to "3",
                    "damage" to "4"
                )
            ),
            "weak_goblin" to ObjectInstance(
                objectType = "creature",
                properties = mapOf(
                    "name" to "Weak Goblin",
                    "health" to "30"
                )
            )
        )
        
        return UniversalGameDefinition(
            meta = GameMeta("Test Game", intArrayOf(8, 12), intArrayOf(2, 4)),
            objectTypes = mapOf(
                "creature" to creatureType,
                "participant" to participantType,
                "card" to cardType
            ),
            instances = instances
        )
    }
    
    @Test
    fun testCreateObject() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        val result = factory.createObject(
            type = "creature",
            propertyOverrides = mapOf("health" to IntValue(50)),
            stateOverrides = mapOf("activated" to BoolValue(true))
        )
        val creature = result.gameObject
        
        assertEquals("creature", creature.type)
        assertEquals("creature_1", creature.id)
        assertEquals(IntValue(50), creature.getProperty("health"))
        assertEquals(StringValue("Default Creature"), creature.getProperty("name"))
        assertEquals(BoolValue(true), creature.getState("activated"))
        
        // Check that factory was updated
        assertEquals(2, result.updatedFactory.getCurrentIdCounter())
    }
    
    @Test
    fun testCreateObjectWithCustomId() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        val result = factory.createObject(
            type = "creature",
            customId = "my_creature"
        )
        val creature = result.gameObject
        
        assertEquals("my_creature", creature.id)
        assertEquals("creature", creature.type)
        
        // ID counter should not be incremented for custom IDs
        assertEquals(1, result.updatedFactory.getCurrentIdCounter())
    }
    
    @Test
    fun testCreateFromInstance() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        val result = factory.createFromInstance("fire_spell")
        val fireSpell = result.gameObject
        
        assertEquals("card", fireSpell.type)
        assertEquals("card_1", fireSpell.id)
        assertEquals(StringValue("Fire Spell"), fireSpell.getProperty("name"))
        assertEquals(IntValue(3), fireSpell.getProperty("cost"))
        assertEquals(IntValue(4), fireSpell.getProperty("damage"))
        
        assertEquals(2, result.updatedFactory.getCurrentIdCounter())
    }
    
    @Test
    fun testCreateFromInstanceWithCustomId() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        val result = factory.createFromInstance("weak_goblin", "my_goblin")
        val goblin = result.gameObject
        
        assertEquals("my_goblin", goblin.id)
        assertEquals("creature", goblin.type)
        assertEquals(StringValue("Weak Goblin"), goblin.getProperty("name"))
        assertEquals(IntValue(30), goblin.getProperty("health"))
        
        // ID counter should not be incremented for custom IDs
        assertEquals(1, result.updatedFactory.getCurrentIdCounter())
    }
    
    @Test
    fun testCreateInitialSetup() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        val result = factory.createInitialSetup(listOf("Alice", "Bob"))
        val participants = result.gameObjects
        
        assertEquals(2, participants.size)
        
        val alice = participants[0]
        assertEquals("participant", alice.type)
        assertEquals("participant_0", alice.id)
        assertEquals(IntValue(0), alice.getProperty("participant_id"))
        assertEquals(StringValue("Alice"), alice.getProperty("name"))
        
        val bob = participants[1]
        assertEquals("participant", bob.type)
        assertEquals("participant_1", bob.id)
        assertEquals(IntValue(1), bob.getProperty("participant_id"))
        assertEquals(StringValue("Bob"), bob.getProperty("name"))
        
        // Factory ID counter should not change since we used custom IDs
        assertEquals(1, result.updatedFactory.getCurrentIdCounter())
    }
    
    @Test
    fun testCreateInitialSetupWithoutParticipantType() {
        val minimalDefinition = UniversalGameDefinition(
            meta = GameMeta("Minimal", intArrayOf(8, 12), intArrayOf(1, 2)),
            objectTypes = mapOf("card" to ObjectTypeDefinition())
        )
        
        val factory = ObjectFactory(minimalDefinition)
        val result = factory.createInitialSetup(listOf("Alice"))
        
        assertTrue(result.gameObjects.isEmpty())
        assertEquals(factory, result.updatedFactory) // Should be unchanged
    }
    
    @Test
    fun testCreateMultiple() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        val result = factory.createMultiple(
            type = "card",
            count = 3,
            propertyOverrides = mapOf("cost" to IntValue(2))
        )
        val cards = result.gameObjects
        
        assertEquals(3, cards.size)
        
        cards.forEachIndexed { index, card ->
            assertEquals("card", card.type)
            assertEquals("card_${index + 1}", card.id)  // Sequential IDs: card_1, card_2, card_3
            assertEquals(IntValue(2), card.getProperty("cost"))
            assertEquals(IntValue(1), card.getProperty("damage")) // Default value
            assertEquals(StringValue("Basic Card"), card.getProperty("name")) // Default value
        }
        
        assertEquals(4, result.updatedFactory.getCurrentIdCounter()) // 1 + 3 created
    }
    
    @Test
    fun testCreateWithPatterns() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        val substitutions = mapOf(
            "participant_id" to "0",
            "participant_name" to "Alice"
        )
        
        val result = factory.createWithPatterns(
            type = "creature",
            propertyPatterns = mapOf(
                "participant_id" to "{participant_id}",
                "name" to "{participant_name}'s Creature"
            ),
            substitutions = substitutions,
            customId = "creature_{participant_id}"
        )
        val creature = result.gameObject
        
        assertEquals("creature_0", creature.id)
        assertEquals("creature", creature.type)
        assertEquals(IntValue(0), creature.getProperty("participant_id"))
        assertEquals(StringValue("Alice's Creature"), creature.getProperty("name"))
        
        // ID counter should not change since we used custom ID
        assertEquals(1, result.updatedFactory.getCurrentIdCounter())
    }
    
    @Test
    fun testApplySubstitutions() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        val pattern = "Hello {name}, your score is {score}"
        val substitutions = mapOf(
            "name" to "Alice",
            "score" to "100"
        )
        
        val result = factory.applySubstitutions(pattern, substitutions)
        assertEquals("Hello Alice, your score is 100", result)
    }
    
    @Test
    fun testApplySubstitutionsWithMissingKeys() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        val pattern = "Hello {name}, your score is {missing_key}"
        val substitutions = mapOf("name" to "Alice")
        
        val result = factory.applySubstitutions(pattern, substitutions)
        assertEquals("Hello Alice, your score is {missing_key}", result)
    }
    
    @Test
    fun testResetIdCounter() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        // Create some objects to increment counter
        val result1 = factory.createObject("creature")
        val result2 = result1.updatedFactory.createObject("creature")
        
        assertEquals(3, result2.updatedFactory.getCurrentIdCounter())
        
        // Reset counter
        val resetFactory = result2.updatedFactory.resetIdCounter()
        assertEquals(1, resetFactory.getCurrentIdCounter())
        
        // Verify original factory is unchanged
        assertEquals(3, result2.updatedFactory.getCurrentIdCounter())
    }
    
    @Test
    fun testIdCounterImmutability() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        val result1 = factory.createObject("creature")
        val result2 = factory.createObject("creature")
        
        // Both results should have different factories
        assertEquals(2, result1.updatedFactory.getCurrentIdCounter())
        assertEquals(2, result2.updatedFactory.getCurrentIdCounter()) // Same as result1 since both started from original factory
        assertEquals(1, factory.getCurrentIdCounter()) // Original unchanged
        
        // Creating from result1's factory should increment further
        val result3 = result1.updatedFactory.createObject("creature")
        assertEquals(3, result3.updatedFactory.getCurrentIdCounter())
    }
    
    @Test
    fun testCreateFromUnknownObjectType() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        assertFailsWith<IllegalArgumentException> {
            factory.createObject("unknown_type")
        }
    }
    
    @Test
    fun testCreateFromUnknownInstance() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        assertFailsWith<IllegalArgumentException> {
            factory.createFromInstance("unknown_instance")
        }
    }
    
    @Test
    fun testCreateWithPatternsUnknownObjectType() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        assertFailsWith<IllegalArgumentException> {
            factory.createWithPatterns(
                type = "unknown_type",
                propertyPatterns = mapOf("name" to "{participant_name}"),
                substitutions = mapOf("participant_name" to "Alice")
            )
        }
    }
    
    @Test
    fun testCreateWithPatternsUnknownProperty() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        
        assertFailsWith<IllegalArgumentException> {
            factory.createWithPatterns(
                type = "creature",
                propertyPatterns = mapOf("unknown_property" to "{participant_name}"),
                substitutions = mapOf("participant_name" to "Alice")
            )
        }
    }
}