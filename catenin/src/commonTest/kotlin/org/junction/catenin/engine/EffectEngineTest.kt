package org.junction.catenin.engine

import org.junction.catenin.core.*
import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.triggers.*
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.test.*

class EffectEngineTest {
    
    private fun createTestSchema(): UniversalGameSchema {
        val meta = GameMeta(
            name = "Test Game",
            targetAge = intArrayOf(8, 12),
            participantCount = intArrayOf(2, 2)
        )
        
        val unitType = ObjectTypeDefinition(
            properties = mapOf(
                "health" to PropertyDefinition(
                    type = PropertyType.INT,
                    initial = IntValue(10)
                ),
                "armor" to PropertyDefinition(
                    type = PropertyType.INT,
                    initial = IntValue(0)
                ),
                "name" to PropertyDefinition(
                    type = PropertyType.STRING,
                    initial = StringValue("Unit")
                ),
                "active" to PropertyDefinition(
                    type = PropertyType.BOOL,
                    initial = BoolValue(true)
                )
            )
        )
        
        return UniversalGameSchema(
            meta = meta,
            objectTypes = mapOf("unit" to unitType)
        )
    }
    
    @Test
    fun testLogEffectGeneratesNoUpdates() {
        val schema = createTestSchema()
        val targetResolver = SimpleTargetResolver()
        val engine = EffectEngine(schema, targetResolver)
        
        val unit = GameObject("unit1", "unit")
        val world = GameWorld.empty()
        
        val effect = LogEffect("Test log message")
        val updates = engine.executeEffect(effect, unit, world)
        
        assertEquals(0, updates.size)
    }
    
    @Test
    fun testModifyPropertyEffectWithDelta() {
        val schema = createTestSchema()
        val targetResolver = SimpleTargetResolver()
        val engine = EffectEngine(schema, targetResolver)
        
        val unit = GameObject(
            id = "unit1",
            type = "unit",
            properties = mapOf(
                "health" to IntValue(10),
                "armor" to IntValue(0)
            )
        )
        val world = GameWorld.empty().applyUpdate(AddObjectUpdate(unit))
        
        // Effect: Add 2 to armor of self
        val effect = ModifyPropertyEffect(
            target = "self",
            property = "armor",
            delta = "2"
        )
        
        val updates = engine.executeEffect(effect, unit, world)
        
        assertEquals(1, updates.size)
        val update = updates[0] as UpdatePropertyUpdate
        assertEquals("unit1", update.objectId)
        assertEquals("armor", update.propertyName)
        assertEquals(IntValue(2), update.value)
    }
    
    @Test
    fun testModifyPropertyEffectWithValue() {
        val schema = createTestSchema()
        val targetResolver = SimpleTargetResolver()
        val engine = EffectEngine(schema, targetResolver)
        
        val unit = GameObject(
            id = "unit1",
            type = "unit",
            properties = mapOf("health" to IntValue(10))
        )
        val world = GameWorld.empty().applyUpdate(AddObjectUpdate(unit))
        
        // Effect: Set health to 5
        val effect = ModifyPropertyEffect(
            target = "self",
            property = "health",
            value = "5"
        )
        
        val updates = engine.executeEffect(effect, unit, world)
        
        assertEquals(1, updates.size)
        val update = updates[0] as UpdatePropertyUpdate
        assertEquals("unit1", update.objectId)
        assertEquals("health", update.propertyName)
        assertEquals(IntValue(5), update.value)
    }
    
    @Test
    fun testModifyPropertyEffectWithMultipleTargets() {
        val schema = createTestSchema()
        val targetResolver = SimpleTargetResolver()
        val engine = EffectEngine(schema, targetResolver)
        
        val unit1 = GameObject(
            id = "unit1",
            type = "unit",
            properties = mapOf("health" to IntValue(10))
        )
        val unit2 = GameObject(
            id = "unit2",
            type = "unit",
            properties = mapOf("health" to IntValue(8))
        )
        val building = GameObject(
            id = "building1",
            type = "building",
            properties = mapOf("health" to IntValue(20))
        )
        
        var world = GameWorld.empty()
        world = world.applyUpdate(AddObjectUpdate(unit1))
        world = world.applyUpdate(AddObjectUpdate(unit2))
        world = world.applyUpdate(AddObjectUpdate(building))
        
        // Effect: Add 3 health to all units
        val effect = ModifyPropertyEffect(
            target = "type:unit",
            property = "health",
            delta = "3"
        )
        
        val updates = engine.executeEffect(effect, unit1, world)
        
        assertEquals(2, updates.size)
        
        // Check both units got updated
        val update1 = updates.find { (it as UpdatePropertyUpdate).objectId == "unit1" } as UpdatePropertyUpdate
        assertEquals(IntValue(13), update1.value)
        
        val update2 = updates.find { (it as UpdatePropertyUpdate).objectId == "unit2" } as UpdatePropertyUpdate
        assertEquals(IntValue(11), update2.value)
    }
    
    @Test
    fun testModifyPropertyEffectWithBoolToggle() {
        val schema = createTestSchema()
        val targetResolver = SimpleTargetResolver()
        val engine = EffectEngine(schema, targetResolver)
        
        val unit = GameObject(
            id = "unit1",
            type = "unit",
            properties = mapOf("active" to BoolValue(true))
        )
        val world = GameWorld.empty().applyUpdate(AddObjectUpdate(unit))
        
        // Effect: Toggle active state
        val effect = ModifyPropertyEffect(
            target = "self",
            property = "active",
            delta = "toggle"
        )
        
        val updates = engine.executeEffect(effect, unit, world)
        
        assertEquals(1, updates.size)
        val update = updates[0] as UpdatePropertyUpdate
        assertEquals(BoolValue(false), update.value)
    }
    
    @Test
    fun testModifyPropertyEffectWithStringConcatenation() {
        val schema = createTestSchema()
        val targetResolver = SimpleTargetResolver()
        val engine = EffectEngine(schema, targetResolver)
        
        val unit = GameObject(
            id = "unit1",
            type = "unit",
            properties = mapOf("name" to StringValue("Unit"))
        )
        val world = GameWorld.empty().applyUpdate(AddObjectUpdate(unit))
        
        // Effect: Append " Elite" to name
        val effect = ModifyPropertyEffect(
            target = "self",
            property = "name",
            delta = " Elite"
        )
        
        val updates = engine.executeEffect(effect, unit, world)
        
        assertEquals(1, updates.size)
        val update = updates[0] as UpdatePropertyUpdate
        assertEquals(StringValue("Unit Elite"), update.value)
    }
}