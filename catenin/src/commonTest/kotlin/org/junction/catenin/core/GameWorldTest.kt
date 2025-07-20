package org.junction.catenin.core

import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameWorldTest {
    
    private fun createTestObject(id: String, type: String): GameObject {
        return GameObject(
            id = id,
            type = type,
            properties = mapOf(
                "health" to IntValue(100),
                "name" to StringValue("Test $type")
            ),
            states = mapOf(
                "activated" to BoolValue(false)
            )
        )
    }
    
    @Test
    fun testEmptyWorld() {
        val world = GameWorld.empty()
        
        assertTrue(world.isEmpty())
        assertEquals(0, world.getObjectCount())
        assertEquals(emptyList(), world.getAllObjects())
        assertEquals(emptyList(), world.getAllObjectIds())
    }
    
    @Test
    fun testWithObject() {
        val world = GameWorld.empty()
        val creature = createTestObject("creature_1", "creature")
        
        val newWorld = world.withObject(creature)
        
        // Original world unchanged
        assertTrue(world.isEmpty())
        assertFalse(world.hasObject("creature_1"))
        
        // New world has the object
        assertFalse(newWorld.isEmpty())
        assertTrue(newWorld.hasObject("creature_1"))
        assertEquals(1, newWorld.getObjectCount())
        assertEquals(creature, newWorld.getObject("creature_1"))
    }
    
    @Test
    fun testWithObjects() {
        val world = GameWorld.empty()
        val objects = listOf(
            createTestObject("creature_1", "creature"),
            createTestObject("spell_1", "spell"),
            createTestObject("artifact_1", "artifact")
        )
        
        val newWorld = world.withObjects(objects)
        
        assertEquals(3, newWorld.getObjectCount())
        assertTrue(newWorld.hasObject("creature_1"))
        assertTrue(newWorld.hasObject("spell_1"))
        assertTrue(newWorld.hasObject("artifact_1"))
    }
    
    @Test
    fun testWithoutObject() {
        val creature = createTestObject("creature_1", "creature")
        val spell = createTestObject("spell_1", "spell")
        val world = GameWorld.empty().withObjects(listOf(creature, spell))
        
        val newWorld = world.withoutObject("creature_1")
        
        // Original world unchanged
        assertEquals(2, world.getObjectCount())
        assertTrue(world.hasObject("creature_1"))
        
        // New world has object removed
        assertEquals(1, newWorld.getObjectCount())
        assertFalse(newWorld.hasObject("creature_1"))
        assertTrue(newWorld.hasObject("spell_1"))
    }
    
    @Test
    fun testWithoutObjects() {
        val objects = listOf(
            createTestObject("creature_1", "creature"),
            createTestObject("spell_1", "spell"),
            createTestObject("artifact_1", "artifact")
        )
        val world = GameWorld.empty().withObjects(objects)
        
        val newWorld = world.withoutObjects(listOf("creature_1", "spell_1"))
        
        assertEquals(1, newWorld.getObjectCount())
        assertFalse(newWorld.hasObject("creature_1"))
        assertFalse(newWorld.hasObject("spell_1"))
        assertTrue(newWorld.hasObject("artifact_1"))
    }
    
    @Test
    fun testUpdateObject() {
        val creature = createTestObject("creature_1", "creature")
        val world = GameWorld.empty().withObject(creature)
        
        val updatedCreature = creature.withProperty("health", IntValue(50))
        val newWorld = world.updateObject("creature_1", updatedCreature)
        
        // Original world unchanged
        val originalCreature = world.getObject("creature_1")
        assertNotNull(originalCreature, "Original creature should exist")
        assertEquals(IntValue(100), originalCreature.getProperty("health"))
        
        // New world has updated object
        val updatedCreatureInNewWorld = newWorld.getObject("creature_1")
        assertNotNull(updatedCreatureInNewWorld, "Updated creature should exist in new world")
        assertEquals(IntValue(50), updatedCreatureInNewWorld.getProperty("health"))
    }
    
    @Test
    fun testUpdateNonexistentObject() {
        val world = GameWorld.empty()
        val creature = createTestObject("creature_1", "creature")
        
        assertFailsWith<IllegalArgumentException> {
            world.updateObject("creature_1", creature)
        }
    }
    
    @Test
    fun testUpdateObjectProperty() {
        val creature = createTestObject("creature_1", "creature")
        val world = GameWorld.empty().withObject(creature)
        
        val newWorld = world.updateObjectProperty("creature_1", "health", IntValue(75))
        
        val originalCreature = world.getObject("creature_1")
        assertNotNull(originalCreature, "Original creature should exist in world")
        assertEquals(IntValue(100), originalCreature.getProperty("health"))
        
        val updatedCreature = newWorld.getObject("creature_1")
        assertNotNull(updatedCreature, "Updated creature should exist in new world")
        assertEquals(IntValue(75), updatedCreature.getProperty("health"))
    }
    
    @Test
    fun testUpdateObjectState() {
        val creature = createTestObject("creature_1", "creature")
        val world = GameWorld.empty().withObject(creature)
        
        val newWorld = world.updateObjectState("creature_1", "activated", BoolValue(true))
        
        val originalCreature = world.getObject("creature_1")
        assertNotNull(originalCreature, "Original creature should exist in world")
        assertEquals(BoolValue(false), originalCreature.getState("activated"))
        
        val updatedCreature = newWorld.getObject("creature_1")
        assertNotNull(updatedCreature, "Updated creature should exist in new world")
        assertEquals(BoolValue(true), updatedCreature.getState("activated"))
    }
    
    @Test
    fun testUpdateNonexistentObjectProperty() {
        val world = GameWorld.empty()
        
        assertFailsWith<IllegalArgumentException> {
            world.updateObjectProperty("nonexistent", "health", IntValue(50))
        }
    }
    
    @Test
    fun testGetObject() {
        val creature = createTestObject("creature_1", "creature")
        val world = GameWorld.empty().withObject(creature)
        
        assertEquals(creature, world.getObject("creature_1"))
        assertNull(world.getObject("nonexistent"))
    }
    
    @Test
    fun testRequireObject() {
        val creature = createTestObject("creature_1", "creature")
        val world = GameWorld.empty().withObject(creature)
        
        assertEquals(creature, world.requireObject("creature_1"))
        
        assertFailsWith<IllegalArgumentException> {
            world.requireObject("nonexistent")
        }
    }
    
    @Test
    fun testGetObjectsByType() {
        val creatures = listOf(
            createTestObject("creature_1", "creature"),
            createTestObject("creature_2", "creature")
        )
        val spell = createTestObject("spell_1", "spell")
        val world = GameWorld.empty().withObjects(creatures + spell)
        
        val foundCreatures = world.getObjectsByType("creature")
        assertEquals(2, foundCreatures.size)
        assertTrue(foundCreatures.all { it.type == "creature" })
        
        val foundSpells = world.getObjectsByType("spell")
        assertEquals(1, foundSpells.size)
        assertEquals("spell", foundSpells[0].type)
        
        val foundArtifacts = world.getObjectsByType("artifact")
        assertEquals(0, foundArtifacts.size)
    }
    
    @Test
    fun testGetObjectsByProperty() {
        val lowHealthCreature = createTestObject("creature_1", "creature")
            .withProperty("health", IntValue(25))
        val highHealthCreature = createTestObject("creature_2", "creature")
            .withProperty("health", IntValue(100))
        val world = GameWorld.empty().withObjects(listOf(lowHealthCreature, highHealthCreature))
        
        val lowHealth = world.getObjectsByProperty("health", IntValue(25))
        assertEquals(1, lowHealth.size)
        assertEquals("creature_1", lowHealth[0].id)
        
        val highHealth = world.getObjectsByProperty("health", IntValue(100))
        assertEquals(1, highHealth.size)
        assertEquals("creature_2", highHealth[0].id)
    }
    
    @Test
    fun testGetObjectsByState() {
        val activeCreature = createTestObject("creature_1", "creature")
            .withState("activated", BoolValue(true))
        val inactiveCreature = createTestObject("creature_2", "creature")
            .withState("activated", BoolValue(false))
        val world = GameWorld.empty().withObjects(listOf(activeCreature, inactiveCreature))
        
        val active = world.getObjectsByState("activated", BoolValue(true))
        assertEquals(1, active.size)
        assertEquals("creature_1", active[0].id)
        
        val inactive = world.getObjectsByState("activated", BoolValue(false))
        assertEquals(1, inactive.size)
        assertEquals("creature_2", inactive[0].id)
    }
    
    @Test
    fun testFindObjects() {
        val creatures = listOf(
            createTestObject("creature_1", "creature").withProperty("health", IntValue(25)),
            createTestObject("creature_2", "creature").withProperty("health", IntValue(100)),
            createTestObject("spell_1", "spell").withProperty("health", IntValue(50))
        )
        val world = GameWorld.empty().withObjects(creatures)
        
        val lowHealthObjects = world.findObjects { obj ->
            val health = obj.getProperty("health") as? IntValue
            health != null && health.value < 50
        }
        
        assertEquals(1, lowHealthObjects.size)
        assertEquals("creature_1", lowHealthObjects[0].id)
    }
    
    @Test
    fun testFindObject() {
        val creatures = listOf(
            createTestObject("creature_1", "creature"),
            createTestObject("creature_2", "creature")
        )
        val world = GameWorld.empty().withObjects(creatures)
        
        val found = world.findObject { it.id == "creature_2" }
        assertNotNull(found)
        assertEquals("creature_2", found.id)
        
        val notFound = world.findObject { it.id == "nonexistent" }
        assertNull(notFound)
    }
    
    @Test
    fun testGetObjectCount() {
        val world = GameWorld.empty()
        assertEquals(0, world.getObjectCount())
        
        val worldWithObjects = world.withObjects(listOf(
            createTestObject("creature_1", "creature"),
            createTestObject("spell_1", "spell")
        ))
        assertEquals(2, worldWithObjects.getObjectCount())
    }
    
    @Test
    fun testGetObjectCountByType() {
        val objects = listOf(
            createTestObject("creature_1", "creature"),
            createTestObject("creature_2", "creature"),
            createTestObject("spell_1", "spell")
        )
        val world = GameWorld.empty().withObjects(objects)
        
        assertEquals(2, world.getObjectCountByType("creature"))
        assertEquals(1, world.getObjectCountByType("spell"))
        assertEquals(0, world.getObjectCountByType("artifact"))
    }
    
    @Test
    fun testGetObjectsWithProperty() {
        val creatureWithMana = createTestObject("creature_1", "creature")
            .withProperty("mana", IntValue(50))
        val creatureWithoutMana = createTestObject("creature_2", "creature")
        val world = GameWorld.empty().withObjects(listOf(creatureWithMana, creatureWithoutMana))
        
        val withMana = world.getObjectsWithProperty("mana")
        assertEquals(1, withMana.size)
        assertEquals("creature_1", withMana[0].id)
        
        val withHealth = world.getObjectsWithProperty("health")
        assertEquals(2, withHealth.size) // Both have health
    }
    
    @Test
    fun testGetObjectsWithState() {
        val creatureWithPosition = createTestObject("creature_1", "creature")
            .withState("position", StringValue("front"))
        val creatureWithoutPosition = createTestObject("creature_2", "creature")
        val world = GameWorld.empty().withObjects(listOf(creatureWithPosition, creatureWithoutPosition))
        
        val withPosition = world.getObjectsWithState("position")
        assertEquals(1, withPosition.size)
        assertEquals("creature_1", withPosition[0].id)
        
        val withActivated = world.getObjectsWithState("activated")
        assertEquals(2, withActivated.size) // Both have activated state
    }
    
    @Test
    fun testGetParticipants() {
        val participant1 = createTestObject("participant_1", "participant")
            .withProperty("participant_id", IntValue(0))
        val participant2 = createTestObject("participant_2", "participant")
            .withProperty("participant_id", IntValue(1))
        val creature = createTestObject("creature_1", "creature")
        val world = GameWorld.empty().withObjects(listOf(participant1, participant2, creature))
        
        val participants = world.getParticipants()
        assertEquals(2, participants.size)
        assertTrue(participants.all { it.hasProperty("participant_id") })
    }
    
    @Test
    fun testGetParticipant() {
        val participant1 = createTestObject("participant_1", "participant")
            .withProperty("participant_id", IntValue(0))
        val participant2 = createTestObject("participant_2", "participant")
            .withProperty("participant_id", IntValue(1))
        val world = GameWorld.empty().withObjects(listOf(participant1, participant2))
        
        val found0 = world.getParticipant(0)
        assertNotNull(found0)
        assertEquals("participant_1", found0.id)
        
        val found1 = world.getParticipant(1)
        assertNotNull(found1)
        assertEquals("participant_2", found1.id)
        
        val notFound = world.getParticipant(2)
        assertNull(notFound)
    }
    
    @Test
    fun testApplyUpdates() {
        val creature = createTestObject("creature_1", "creature")
        val world = GameWorld.empty().withObject(creature)
        
        val updates = listOf(
            UpdatePropertyUpdate("creature_1", "health", IntValue(50)),
            AddObjectUpdate(createTestObject("spell_1", "spell")),
            UpdateStateUpdate("creature_1", "activated", BoolValue(true))
        )
        
        val newWorld = world.applyUpdates(updates)
        
        // Original world unchanged
        assertEquals(1, world.getObjectCount())
        val originalCreature = world.getObject("creature_1")
        assertNotNull(originalCreature, "Original creature should exist in world")
        assertEquals(IntValue(100), originalCreature.getProperty("health"))
        
        // New world has all updates applied
        assertEquals(2, newWorld.getObjectCount())
        val finalCreature = newWorld.getObject("creature_1")
        assertNotNull(finalCreature, "Final creature should exist in new world")
        assertEquals(IntValue(50), finalCreature.getProperty("health"))
        assertEquals(BoolValue(true), finalCreature.getState("activated"))
        assertTrue(newWorld.hasObject("spell_1"))
    }
    
    @Test
    fun testApplyUpdatesWithRemoval() {
        val objects = listOf(
            createTestObject("creature_1", "creature"),
            createTestObject("spell_1", "spell")
        )
        val world = GameWorld.empty().withObjects(objects)
        
        val updates = listOf(
            RemoveObjectUpdate("creature_1"),
            AddObjectUpdate(createTestObject("artifact_1", "artifact"))
        )
        
        val newWorld = world.applyUpdates(updates)
        
        assertEquals(2, newWorld.getObjectCount())
        assertFalse(newWorld.hasObject("creature_1"))
        assertTrue(newWorld.hasObject("spell_1"))
        assertTrue(newWorld.hasObject("artifact_1"))
    }
    
    @Test
    fun testCreateSnapshot() {
        val objects = listOf(
            createTestObject("creature_1", "creature"),
            createTestObject("creature_2", "creature"),
            createTestObject("spell_1", "spell")
        )
        val world = GameWorld.empty().withObjects(objects)
        
        val snapshot = world.createSnapshot()
        
        assertEquals(3, snapshot.objectCount)
        assertEquals(2, snapshot.objectsByType["creature"])
        assertEquals(1, snapshot.objectsByType["spell"])
        assertEquals(listOf("creature_1", "creature_2", "spell_1"), snapshot.objectIds)
    }
    
    @Test
    fun testWithObjectsConstructor() {
        val objects = listOf(
            createTestObject("creature_1", "creature"),
            createTestObject("spell_1", "spell")
        )
        
        val world = GameWorld.withObjects(objects)
        
        assertEquals(2, world.getObjectCount())
        assertTrue(world.hasObject("creature_1"))
        assertTrue(world.hasObject("spell_1"))
    }
    
    @Test
    fun testUpdateObjectWithIdMismatch() {
        val creature = createTestObject("creature_1", "creature")
        val world = GameWorld.empty().withObject(creature)
        
        val differentIdObject = createTestObject("different_id", "creature")
        val newWorld = world.updateObject("creature_1", differentIdObject)
        
        // Should correct the ID to match the key
        val updated = newWorld.getObject("creature_1")
        assertNotNull(updated)
        assertEquals("creature_1", updated.id)
    }
}