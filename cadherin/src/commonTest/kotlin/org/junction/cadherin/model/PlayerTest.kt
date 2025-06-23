package org.junction.cadherin.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerTest {
    
    @Test
    fun testPlayerBasicFunctionality() {
        val player = Player(id = "player_1", name = "Alice", health = 15)
        
        assertEquals("player_1", player.id)
        assertEquals("Alice", player.name)
        assertEquals(15, player.health)
        assertEquals(0, player.score)
        assertTrue(player.isAlive())
        assertTrue(player.hand.isEmpty())
    }
    
    @Test
    fun testCardManagement() {
        val player = Player(id = "player_1", name = "Alice")
        val card = Card(
            id = "card_1",
            type = "attack",
            properties = mapOf("damage" to CardPropertyValue.IntValue(3))
        )
        
        // Add card
        player.addCard(card)
        assertEquals(1, player.hand.size)
        assertTrue(player.hasCard("card_1"))
        assertFalse(player.hasCard("card_2"))
        
        // Remove card
        val removedCard = player.removeCard("card_1")
        assertNotNull(removedCard)
        assertEquals("card_1", removedCard.id)
        assertTrue(player.hand.isEmpty())
        
        // Try to remove non-existent card
        val nonExistentCard = player.removeCard("card_2")
        assertNull(nonExistentCard)
    }
    
    @Test
    fun testHealthManagement() {
        val player = Player(id = "player_1", name = "Alice", health = 10)
        
        // Take damage
        player.takeDamage(3)
        assertEquals(7, player.health)
        assertTrue(player.isAlive())
        
        // Take fatal damage
        player.takeDamage(10)
        assertEquals(0, player.health)
        assertFalse(player.isAlive())
        
        // Heal
        player.heal(5)
        assertEquals(5, player.health)
        assertTrue(player.isAlive())
    }
}