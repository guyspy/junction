package org.junction.catenin.core

import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.IntValue
import org.junction.catenin.model.values.StringValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameWorldTest {
    
    @Test
    fun testEmptyWorld() {
        val world = GameWorld.empty()
        
        assertEquals(0, world.getAllObjects().size)
        assertEquals(1, world.getNextObjectId())
    }
    
    @Test
    fun testAddObject() {
        val world = GameWorld.empty()
        val obj = GameObject("test1", "testType", properties = mapOf("health" to IntValue(100)))
        val update = AddObjectUpdate(obj)
        
        val newWorld = world.applyUpdate(update)
        
        // Test that we get a new world back
        assertTrue(world !== newWorld)
        assertEquals(1, newWorld.getAllObjects().size)
        assertNotNull(newWorld.getObject("test1"))
        assertEquals("testType", newWorld.getObject("test1")?.type)
    }
    
    @Test
    fun testRemoveObject() {
        val world = GameWorld.empty()
        val obj = GameObject("test1", "testType")
        val worldWithObj = world.applyUpdate(AddObjectUpdate(obj))
        
        val update = RemoveObjectUpdate("test1")
        val newWorld = worldWithObj.applyUpdate(update)
        
        assertEquals(0, newWorld.getAllObjects().size)
        assertEquals(null, newWorld.getObject("test1"))
    }
    
    @Test
    fun testUpdateProperty() {
        val world = GameWorld.empty()
        val obj = GameObject("test1", "testType", properties = mapOf("health" to IntValue(100)))
        val worldWithObj = world.applyUpdate(AddObjectUpdate(obj))
        
        val update = UpdatePropertyUpdate("test1", "health", IntValue(50))
        val newWorld = worldWithObj.applyUpdate(update)
        
        val updatedObj = newWorld.getObject("test1")
        assertNotNull(updatedObj)
        assertEquals(IntValue(50), updatedObj.getProperty("health"))
    }
    
    @Test
    fun testUpdateState() {
        val world = GameWorld.empty()
        val obj = GameObject("test1", "testType", states = mapOf("position" to StringValue("back")))
        val worldWithObj = world.applyUpdate(AddObjectUpdate(obj))
        
        val update = UpdateStateUpdate("test1", "position", StringValue("front"))
        val newWorld = worldWithObj.applyUpdate(update)
        
        val updatedObj = newWorld.getObject("test1")
        assertNotNull(updatedObj)
        assertEquals(StringValue("front"), updatedObj.getState("position"))
    }
    
    @Test
    fun testReplaceObject() {
        val world = GameWorld.empty()
        val obj = GameObject("test1", "testType")
        val worldWithObj = world.applyUpdate(AddObjectUpdate(obj))
        
        val newObj = GameObject("test1", "newType")
        val update = ReplaceObjectUpdate("test1", newObj)
        
        val newWorld = worldWithObj.applyUpdate(update)
        val replacedObj = newWorld.getObject("test1")
        assertNotNull(replacedObj)
        assertEquals("newType", replacedObj.type)
    }
}