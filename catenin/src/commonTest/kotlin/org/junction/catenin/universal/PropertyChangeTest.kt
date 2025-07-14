package org.junction.catenin.universal

import org.junction.catenin.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PropertyChangeTest {

    @Test
    fun testPropertyValueTypes() {
        val intValue = PropertyValue.IntValue(42)
        val stringValue = PropertyValue.StringValue("test")
        val boolValue = PropertyValue.BoolValue(true)
        val objRefValue = PropertyValue.ObjectRefValue("obj_123")
        
        assertEquals(42, (intValue as PropertyValue.IntValue).value)
        assertEquals("test", (stringValue as PropertyValue.StringValue).value)
        assertEquals(true, (boolValue as PropertyValue.BoolValue).value)
        assertEquals("obj_123", (objRefValue as PropertyValue.ObjectRefValue).objectId)
    }

    @Test
    fun testPropertyValueEquality() {
        val int1 = PropertyValue.IntValue(42)
        val int2 = PropertyValue.IntValue(42)
        val int3 = PropertyValue.IntValue(43)
        
        assertEquals(int1, int2)
        assertNotEquals(int1, int3)
        
        val str1 = PropertyValue.StringValue("hello")
        val str2 = PropertyValue.StringValue("hello")
        val str3 = PropertyValue.StringValue("world")
        
        assertEquals(str1, str2)
        assertNotEquals(str1, str3)
    }

    @Test
    fun testGameObjectPropertyChanges() {
        val originalObject = GameObject(
            id = "player_1",
            type = "player",
            properties = mapOf(
                "health" to PropertyValue.IntValue(20),
                "name" to PropertyValue.StringValue("Alice")
            ),
            states = mapOf(
                "active" to PropertyValue.BoolValue(false)
            )
        )
        
        // Test property change
        val updatedProperties = originalObject.properties.toMutableMap()
        updatedProperties["health"] = PropertyValue.IntValue(15)
        
        val updatedObject = originalObject.copy(properties = updatedProperties)
        
        assertEquals(PropertyValue.IntValue(15), updatedObject.properties["health"])
        assertEquals(PropertyValue.StringValue("Alice"), updatedObject.properties["name"])
        
        // Original should be unchanged (immutable)
        assertEquals(PropertyValue.IntValue(20), originalObject.properties["health"])
    }

    @Test
    fun testGameObjectStateChanges() {
        val originalObject = GameObject(
            id = "card_1",
            type = "card",
            properties = mapOf(
                "name" to PropertyValue.StringValue("Fire Bolt")
            ),
            states = mapOf(
                "tapped" to PropertyValue.BoolValue(false),
                "face_up" to PropertyValue.BoolValue(true)
            )
        )
        
        // Test state change
        val updatedStates = originalObject.states.toMutableMap()
        updatedStates["tapped"] = PropertyValue.BoolValue(true)
        
        val updatedObject = originalObject.copy(states = updatedStates)
        
        assertEquals(PropertyValue.BoolValue(true), updatedObject.states["tapped"])
        assertEquals(PropertyValue.BoolValue(true), updatedObject.states["face_up"])
        
        // Original should be unchanged (immutable)
        assertEquals(PropertyValue.BoolValue(false), originalObject.states["tapped"])
    }

    @Test
    fun testParentRelationshipChanges() {
        val card = GameObject(
            id = "card_1",
            type = "card",
            properties = mapOf("name" to PropertyValue.StringValue("Fire Bolt")),
            parentId = null
        )
        
        // Move card to a container
        val cardInHand = card.copy(parentId = "hand_container")
        assertEquals("hand_container", cardInHand.parentId)
        assertEquals(null, card.parentId) // Original unchanged
        
        // Move card to battlefield
        val cardInPlay = cardInHand.copy(parentId = "battlefield_container")
        assertEquals("battlefield_container", cardInPlay.parentId)
        assertEquals("hand_container", cardInHand.parentId) // Previous unchanged
    }

    @Test
    fun testZoneRelationshipChanges() {
        val player = GameObject(
            id = "player_1",
            type = "player",
            properties = mapOf("name" to PropertyValue.StringValue("Alice")),
            zoneIds = mapOf(
                "hand" to "hand_zone_1",
                "graveyard" to "graveyard_zone_1"
            )
        )
        
        // Update zone relationships
        val updatedZones = player.zoneIds.toMutableMap()
        updatedZones["battlefield"] = "battlefield_zone_1"
        
        val updatedPlayer = player.copy(zoneIds = updatedZones)
        
        assertEquals("battlefield_zone_1", updatedPlayer.zoneIds["battlefield"])
        assertEquals("hand_zone_1", updatedPlayer.zoneIds["hand"])
        assertEquals(null, player.zoneIds["battlefield"]) // Original unchanged
    }

    @Test
    fun testComplexPropertyChanges() {
        val container = GameObject(
            id = "deck_1",
            type = "container",
            properties = mapOf(
                "name" to PropertyValue.StringValue("Main Deck"),
                "max_size" to PropertyValue.IntValue(60),
                "current_size" to PropertyValue.IntValue(52),
                "owner" to PropertyValue.ObjectRefValue("player_1")
            )
        )
        
        // Simulate drawing a card (reducing size)
        val updatedProperties = container.properties.toMutableMap()
        val currentSize = (container.properties["current_size"] as PropertyValue.IntValue).value
        updatedProperties["current_size"] = PropertyValue.IntValue(currentSize - 1)
        
        val updatedContainer = container.copy(properties = updatedProperties)
        
        assertEquals(PropertyValue.IntValue(51), updatedContainer.properties["current_size"])
        assertEquals(PropertyValue.IntValue(52), container.properties["current_size"])
    }
}