package org.junction.catenin.universal

import org.junction.catenin.core.ObjectFactory
import org.junction.catenin.model.*
import org.junction.catenin.parser.UniversalGameParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class UniversalObjectSystemTest {

    private val testGameYaml = """
        meta:
          name: "Test Game"
          target_age: [8, 12]
          participant_count: [2, 4]

        object_types:
          player:
            properties:
              health: {type: INT, initial: 20, min: 0}
              mana: {type: INT, initial: 0, min: 0, max: 10}
              name: {type: STRING}
              participant_id: {type: INT}
            states:
              active: {type: BOOL, initial: false}

          card:
            properties:
              name: {type: STRING}
              cost: {type: INT, min: 0}
              attack: {type: INT, min: 0}
            states:
              tapped: {type: BOOL, initial: false}

          container:
            properties:
              name: {type: STRING}
              max_size: {type: INT, initial: -1}

        instances:
          fire_bolt:
            template: card
            properties:
              name: "Fire Bolt"
              cost: 2
              attack: 3

          healing_spell:
            template: card
            properties:
              name: "Healing Spell"
              cost: 1
              attack: 0
    """.trimIndent()

    @Test
    fun testParseUniversalGameDefinition() {
        val parser = UniversalGameParser()
        val definition = parser.parseFromString(testGameYaml)
        
        assertEquals("Test Game", definition.meta.name)
        assertEquals(3, definition.objectTypes.size)
        assertEquals(2, definition.instances.size)
        
        // Check object types
        assertTrue(definition.objectTypes.containsKey("player"))
        assertTrue(definition.objectTypes.containsKey("card"))
        assertTrue(definition.objectTypes.containsKey("container"))
        
        // Check player object type
        val playerDef = definition.objectTypes["player"]!!
        assertEquals(3, playerDef.properties.size)
        assertEquals(PropertyType.INT, playerDef.properties["health"]!!.type)
        assertEquals(PropertyValue.IntValue(20), playerDef.properties["health"]!!.initial)
        
        // Check instances
        val fireBolt = definition.instances["fire_bolt"]!!
        assertEquals("card", fireBolt.template)
        assertEquals(PropertyValue.StringValue("Fire Bolt"), fireBolt.properties["name"])
        assertEquals(PropertyValue.IntValue(2), fireBolt.properties["cost"])
    }

    @Test
    fun testObjectFactoryCreateObject() {
        val parser = UniversalGameParser()
        val definition = parser.parseFromString(testGameYaml)
        val factory = ObjectFactory(definition)
        
        // Create a player object
        val player = factory.createObject("player", mapOf(
            "name" to PropertyValue.StringValue("Alice")
        ))
        
        assertTrue(player.id.startsWith("player_"))
        assertEquals("player", player.type)
        assertEquals(PropertyValue.IntValue(20), player.properties["health"])
        assertEquals(PropertyValue.IntValue(0), player.properties["mana"])
        assertEquals(PropertyValue.StringValue("Alice"), player.properties["name"])
        assertEquals(PropertyValue.BoolValue(false), player.states["active"])
    }

    @Test
    fun testObjectFactoryCreateFromInstance() {
        val parser = UniversalGameParser()
        val definition = parser.parseFromString(testGameYaml)
        val factory = ObjectFactory(definition)
        
        // Create from predefined instance
        val fireBolt = factory.createFromInstance("fire_bolt")
        
        assertEquals("fire_bolt", fireBolt.id)
        assertEquals("card", fireBolt.type)
        assertEquals(PropertyValue.StringValue("Fire Bolt"), fireBolt.properties["name"])
        assertEquals(PropertyValue.IntValue(2), fireBolt.properties["cost"])
        assertEquals(PropertyValue.IntValue(3), fireBolt.properties["attack"])
        assertEquals(PropertyValue.BoolValue(false), fireBolt.states["tapped"])
    }

    @Test
    fun testObjectFactoryCreateAllInstances() {
        val parser = UniversalGameParser()
        val definition = parser.parseFromString(testGameYaml)
        val factory = ObjectFactory(definition)
        
        val instances = factory.createAllInstances()
        
        assertEquals(2, instances.size)
        
        val fireBolt = instances.find { it.id == "fire_bolt" }!!
        assertEquals("card", fireBolt.type)
        assertEquals(PropertyValue.StringValue("Fire Bolt"), fireBolt.properties["name"])
        
        val healingSpell = instances.find { it.id == "healing_spell" }!!
        assertEquals("card", healingSpell.type)
        assertEquals(PropertyValue.StringValue("Healing Spell"), healingSpell.properties["name"])
    }

    @Test
    fun testObjectFactoryInitialSetup() {
        val parser = UniversalGameParser()
        val definition = parser.parseFromString(testGameYaml)
        val factory = ObjectFactory(definition)
        
        val objects = factory.createInitialSetup(listOf("Alice", "Bob"))
        
        // Should have 2 players + 2 predefined instances = 4 objects
        assertEquals(4, objects.size)
        
        val players = objects.filter { it.type == "player" }
        assertEquals(2, players.size)
        
        val alice = players.find { it.properties["name"] == PropertyValue.StringValue("Alice") }!!
        assertEquals(PropertyValue.IntValue(0), alice.properties["participant_id"])
        
        val bob = players.find { it.properties["name"] == PropertyValue.StringValue("Bob") }!!
        assertEquals(PropertyValue.IntValue(1), bob.properties["participant_id"])
        
        val cards = objects.filter { it.type == "card" }
        assertEquals(2, cards.size)
    }

    @Test
    fun testInvalidObjectType() {
        val parser = UniversalGameParser()
        val definition = parser.parseFromString(testGameYaml)
        val factory = ObjectFactory(definition)
        
        assertFailsWith<IllegalArgumentException> {
            factory.createObject("nonexistent_type")
        }
    }

    @Test
    fun testInvalidInstance() {
        val parser = UniversalGameParser()
        val definition = parser.parseFromString(testGameYaml)
        val factory = ObjectFactory(definition)
        
        assertFailsWith<IllegalArgumentException> {
            factory.createFromInstance("nonexistent_instance")
        }
    }

    @Test
    fun testValidation() {
        val parser = UniversalGameParser()
        val definition = parser.parseFromString(testGameYaml)
        
        val errors = parser.validate(definition)
        assertTrue(errors.isEmpty(), "Validation should pass for valid definition")
    }

    @Test
    fun testValidationWithErrors() {
        val invalidYaml = """
            meta:
              name: "Invalid Game"
              target_age: [8, 12]
              participant_count: [2, 4]

            object_types:
              player:
                properties:
                  health: {type: INT, initial: 20}

            instances:
              invalid_card:
                template: nonexistent_type
                properties:
                  name: "Invalid"
        """.trimIndent()
        
        val parser = UniversalGameParser()
        val definition = parser.parseFromString(invalidYaml)
        
        val errors = parser.validate(definition)
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("nonexistent_type") })
    }
}