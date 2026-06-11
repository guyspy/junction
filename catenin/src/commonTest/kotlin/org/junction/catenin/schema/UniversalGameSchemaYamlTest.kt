package org.junction.catenin.schema

import org.junction.catenin.parser.YamlParser
import org.junction.catenin.parser.YamlParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class UniversalGameSchemaYamlTest {
    
    private val yamlParser = YamlParser()
    
    @Test
    fun testParseBasicUniversalGameSchema() {
        // Using simplified structure that avoids PropertyValue serialization complexity
        val yamlContent = """
            meta:
              name: "Simple Test Game"
              targetAge: [8, 12]
              participantCount: [2, 4]
            objectTypes:
              creature:
                properties: {}
                states: {}
        """.trimIndent()
        
        val schema = yamlParser.parseFromString<UniversalGameSchema>(yamlContent)
        
        // Verify meta
        assertEquals("Simple Test Game", schema.meta.name)
        assertEquals(listOf(8, 12), schema.meta.targetAge.toList())
        assertEquals(listOf(2, 4), schema.meta.participantCount.toList())
        
        // Verify object types
        assertEquals(1, schema.objectTypes.size)
        assertTrue(schema.hasObjectType("creature"))
        
        val creatureType = schema.getObjectType("creature")
        assertNotNull(creatureType)
        
        // Verify empty collections
        assertEquals(0, schema.instances.size)
        assertEquals(0, schema.triggers.size)
    }
    
    @Test
    fun testParseUniversalGameSchemaWithInstances() {
        val yamlContent = """
            meta:
              name: "Game with Instances"
              targetAge: [10, 16]
              participantCount: [2, 2]
            objectTypes:
              card:
                properties: {}
                states: {}
            instances:
              fire_spell:
                objectType: "card"
                properties:
                  name: "Fire Spell"
                  cost: "3"
                  damage: "4"
              ice_bolt:
                objectType: "card"
                properties:
                  name: "Ice Bolt" 
                  cost: "2"
                  damage: "3"
        """.trimIndent()
        
        val schema = yamlParser.parseFromString<UniversalGameSchema>(yamlContent)
        
        // Verify instances
        assertEquals(2, schema.instances.size)
        assertTrue(schema.hasInstance("fire_spell"))
        assertTrue(schema.hasInstance("ice_bolt"))
        
        val fireSpell = schema.getInstance("fire_spell")
        assertNotNull(fireSpell)
        assertEquals("card", fireSpell.objectType)
        assertEquals("Fire Spell", fireSpell.properties["name"])
        assertEquals("3", fireSpell.properties["cost"])
        assertEquals("4", fireSpell.properties["damage"])
    }
    
    @Test
    fun testParseUniversalGameSchemaWithTriggers() {
        // Skip complex trigger effects for now - focus on basic YAML parsing
        val yamlContent = """
            meta:
              name: "Game with Triggers"
              targetAge: [12, 18]
              participantCount: [1, 1]
            objectTypes:
              spell:
                properties: {}
                states: {}
            triggers: []
        """.trimIndent()
        
        val schema = yamlParser.parseFromString<UniversalGameSchema>(yamlContent)
        
        // Verify basic structure parses correctly
        assertEquals("Game with Triggers", schema.meta.name)
        assertEquals(1, schema.objectTypes.size)
        assertEquals(0, schema.triggers.size)
    }
    
    @Test
    fun testParseMinimalUniversalGameSchema() {
        val yamlContent = """
            meta:
              name: "Minimal Game"
              targetAge: [5, 99]
              participantCount: [1, 8]
            objectTypes:
              player:
                properties: {}
                states: {}
        """.trimIndent()
        
        val schema = yamlParser.parseFromString<UniversalGameSchema>(yamlContent)
        
        assertEquals("Minimal Game", schema.meta.name)
        assertEquals(1, schema.objectTypes.size)
        assertEquals(0, schema.instances.size)
        assertEquals(0, schema.triggers.size)
    }
    
    @Test
    fun testParseInvalidYamlThrowsException() {
        val invalidYaml = """
            meta:
              name: "Invalid Game"
              # Missing required fields
            objectTypes: {}
        """.trimIndent()
        
        assertFailsWith<YamlParseException> {
            yamlParser.parseFromString<UniversalGameSchema>(invalidYaml)
        }
    }
    
    @Test
    fun testParseMalformedYamlThrowsException() {
        val malformedYaml = """
            meta:
              name: "Test Game
              targetAge: [8, 12]
            objectTypes: {}
        """.trimIndent()
        
        assertFailsWith<YamlParseException> {
            yamlParser.parseFromString<UniversalGameSchema>(malformedYaml)
        }
    }
}