package org.junction.catenin.engine

import org.junction.catenin.core.*
import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.ObjectInstance
import org.junction.catenin.model.triggers.*
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.test.*

class GameEngineTest {
    
    private fun createTestSchema(): UniversalGameSchema {
        val meta = GameMeta(
            name = "Test Game",
            targetAge = intArrayOf(8, 12),
            participantCount = intArrayOf(2, 4)
        )
        
        val participantType = ObjectTypeDefinition(
            properties = mapOf(
                "health" to PropertyDefinition(
                    type = PropertyType.INT,
                    initial = IntValue(20)
                ),
                "energy" to PropertyDefinition(
                    type = PropertyType.INT,
                    initial = IntValue(5)
                )
            ),
            states = mapOf(
                "turn_phase" to PropertyDefinition(
                    type = PropertyType.STRING,
                    initial = StringValue("waiting")
                )
            )
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
                "owner" to PropertyDefinition(
                    type = PropertyType.OBJECT_REF,
                    initial = ObjectRefValue("")
                )
            )
        )
        
        // Trigger: When unit health drops below 5, gain 2 armor
        val lowHealthTrigger = TriggerDefinition(
            name = "low_health_armor",
            `when` = TriggerCondition(
                objectType = "unit",
                propertyChanged = "health"
            ),
            effects = listOf(
                ModifyPropertyEffect(
                    target = "self",
                    property = "armor",
                    delta = "2"
                )
            )
        )
        
        val objectTypes = mapOf(
            "participant" to participantType,
            "unit" to unitType
        )
        
        val instances = mapOf(
            "starter_unit" to ObjectInstance(
                objectType = "unit",
                properties = mapOf(
                    "health" to "15"
                )
            )
        )
        
        val initConfig = InitializationConfig(
            participantType = "participant",
            createAllInstances = true
        )
        
        return UniversalGameSchema(
            meta = meta,
            objectTypes = objectTypes,
            instances = instances,
            triggers = listOf(lowHealthTrigger),
            initialization = initConfig
        )
    }
    
    @Test
    fun testCreateGameEngine() {
        val schema = createTestSchema()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
        
        assertNotNull(engine)
        assertEquals(schema, engine.getSchema())
        assertEquals(2, engine.getMinParticipants())
        assertEquals(4, engine.getMaxParticipants())
    }
    
    @Test
    fun testInitializeGame() {
        val schema = createTestSchema()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
        
        val participantIds = listOf("player1", "player2")
        engine.initializeGame(participantIds)
        
        val world = engine.getWorld()
        
        // Should have 2 participants + 1 starter unit
        assertEquals(3, world.getAllObjects().size)
        
        // Check participants exist
        assertNotNull(world.getObject("player1"))
        assertNotNull(world.getObject("player2"))
        
        // Check starter unit exists
        val units = world.getObjectsByType("unit")
        assertEquals(1, units.size)
        assertEquals(IntValue(15), units[0].getProperty("health"))
    }
    
    @Test
    fun testInitializeGameInvalidParticipantCount() {
        val schema = createTestSchema()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
        
        // Too few participants
        assertFailsWith<IllegalArgumentException> {
            engine.initializeGame(listOf("player1"))
        }
        
        // Too many participants
        assertFailsWith<IllegalArgumentException> {
            engine.initializeGame(listOf("p1", "p2", "p3", "p4", "p5"))
        }
    }
    
    @Test
    fun testCreateAndAddObject() {
        val schema = createTestSchema()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))
        
        // Create a new unit
        val unit = engine.createObject(
            objectType = "unit",
            propertyOverrides = mapOf(
                "health" to "8",
                "owner" to "player1"
            )
        )
        
        engine.addObject(unit)
        
        val world = engine.getWorld()
        assertNotNull(world.getObject(unit.id))
        assertEquals(IntValue(8), world.getObject(unit.id)?.getProperty("health"))
    }
    
    @Test
    fun testUpdatePropertyWithTrigger() {
        val schema = createTestSchema()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))
        
        // Create a unit
        val unit = engine.createObject("unit")
        engine.addObject(unit)
        
        // Update health to 4 (should trigger armor bonus)
        engine.updateProperty(unit.id, "health", IntValue(4))
        
        val world = engine.getWorld()
        val updatedUnit = world.getObject(unit.id)
        assertNotNull(updatedUnit)
        assertEquals(IntValue(4), updatedUnit.getProperty("health"))
        assertEquals(IntValue(2), updatedUnit.getProperty("armor")) // Triggered effect
    }
    
    @Test
    fun testRemoveObject() {
        val schema = createTestSchema()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))
        
        val unit = engine.createObject("unit")
        engine.addObject(unit)
        
        // Verify it exists
        assertNotNull(engine.getWorld().getObject(unit.id))
        
        // Remove it
        engine.removeObject(unit.id)
        
        // Verify it's gone
        assertNull(engine.getWorld().getObject(unit.id))
    }
    
    @Test
    fun testUpdateState() {
        val schema = createTestSchema()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))
        
        engine.updateState("player1", "turn_phase", StringValue("active"))
        
        val world = engine.getWorld()
        val player = world.getObject("player1")
        assertNotNull(player)
        assertEquals(StringValue("active"), player.getState("turn_phase"))
    }
    
    @Test
    fun testApplyMultipleUpdates() {
        val schema = createTestSchema()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))
        
        val unit1 = engine.createObject("unit", id = "unit1")
        val unit2 = engine.createObject("unit", id = "unit2")
        
        val updates = listOf(
            AddObjectUpdate(unit1),
            AddObjectUpdate(unit2),
            UpdatePropertyUpdate("unit1", "health", IntValue(15)),
            UpdatePropertyUpdate("unit2", "health", IntValue(12))
        )
        
        engine.applyUpdates(updates)
        
        val world = engine.getWorld()
        assertEquals(IntValue(15), world.getObject("unit1")?.getProperty("health"))
        assertEquals(IntValue(12), world.getObject("unit2")?.getProperty("health"))
    }
    
    @Test
    fun testCreateFromInstance() {
        val schema = createTestSchema()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
        
        val starterUnit = engine.createFromInstance("starter_unit")
        engine.addObject(starterUnit)
        
        val world = engine.getWorld()
        val unit = world.getObject(starterUnit.id)
        assertNotNull(unit)
        assertEquals("unit", unit.type)
        assertEquals(IntValue(15), unit.getProperty("health"))
    }
    
    @Test
    fun testMethodChaining() {
        val schema = createTestSchema()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))
        
        val unit = engine.createObject("unit", id = "unit1")
        
        // Method chaining should work
        val result = engine
            .addObject(unit)
            .updateProperty("unit1", "health", IntValue(8))
            .updateState("player1", "turn_phase", StringValue("active"))
            .removeObject("player2")
        
        // Result should be the same engine instance
        assertEquals(engine, result)
        
        // Verify all changes applied
        val world = engine.getWorld()
        assertEquals(IntValue(8), world.getObject("unit1")?.getProperty("health"))
        assertEquals(StringValue("active"), world.getObject("player1")?.getState("turn_phase"))
        assertNull(world.getObject("player2"))
    }
}