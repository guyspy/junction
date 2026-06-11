package org.junction.catenin.model

import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.*

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class GameObjectTest {

    @Test
    fun testBasicGameObjectCreation() {
        val obj = GameObject(
            id = "player_1",
            type = "Player",
            properties = mapOf(
                "health" to IntValue(100),
                "name" to StringValue("Alice")
            ),
            states = mapOf(
                "active" to BoolValue(true)
            )
        )
        
        assertEquals("player_1", obj.id)
        assertEquals("Player", obj.type)
        assertEquals(IntValue(100), obj.properties["health"])
        assertEquals(StringValue("Alice"), obj.properties["name"])
        assertEquals(BoolValue(true), obj.states["active"])
    }

    @Test
    fun testGameObjectImmutability() {
        val originalObj = GameObject(
            id = "test_obj",
            type = "TestType",
            properties = mapOf("value" to IntValue(10)),
            states = mapOf("state" to BoolValue(false))
        )
        
        val updatedObj = originalObj.withProperty("value", IntValue(20))
        
        // Original object should be unchanged
        assertEquals(IntValue(10), originalObj.properties["value"])
        
        // New object should have updated value
        assertEquals(IntValue(20), updatedObj.properties["value"])
        
        // Objects should be different instances
        assertTrue(originalObj !== updatedObj)
    }

    @Test
    fun testWithProperty() {
        val obj = GameObject(
            id = "test",
            type = "Test",
            properties = mapOf("health" to IntValue(100))
        )
        
        val updated = obj.withProperty("health", IntValue(80))
        assertEquals(IntValue(80), updated.properties["health"])
        
        val withNew = obj.withProperty("mana", IntValue(50))
        assertEquals(IntValue(50), withNew.properties["mana"])
        assertEquals(IntValue(100), withNew.properties["health"]) // Original should remain
    }

    @Test
    fun testWithState() {
        val obj = GameObject(
            id = "test",
            type = "Test",
            states = mapOf("active" to BoolValue(true))
        )
        
        val updated = obj.withState("active", BoolValue(false))
        assertEquals(BoolValue(false), updated.states["active"])
        
        val withNew = obj.withState("stunned", BoolValue(true))
        assertEquals(BoolValue(true), withNew.states["stunned"])
        assertEquals(BoolValue(true), withNew.states["active"]) // Original should remain
    }

    @Test
    fun testWithoutProperty() {
        val obj = GameObject(
            id = "test",
            type = "Test",
            properties = mapOf(
                "health" to IntValue(100),
                "mana" to IntValue(50)
            )
        )
        
        val updated = obj.withoutProperty("mana")
        assertFalse(updated.properties.containsKey("mana"))
        assertTrue(updated.properties.containsKey("health"))
    }

    @Test
    fun testWithoutState() {
        val obj = GameObject(
            id = "test",
            type = "Test",
            states = mapOf(
                "active" to BoolValue(true),
                "stunned" to BoolValue(false)
            )
        )
        
        val updated = obj.withoutState("stunned")
        assertFalse(updated.states.containsKey("stunned"))
        assertTrue(updated.states.containsKey("active"))
    }

    @Test
    fun testGetProperty() {
        val obj = GameObject(
            id = "test",
            type = "Test",
            properties = mapOf("health" to IntValue(100))
        )
        
        assertEquals(IntValue(100), obj.getProperty("health"))
        assertEquals(null, obj.getProperty("nonexistent"))
    }

    @Test
    fun testGetState() {
        val obj = GameObject(
            id = "test",
            type = "Test",
            states = mapOf("active" to BoolValue(true))
        )
        
        assertEquals(BoolValue(true), obj.getState("active"))
        assertEquals(null, obj.getState("nonexistent"))
    }

    @Test
    fun testHasProperty() {
        val obj = GameObject(
            id = "test",
            type = "Test",
            properties = mapOf("health" to IntValue(100))
        )
        
        assertTrue(obj.hasProperty("health"))
        assertFalse(obj.hasProperty("nonexistent"))
    }

    @Test
    fun testHasState() {
        val obj = GameObject(
            id = "test",
            type = "Test",
            states = mapOf("active" to BoolValue(true))
        )
        
        assertTrue(obj.hasState("active"))
        assertFalse(obj.hasState("nonexistent"))
    }

    @Test
    fun testGetAllPropertyNames() {
        val obj = GameObject(
            id = "test",
            type = "Test",
            properties = mapOf(
                "health" to IntValue(100),
                "name" to StringValue("Test")
            )
        )
        
        val propertyNames = obj.getAllPropertyNames()
        assertEquals(2, propertyNames.size)
        assertTrue(propertyNames.contains("health"))
        assertTrue(propertyNames.contains("name"))
    }

    @Test
    fun testGetAllStateNames() {
        val obj = GameObject(
            id = "test",
            type = "Test",
            states = mapOf(
                "active" to BoolValue(true),
                "stunned" to BoolValue(false)
            )
        )
        
        val stateNames = obj.getAllStateNames()
        assertEquals(2, stateNames.size)
        assertTrue(stateNames.contains("active"))
        assertTrue(stateNames.contains("stunned"))
    }

    @Test
    fun testGameObjectEquality() {
        val obj1 = GameObject(
            id = "test",
            type = "Test",
            properties = mapOf("health" to IntValue(100))
        )
        
        val obj2 = GameObject(
            id = "test",
            type = "Test",
            properties = mapOf("health" to IntValue(100))
        )
        
        val obj3 = GameObject(
            id = "test",
            type = "Test",
            properties = mapOf("health" to IntValue(90))
        )
        
        assertEquals(obj1, obj2)
        assertTrue(obj1 != obj3)
    }

    @Test
    fun testEmptyGameObject() {
        val obj = GameObject(
            id = "empty",
            type = "Empty"
        )
        
        assertTrue(obj.properties.isEmpty())
        assertTrue(obj.states.isEmpty())
        assertTrue(obj.getAllPropertyNames().isEmpty())
        assertTrue(obj.getAllStateNames().isEmpty())
    }
}