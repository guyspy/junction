package org.junction.catenin.factory

import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.ObjectInstance
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.test.*

class ObjectFactoryTest {
    
    private fun createTestSchema(): UniversalGameSchema {
        val meta = GameMeta(
            name = "Test Game",
            targetAge = intArrayOf(8, 12),
            participantCount = intArrayOf(2, 4)
        )
        
        val cardType = ObjectTypeDefinition(
            properties = mapOf(
                "cost" to PropertyDefinition(
                    type = PropertyType.INT,
                    initial = IntValue(1)
                ),
                "name" to PropertyDefinition(
                    type = PropertyType.STRING,
                    initial = StringValue("Default Card")
                )
            ),
            states = mapOf(
                "tapped" to PropertyDefinition(
                    type = PropertyType.BOOL,
                    initial = BoolValue(false)
                )
            )
        )
        
        val objectTypes = mapOf("card" to cardType)
        
        val instances = mapOf(
            "lightning_bolt" to ObjectInstance(
                objectType = "card",
                properties = mapOf(
                    "name" to "Lightning Bolt",
                    "cost" to "3"
                )
            )
        )
        
        return UniversalGameSchema(
            meta = meta,
            objectTypes = objectTypes,
            instances = instances
        )
    }
    
    @Test
    fun testCreateObjectFromType() {
        val schema = createTestSchema()
        val factory = ObjectFactory(schema)
        
        val obj = factory.createObject("card")
        
        assertEquals("card", obj.type)
        assertTrue(obj.id.startsWith("card_"))
        assertEquals(IntValue(1), obj.getProperty("cost"))
        assertEquals(StringValue("Default Card"), obj.getProperty("name"))
        assertEquals(BoolValue(false), obj.getState("tapped"))
    }
    
    @Test
    fun testCreateObjectWithOverrides() {
        val schema = createTestSchema()
        val factory = ObjectFactory(schema)
        
        val obj = factory.createObject(
            objectType = "card",
            propertyOverrides = mapOf(
                "cost" to "5",
                "name" to "Expensive Card"
            )
        )
        
        assertEquals(IntValue(5), obj.getProperty("cost"))
        assertEquals(StringValue("Expensive Card"), obj.getProperty("name"))
    }
    
    @Test
    fun testCreateObjectWithCustomId() {
        val schema = createTestSchema()
        val factory = ObjectFactory(schema)
        
        val obj = factory.createObject("card", id = "my_custom_id")
        
        assertEquals("my_custom_id", obj.id)
    }
    
    @Test
    fun testCreateFromInstance() {
        val schema = createTestSchema()
        val factory = ObjectFactory(schema)
        
        val obj = factory.createFromInstance("lightning_bolt")
        
        assertEquals("card", obj.type)
        assertEquals(StringValue("Lightning Bolt"), obj.getProperty("name"))
        assertEquals(IntValue(3), obj.getProperty("cost"))
        assertEquals(BoolValue(false), obj.getState("tapped"))
    }
    
    @Test
    fun testCreateUnknownObjectType() {
        val schema = createTestSchema()
        val factory = ObjectFactory(schema)
        
        assertFailsWith<IllegalArgumentException> {
            factory.createObject("unknown_type")
        }
    }
    
    @Test
    fun testCreateUnknownInstance() {
        val schema = createTestSchema()
        val factory = ObjectFactory(schema)
        
        assertFailsWith<IllegalArgumentException> {
            factory.createFromInstance("unknown_instance")
        }
    }
    
    @Test
    fun testUniqueIdGeneration() {
        val schema = createTestSchema()
        val factory = ObjectFactory(schema)
        
        val obj1 = factory.createObject("card")
        val obj2 = factory.createObject("card")
        val obj3 = factory.createObject("card")
        
        assertNotEquals(obj1.id, obj2.id)
        assertNotEquals(obj2.id, obj3.id)
        assertNotEquals(obj1.id, obj3.id)
    }
}