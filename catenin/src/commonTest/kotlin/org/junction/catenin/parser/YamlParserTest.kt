package org.junction.catenin.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YamlParserTest {
    
    @Test
    fun testParseSimpleGameDefinition() {
        val yaml = """
            meta:
              name: "Test Game"
              target_age: [8, 12]
              player_count: [2, 4]
            
            cards:
              attack_card:
                count: 10
                properties:
                  damage:
                    type: int
                    min: 1
                    max: 5
                  element:
                    type: enum
                    values: [fire, water]
                events:
                  on_play:
                    action: "deal_damage"
                    target: "opponent"
                    amount: "{damage}"
        """.trimIndent()
        
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(yaml)
        
        assertEquals("Test Game", definition.meta.name)
        assertEquals(listOf(8, 12), definition.meta.targetAge)
        assertEquals(1, definition.cards.size)
        
        val attackCard = definition.cards["attack_card"]
        assertNotNull(attackCard)
        assertEquals(10, attackCard.count)
        assertEquals(2, attackCard.properties.size)
        
        val validation = parser.validate(definition)
        assertTrue(validation is ParseResult.Success)
    }
    
    @Test
    fun testValidationFailure() {
        val yaml = """
            meta:
              name: ""
              target_age: [8, 12]
            
            cards: {}
        """.trimIndent()
        
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(yaml)
        val validation = parser.validate(definition)
        
        assertTrue(validation is ParseResult.Failure)
        assertTrue(validation.errors.contains("Game name cannot be empty"))
        assertTrue(validation.errors.contains("Game must define at least one card type"))
    }
}