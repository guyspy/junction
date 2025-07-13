package org.junction.catenin.model

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
        assertEquals(0, player.hand.size)
    }
    
    @Test
    fun testCardManagement() {
        val player = Player(id = "player_1", name = "Alice")
        val card = Card(
            id = "card_1",
            type = "attack",
            properties = mapOf("damage" to CardPropertyValue.IntValue(3))
        )
        
        // Add card (immutable)
        val playerWithCard = player.addCard(card)
        assertEquals(1, playerWithCard.hand.size)
        assertTrue(playerWithCard.hasCard("card_1"))
        assertFalse(playerWithCard.hasCard("card_2"))
        
        // Original player unchanged
        assertEquals(0, player.hand.size)
        
        // Remove card (immutable)
        val (playerWithoutCard, removedCard) = playerWithCard.removeCard("card_1")
        assertNotNull(removedCard)
        assertEquals("card_1", removedCard.id)
        assertEquals(0, playerWithoutCard.hand.size)
        
        // Original player with card unchanged
        assertEquals(1, playerWithCard.hand.size)
        
        // Try to remove non-existent card
        val (unchangedPlayer, nonExistentCard) = playerWithCard.removeCard("card_2")
        assertNull(nonExistentCard)
        assertEquals(playerWithCard, unchangedPlayer) // Should be same player
    }
    
    @Test
    fun testHealthManagement() {
        val player = Player(id = "player_1", name = "Alice", health = 10)
        
        // Take damage (immutable)
        val damagedPlayer = player.takeDamage(3)
        assertEquals(7, damagedPlayer.health)
        assertTrue(damagedPlayer.isAlive())
        
        // Original player unchanged
        assertEquals(10, player.health)
        
        // Take fatal damage
        val deadPlayer = damagedPlayer.takeDamage(10)
        assertEquals(0, deadPlayer.health)
        assertFalse(deadPlayer.isAlive())
        
        // Damaged player unchanged
        assertEquals(7, damagedPlayer.health)
        
        // Heal
        val healedPlayer = deadPlayer.heal(5)
        assertEquals(5, healedPlayer.health)
        assertTrue(healedPlayer.isAlive())
        
        // Dead player unchanged
        assertEquals(0, deadPlayer.health)
    }
}