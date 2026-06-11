package org.junction.catenin.engine

import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.ObjectInstance
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema
import kotlin.test.*

class GameInitializerTest {
    
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
        
        val cardType = ObjectTypeDefinition(
            properties = mapOf(
                "cost" to PropertyDefinition(
                    type = PropertyType.INT,
                    initial = IntValue(1)
                ),
                "name" to PropertyDefinition(
                    type = PropertyType.STRING,
                    initial = StringValue("Card")
                )
            )
        )
        
        val objectTypes = mapOf(
            "participant" to participantType,
            "card" to cardType
        )
        
        val instances = mapOf(
            "starter_deck" to ObjectInstance(
                objectType = "card",
                properties = mapOf(
                    "name" to "Starter Card",
                    "cost" to "0"
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
    fun testCreateInitialWorldWithParticipants() {
        val schema = createTestSchema()
        val config = InitializationConfig(
            participantType = "participant",
            createAllInstances = true  // Create the starter_deck instance
        )
        val initializer = GameInitializer(schema, config)
        
        val participantIds = listOf("player1", "player2")
        val world = initializer.createInitialWorld(participantIds)
        
        // Should have 2 participants + 1 starter deck instance
        assertEquals(3, world.getAllObjects().size)
        
        // Check participants were created
        val player1 = world.getObject("player1")
        assertNotNull(player1)
        assertEquals("participant", player1.type)
        assertEquals(IntValue(20), player1.getProperty("health"))
        assertEquals(IntValue(5), player1.getProperty("energy"))
        assertEquals(StringValue("waiting"), player1.getState("turn_phase"))
        
        val player2 = world.getObject("player2")
        assertNotNull(player2)
        assertEquals("participant", player2.type)
    }
    
    @Test
    fun testCreateInitialWorldWithoutParticipantType() {
        val schema = createTestSchema().withoutObjectType("participant")
        val config = InitializationConfig(
            participantType = "participant",  // Will be ignored since type doesn't exist
            createAllInstances = true
        )
        val initializer = GameInitializer(schema, config)
        
        val participantIds = listOf("player1", "player2")
        val world = initializer.createInitialWorld(participantIds)
        
        // Should only have the starter deck instance
        assertEquals(1, world.getAllObjects().size)
        
        // No participants should be created
        assertNull(world.getObject("player1"))
        assertNull(world.getObject("player2"))
    }
    
    @Test
    fun testValidateParticipantCount() {
        val schema = createTestSchema()
        val config = InitializationConfig()  // Empty config is fine for validation
        val initializer = GameInitializer(schema, config)
        
        // Valid counts (2-4)
        assertTrue(initializer.validateParticipantCount(2))
        assertTrue(initializer.validateParticipantCount(3))
        assertTrue(initializer.validateParticipantCount(4))
        
        // Invalid counts
        assertFalse(initializer.validateParticipantCount(1))
        assertFalse(initializer.validateParticipantCount(5))
        assertFalse(initializer.validateParticipantCount(0))
    }
    
    @Test
    fun testGetParticipantCounts() {
        val schema = createTestSchema()
        val config = InitializationConfig()
        val initializer = GameInitializer(schema, config)
        
        assertEquals(2, initializer.getMinParticipants())
        assertEquals(4, initializer.getMaxParticipants())
    }
    
    @Test
    fun testCreateInitialWorldWithInstances() {
        val schema = createTestSchema()
        val config = InitializationConfig(
            createAllInstances = true  // Create all instances
        )
        val initializer = GameInitializer(schema, config)
        
        val world = initializer.createInitialWorld(emptyList())
        
        // Should have the starter deck instance
        val starterCards = world.getObjectsByType("card")
        assertEquals(1, starterCards.size)
        
        val starterCard = starterCards.first()
        assertEquals(StringValue("Starter Card"), starterCard.getProperty("name"))
        assertEquals(IntValue(0), starterCard.getProperty("cost"))
    }
    
    @Test
    fun testConfigurationDrivenInitialization() {
        val schema = createTestSchema()
        
        // Test with card game configuration
        val cardGameConfig = InitializationConfig(
            participantType = "participant",
            participantIdProperty = "player_id",
            singletonObjects = listOf(
                SingletonObjectConfig("card", "deck", mapOf("name" to "Main Deck")),
                SingletonObjectConfig("card", "discard", mapOf("name" to "Discard Pile"))
            ),
            autoCreateInstances = listOf("starter_deck")
        )
        
        val initializer = GameInitializer(schema, cardGameConfig)
        val world = initializer.createInitialWorld(listOf("Alice", "Bob"))
        
        // Should have: 2 participants + 2 singleton cards + 1 instance
        assertEquals(5, world.getAllObjects().size)
        
        // Check singleton objects were created
        val deck = world.getObject("deck")
        assertNotNull(deck)
        assertEquals("card", deck.type)
        assertEquals(StringValue("Main Deck"), deck.getProperty("name"))
        
        val discard = world.getObject("discard")
        assertNotNull(discard)
        assertEquals("card", discard.type)
        assertEquals(StringValue("Discard Pile"), discard.getProperty("name"))
        
        // Check starter deck instance was created
        val starterCards = world.getObjectsByType("card").filter { 
            it.getProperty("name") == StringValue("Starter Card") 
        }
        assertEquals(1, starterCards.size)
    }
    
    @Test
    fun testNarrativeGameConfiguration() {
        val schema = createTestSchema()
        
        // Narrative game has no participants
        val narrativeConfig = InitializationConfig(
            participantType = null,  // No multiplayer
            singletonObjects = listOf(
                SingletonObjectConfig("participant", "protagonist", 
                    mapOf("health" to "30", "energy" to "10")
                )
            )
        )
        
        val initializer = GameInitializer(schema, narrativeConfig)
        val world = initializer.createInitialWorld(listOf("ignored1", "ignored2"))
        
        // Should only have the protagonist singleton
        assertEquals(1, world.getAllObjects().size)
        
        val protagonist = world.getObject("protagonist")
        assertNotNull(protagonist)
        assertEquals("participant", protagonist.type)
        assertEquals(IntValue(30), protagonist.getProperty("health"))
        assertEquals(IntValue(10), protagonist.getProperty("energy"))
    }
}