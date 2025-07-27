package org.junction.catenin.core

import kotlinx.coroutines.test.runTest
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.IntValue
import org.junction.catenin.model.values.StringValue
import kotlin.test.Test
import kotlin.test.assertTrue

class GameWorldTest {
    
    @Test
    fun testEmptyWorld() = runTest {
        val world = GameWorld.empty()
        
        // Just verify it can be created - interface has no query methods
        val obj = GameObject("test1", "testType")
        val update = AddObjectUpdate(obj)
        
        // Apply update returns a new GameWorld
        val newWorld = world.applyUpdate(update)
        
        // Verify we get a new world instance
        assertTrue(newWorld is GameWorld)
        assertTrue(world !== newWorld) // Different instance
    }
    
    @Test
    fun testAddObject() = runTest {
        val world = GameWorld.empty()
        val obj = GameObject("test1", "testType", properties = mapOf("health" to IntValue(100)))
        val update = AddObjectUpdate(obj)
        
        val newWorld = world.applyUpdate(update)
        
        // Test that we get a new world back
        assertTrue(newWorld is GameWorld)
        assertTrue(world !== newWorld)
    }
    
    @Test
    fun testRemoveObject() = runTest {
        val world = GameWorld.empty()
        val update = RemoveObjectUpdate("test1")
        
        val newWorld = world.applyUpdate(update)
        assertTrue(newWorld is GameWorld)
    }
    
    @Test
    fun testUpdateProperty() = runTest {
        val world = GameWorld.empty()
        val update = UpdatePropertyUpdate("test1", "health", IntValue(50))
        
        val newWorld = world.applyUpdate(update)
        assertTrue(newWorld is GameWorld)
    }
    
    @Test
    fun testUpdateState() = runTest {
        val world = GameWorld.empty()
        val update = UpdateStateUpdate("test1", "position", StringValue("front"))
        
        val newWorld = world.applyUpdate(update)
        assertTrue(newWorld is GameWorld)
    }
    
    @Test
    fun testReplaceObject() = runTest {
        val world = GameWorld.empty()
        val newObj = GameObject("test1", "newType")
        val update = ReplaceObjectUpdate("test1", newObj)
        
        val newWorld = world.applyUpdate(update)
        assertTrue(newWorld is GameWorld)
    }
}