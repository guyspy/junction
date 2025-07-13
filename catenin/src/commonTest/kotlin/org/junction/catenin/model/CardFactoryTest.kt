package org.junction.catenin.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CardFactoryTest {
    
    @Test
    fun testGenerateCards() {
        val gameDefinition = GameDefinition(
            meta = GameMeta(name = "Test Game", targetAge = listOf(8, 12)),
            cards = mapOf(
                "test_card" to CardTypeDefinition(
                    count = 3,
                    properties = mapOf(
                        "damage" to PropertyDefinition(type = "int", min = 1, max = 5),
                        "element" to PropertyDefinition(type = "enum", values = listOf("fire", "water"))
                    )
                )
            )
        )
        
        val factory = CardFactory.fromDefinition(gameDefinition)
        val cards = factory.generateCards()
        
        assertEquals(3, cards.size)
        
        cards.forEach { card ->
            assertTrue(card.id.startsWith("test_card_"))
            assertEquals("test_card", card.type)
            assertEquals(2, card.properties.size)
            
            val damage = card.getIntProperty("damage")
            assertNotNull(damage)
            assertTrue(damage in 1..5)
            
            val element = card.getStringProperty("element")
            assertNotNull(element)
            assertTrue(element in listOf("fire", "water"))
        }
    }
    
    @Test
    fun testCardPropertyAccess() {
        val card = Card(
            id = "test_1",
            type = "test",
            properties = mapOf(
                "damage" to CardPropertyValue.IntValue(3),
                "element" to CardPropertyValue.StringValue("fire"),
                "enabled" to CardPropertyValue.BooleanValue(true)
            )
        )
        
        assertEquals(3, card.getIntProperty("damage"))
        assertEquals("fire", card.getStringProperty("element"))
        assertEquals(null, card.getIntProperty("element")) // Wrong type access
        assertEquals(null, card.getStringProperty("damage")) // Wrong type access
    }
    
    @Test
    fun testCreateCardFactoryFunction() {
        // Test the top-level factory function for JavaScript compatibility
        val gameDefinition = GameDefinition(
            meta = GameMeta(name = "Test Game", targetAge = listOf(8, 12)),
            cards = mapOf(
                "test_card" to CardTypeDefinition(
                    count = 2,
                    properties = mapOf("value" to PropertyDefinition(type = "int", min = 1, max = 3))
                )
            )
        )
        
        val factory = createCardFactory(gameDefinition)
        assertNotNull(factory)
        
        val cards = factory.generateCards()
        assertEquals(2, cards.size)
        cards.forEach { card ->
            assertEquals("test_card", card.type)
        }
    }
}