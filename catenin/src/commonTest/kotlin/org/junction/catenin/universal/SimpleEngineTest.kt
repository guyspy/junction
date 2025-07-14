package org.junction.catenin.universal

import org.junction.catenin.core.*
import org.junction.catenin.model.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class SimpleEngineTest {

    @Test
    fun testBasicGameWorldCreation() {
        // Test: Basic GameWorld creation and object management
        val gameWorld = GameWorld()
        val player = GameObject(
            id = "player_1",
            type = "player",
            properties = mapOf("health" to PropertyValue.IntValue(20))
        )
        
        val updatedWorld = gameWorld.withObject(player)
        
        assertNotNull(updatedWorld.objects["player_1"])
        assertEquals(20, (updatedWorld.objects["player_1"]!!.properties["health"] as PropertyValue.IntValue).value)
    }

    @Test
    fun testBasicTargetResolver() {
        // Test: Basic target resolution by ID
        val targetResolver = TargetResolver()
        val gameWorld = GameWorld(objects = mapOf(
            "player_1" to GameObject(
                id = "player_1",
                type = "player",
                properties = mapOf("health" to PropertyValue.IntValue(20))
            )
        ))
        
        val target = TargetDefinition(id = "player_1")
        val resolved = targetResolver.resolveTargets(target, gameWorld, "0")
        
        assertEquals(1, resolved.size)
        assertEquals("player_1", resolved.first().id)
    }

    @Test
    fun testBasicTriggerEngine() {
        // Test: Basic trigger condition matching
        val triggerEngine = TriggerEngine()
        val trigger = TriggerDefinition(
            name = "test_trigger",
            condition = TriggerCondition(objectType = "player"),
            effects = emptyList()
        )
        
        val player = GameObject(
            id = "player_1",
            type = "player",
            properties = mapOf("health" to PropertyValue.IntValue(15))
        )
        
        val gameWorld = GameWorld()
        val matches = triggerEngine.triggerMatches(
            trigger, player, "health", 
            PropertyValue.IntValue(20), PropertyValue.IntValue(15), gameWorld
        )
        
        assertEquals(true, matches)
    }
}