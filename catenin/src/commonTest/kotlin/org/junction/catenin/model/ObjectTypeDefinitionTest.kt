package org.junction.catenin.model

import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.*

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObjectTypeDefinitionTest {
    
    @Test
    fun testEmptyObjectTypeDefinition() {
        val definition = ObjectTypeDefinition()
        
        assertEquals(0, definition.properties.size)
        assertEquals(0, definition.states.size)
        assertEquals(emptyList(), definition.getAllPropertyNames())
        assertEquals(emptyList(), definition.getAllStateNames())
    }
    
    @Test
    fun testObjectTypeDefinitionWithProperties() {
        val healthProp = PropertyDefinition(PropertyType.INT, IntValue(100), IntValue(0), IntValue(200))
        val nameProp = PropertyDefinition(PropertyType.STRING, StringValue("Default"))
        
        val definition = ObjectTypeDefinition(
            properties = mapOf(
                "health" to healthProp,
                "name" to nameProp
            )
        )
        
        assertEquals(2, definition.properties.size)
        assertEquals(0, definition.states.size)
        assertTrue(definition.hasProperty("health"))
        assertTrue(definition.hasProperty("name"))
        assertFalse(definition.hasProperty("unknown"))
        
        assertEquals(healthProp, definition.getPropertyDefinition("health"))
        assertEquals(nameProp, definition.getPropertyDefinition("name"))
        assertNull(definition.getPropertyDefinition("unknown"))
        
        assertEquals(listOf("health", "name"), definition.getAllPropertyNames().sorted())
    }
    
    @Test
    fun testObjectTypeDefinitionWithStates() {
        val activatedState = PropertyDefinition(PropertyType.BOOL, BoolValue(false))
        val statusState = PropertyDefinition(PropertyType.STRING, StringValue("normal"))
        
        val definition = ObjectTypeDefinition(
            states = mapOf(
                "activated" to activatedState,
                "status" to statusState
            )
        )
        
        assertEquals(0, definition.properties.size)
        assertEquals(2, definition.states.size)
        assertTrue(definition.hasState("activated"))
        assertTrue(definition.hasState("status"))
        assertFalse(definition.hasState("unknown"))
        
        assertEquals(activatedState, definition.getStateDefinition("activated"))
        assertEquals(statusState, definition.getStateDefinition("status"))
        assertNull(definition.getStateDefinition("unknown"))
        
        assertEquals(listOf("activated", "status"), definition.getAllStateNames().sorted())
    }
    
    @Test
    fun testWithProperty() {
        val definition = ObjectTypeDefinition()
        val healthProp = PropertyDefinition(PropertyType.INT, IntValue(100))
        
        val newDefinition = definition.withProperty("health", healthProp)
        
        // Original should be unchanged
        assertEquals(0, definition.properties.size)
        assertFalse(definition.hasProperty("health"))
        
        // New definition should have the property
        assertEquals(1, newDefinition.properties.size)
        assertTrue(newDefinition.hasProperty("health"))
        assertEquals(healthProp, newDefinition.getPropertyDefinition("health"))
    }
    
    @Test
    fun testWithState() {
        val definition = ObjectTypeDefinition()
        val activatedState = PropertyDefinition(PropertyType.BOOL, BoolValue(false))
        
        val newDefinition = definition.withState("activated", activatedState)
        
        // Original should be unchanged
        assertEquals(0, definition.states.size)
        assertFalse(definition.hasState("activated"))
        
        // New definition should have the state
        assertEquals(1, newDefinition.states.size)
        assertTrue(newDefinition.hasState("activated"))
        assertEquals(activatedState, newDefinition.getStateDefinition("activated"))
    }
    
    @Test
    fun testWithoutProperty() {
        val healthProp = PropertyDefinition(PropertyType.INT, IntValue(100))
        val nameProp = PropertyDefinition(PropertyType.STRING, StringValue("Default"))
        
        val definition = ObjectTypeDefinition(
            properties = mapOf(
                "health" to healthProp,
                "name" to nameProp
            )
        )
        
        val newDefinition = definition.withoutProperty("health")
        
        // Original should be unchanged
        assertEquals(2, definition.properties.size)
        assertTrue(definition.hasProperty("health"))
        
        // New definition should not have the property
        assertEquals(1, newDefinition.properties.size)
        assertFalse(newDefinition.hasProperty("health"))
        assertTrue(newDefinition.hasProperty("name"))
    }
    
    @Test
    fun testWithoutState() {
        val activatedState = PropertyDefinition(PropertyType.BOOL, BoolValue(false))
        val statusState = PropertyDefinition(PropertyType.STRING, StringValue("normal"))
        
        val definition = ObjectTypeDefinition(
            states = mapOf(
                "activated" to activatedState,
                "status" to statusState
            )
        )
        
        val newDefinition = definition.withoutState("activated")
        
        // Original should be unchanged
        assertEquals(2, definition.states.size)
        assertTrue(definition.hasState("activated"))
        
        // New definition should not have the state
        assertEquals(1, newDefinition.states.size)
        assertFalse(newDefinition.hasState("activated"))
        assertTrue(newDefinition.hasState("status"))
    }
    
    @Test
    fun testValidateObjectSuccess() {
        val healthProp = PropertyDefinition(PropertyType.INT, IntValue(100), IntValue(0), IntValue(200))
        val activatedState = PropertyDefinition(PropertyType.BOOL, BoolValue(false))
        
        val definition = ObjectTypeDefinition(
            properties = mapOf("health" to healthProp),
            states = mapOf("activated" to activatedState)
        )
        
        val obj = GameObject(
            id = "test",
            type = "creature",
            properties = mapOf("health" to IntValue(150)),
            states = mapOf("activated" to BoolValue(true))
        )
        
        val errors = definition.validateObject(obj)
        assertEquals(emptyList(), errors)
    }
    
    @Test
    fun testValidateObjectWithInvalidProperty() {
        val healthProp = PropertyDefinition(PropertyType.INT, IntValue(100), IntValue(0), IntValue(200))
        
        val definition = ObjectTypeDefinition(
            properties = mapOf("health" to healthProp)
        )
        
        val obj = GameObject(
            id = "test",
            type = "creature",
            properties = mapOf("health" to IntValue(250)) // exceeds max
        )
        
        val errors = definition.validateObject(obj)
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Property 'health' has invalid value"))
    }
    
    @Test
    fun testValidateObjectWithMissingProperty() {
        val healthProp = PropertyDefinition(PropertyType.INT, IntValue(100), IntValue(0), IntValue(200))
        
        val definition = ObjectTypeDefinition(
            properties = mapOf("health" to healthProp)
        )
        
        val obj = GameObject(
            id = "test",
            type = "creature"
            // Missing health property
        )
        
        val errors = definition.validateObject(obj)
        assertEquals(emptyList(), errors) // Missing properties are allowed (will use defaults)
    }
    
    @Test
    fun testCreateObjectWithDefaults() {
        val healthProp = PropertyDefinition(PropertyType.INT, IntValue(100), IntValue(0), IntValue(200))
        val activatedState = PropertyDefinition(PropertyType.BOOL, BoolValue(false))
        
        val definition = ObjectTypeDefinition(
            properties = mapOf("health" to healthProp),
            states = mapOf("activated" to activatedState)
        )
        
        val obj = definition.createObject("test", "creature")
        
        assertEquals("test", obj.id)
        assertEquals("creature", obj.type)
        assertEquals(IntValue(100), obj.getProperty("health"))
        assertEquals(BoolValue(false), obj.getState("activated"))
    }
    
    @Test
    fun testCreateObjectWithOverrides() {
        val healthProp = PropertyDefinition(PropertyType.INT, IntValue(100), IntValue(0), IntValue(200))
        val activatedState = PropertyDefinition(PropertyType.BOOL, BoolValue(false))
        
        val definition = ObjectTypeDefinition(
            properties = mapOf("health" to healthProp),
            states = mapOf("activated" to activatedState)
        )
        
        val obj = definition.createObject(
            id = "test",
            type = "creature",
            propertyOverrides = mapOf("health" to IntValue(150)),
            stateOverrides = mapOf("activated" to BoolValue(true))
        )
        
        assertEquals("test", obj.id)
        assertEquals("creature", obj.type)
        assertEquals(IntValue(150), obj.getProperty("health"))
        assertEquals(BoolValue(true), obj.getState("activated"))
    }
    
    @Test
    fun testCreateObjectWithAdditionalOverrides() {
        val healthProp = PropertyDefinition(PropertyType.INT, IntValue(100))
        
        val definition = ObjectTypeDefinition(
            properties = mapOf("health" to healthProp)
        )
        
        val obj = definition.createObject(
            id = "test",
            type = "creature",
            propertyOverrides = mapOf(
                "health" to IntValue(150),
                "name" to StringValue("Goblin") // Additional property not in definition
            )
        )
        
        assertEquals("test", obj.id)
        assertEquals("creature", obj.type)
        assertEquals(IntValue(150), obj.getProperty("health"))
        assertEquals(StringValue("Goblin"), obj.getProperty("name"))
    }
}