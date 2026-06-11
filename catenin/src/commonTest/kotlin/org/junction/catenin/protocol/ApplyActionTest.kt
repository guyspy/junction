package org.junction.catenin.protocol

import org.junction.catenin.core.*
import org.junction.catenin.engine.*
import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.ObjectInstance
import org.junction.catenin.model.triggers.*
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.test.*

class ApplyActionTest {

    private fun createSchemaWithTrigger(): UniversalGameSchema {
        val meta = GameMeta(
            name = "Test Game",
            targetAge = intArrayOf(8, 12),
            participantCount = intArrayOf(2, 2)
        )

        val participantType = ObjectTypeDefinition(
            properties = mapOf(
                "health" to PropertyDefinition(type = PropertyType.INT, initial = IntValue(20)),
                "energy" to PropertyDefinition(type = PropertyType.INT, initial = IntValue(5))
            )
        )

        val unitType = ObjectTypeDefinition(
            properties = mapOf(
                "health" to PropertyDefinition(type = PropertyType.INT, initial = IntValue(10)),
                "armor" to PropertyDefinition(type = PropertyType.INT, initial = IntValue(0)),
                "owner" to PropertyDefinition(type = PropertyType.OBJECT_REF, initial = ObjectRefValue(""))
            )
        )

        // Trigger: When unit health changes, gain 2 armor
        val healthChangeTrigger = TriggerDefinition(
            name = "health_change_armor",
            `when` = TriggerCondition(
                objectType = "unit",
                propertyChanged = "health"
            ),
            effects = listOf(
                ModifyPropertyEffect(target = "self", property = "armor", delta = "2")
            )
        )

        return UniversalGameSchema(
            meta = meta,
            objectTypes = mapOf("participant" to participantType, "unit" to unitType),
            instances = emptyMap(),
            triggers = listOf(healthChangeTrigger),
            initialization = InitializationConfig(participantType = "participant")
        )
    }

    private fun createSchemaWithoutTriggers(): UniversalGameSchema {
        val meta = GameMeta(
            name = "Simple Game",
            targetAge = intArrayOf(8, 12),
            participantCount = intArrayOf(2, 2)
        )

        val participantType = ObjectTypeDefinition(
            properties = mapOf(
                "health" to PropertyDefinition(type = PropertyType.INT, initial = IntValue(20))
            )
        )

        return UniversalGameSchema(
            meta = meta,
            objectTypes = mapOf("participant" to participantType),
            instances = emptyMap(),
            triggers = emptyList(),
            initialization = InitializationConfig(participantType = "participant")
        )
    }

    @Test
    fun testApplyActionSimpleUpdate() {
        val schema = createSchemaWithoutTriggers()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))

        val action = PlayerAction(
            type = "update_property",
            sourceId = "player1",
            targetId = "player1",
            metadata = mapOf("property" to "health", "value" to "15")
        )

        val block = engine.applyAction(action)

        assertEquals(BlockType.ACTION, block.type)
        assertEquals("player1", block.sourceId)
        assertTrue(block.updates.isNotEmpty())
        // No triggers means no children
        assertTrue(block.children.isEmpty())
    }

    @Test
    fun testApplyActionWithTrigger() {
        val schema = createSchemaWithTrigger()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))

        // Add a unit
        val unit = engine.createObject("unit", id = "unit1")
        engine.addObject(unit)

        val action = PlayerAction(
            type = "update_property",
            sourceId = "player1",
            targetId = "unit1",
            metadata = mapOf("property" to "health", "value" to "5")
        )

        val block = engine.applyAction(action)

        assertEquals(BlockType.ACTION, block.type)
        assertEquals("player1", block.sourceId)
        // The action update itself
        assertTrue(block.updates.isNotEmpty())
        // Should have trigger children from the health change
        assertTrue(block.children.isNotEmpty())
        assertEquals(BlockType.TRIGGER, block.children[0].type)
    }

    @Test
    fun testApplyActionWorldStateUpdated() {
        val schema = createSchemaWithoutTriggers()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))

        val action = PlayerAction(
            type = "update_property",
            sourceId = "player1",
            targetId = "player1",
            metadata = mapOf("property" to "health", "value" to "15")
        )

        engine.applyAction(action)

        // World should be updated
        val player = engine.getWorld().getObject("player1")
        assertNotNull(player)
        assertEquals(IntValue(15), player.getProperty("health"))
    }

    @Test
    fun testApplyActionFlattenMatchesWorldState() {
        val schema = createSchemaWithTrigger()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))

        val unit = engine.createObject("unit", id = "unit1")
        engine.addObject(unit)

        val action = PlayerAction(
            type = "update_property",
            sourceId = "player1",
            targetId = "unit1",
            metadata = mapOf("property" to "health", "value" to "5")
        )

        val block = engine.applyAction(action)

        // Flattened updates should account for all state changes
        val allUpdates = block.flattenUpdates()
        assertTrue(allUpdates.isNotEmpty())

        // The world should reflect all updates
        val updatedUnit = engine.getWorld().getObject("unit1")
        assertNotNull(updatedUnit)
        assertEquals(IntValue(5), updatedUnit.getProperty("health"))
        assertEquals(IntValue(2), updatedUnit.getProperty("armor")) // From trigger
    }

    @Test
    fun testApplyActionEndTurn() {
        val schema = createSchemaWithoutTriggers()
        val engine = GameEngine.fromSchema(schema, schema.initialization)
            .initializeGame(listOf("player1", "player2"))

        val action = PlayerAction(
            type = "end_turn",
            sourceId = "player1",
            targetId = null,
            metadata = emptyMap()
        )

        val block = engine.applyAction(action)

        assertEquals(BlockType.ACTION, block.type)
        assertEquals("player1", block.sourceId)
        // end_turn with no effects produces empty updates
        assertTrue(block.updates.isEmpty())
        assertTrue(block.children.isEmpty())
    }
}
