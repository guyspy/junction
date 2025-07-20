package org.junction.catenin.core.initialization

import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.*
import org.junction.catenin.model.values.*
import org.junction.catenin.core.factory.*
import org.junction.catenin.core.initialization.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameInitializerTest {
    
    private fun createTestGameDefinition(): UniversalGameDefinition {
        val participantType = ObjectTypeDefinition(
            properties = mapOf(
                "participant_id" to PropertyDefinition(PropertyType.INT, IntValue(0)),
                "name" to PropertyDefinition(PropertyType.STRING, StringValue("Player"))
            )
        )
        
        val gameControllerType = ObjectTypeDefinition(
            properties = mapOf(
                "current_turn" to PropertyDefinition(PropertyType.INT, IntValue(0)),
                "current_player" to PropertyDefinition(PropertyType.INT, IntValue(0))
            )
        )
        
        val playerStateType = ObjectTypeDefinition(
            properties = mapOf(
                "participant_id" to PropertyDefinition(PropertyType.INT, IntValue(-1)),
                "name" to PropertyDefinition(PropertyType.STRING, StringValue("Unknown")),
                "score" to PropertyDefinition(PropertyType.INT, IntValue(0))
            )
        )
        
        val handType = ObjectTypeDefinition(
            properties = mapOf(
                "owner" to PropertyDefinition(PropertyType.INT, IntValue(-1)),
                "max_size" to PropertyDefinition(PropertyType.INT, IntValue(7))
            )
        )
        
        val deckType = ObjectTypeDefinition(
            properties = mapOf(
                "owner" to PropertyDefinition(PropertyType.INT, IntValue(-1)),
                "shuffled" to PropertyDefinition(PropertyType.BOOL, BoolValue(false))
            )
        )
        
        val boardType = ObjectTypeDefinition(
            properties = mapOf(
                "width" to PropertyDefinition(PropertyType.INT, IntValue(8)),
                "height" to PropertyDefinition(PropertyType.INT, IntValue(8))
            )
        )
        
        val cardType = ObjectTypeDefinition(
            properties = mapOf(
                "name" to PropertyDefinition(PropertyType.STRING, StringValue("Basic Card")),
                "cost" to PropertyDefinition(PropertyType.INT, IntValue(1))
            )
        )
        
        val instances = mapOf(
            "fire_card" to ObjectInstance(
                objectType = "card",
                properties = mapOf(
                    "name" to "Fire Card",
                    "cost" to "3"
                )
            ),
            "water_card" to ObjectInstance(
                objectType = "card",
                properties = mapOf(
                    "name" to "Water Card",
                    "cost" to "2"
                )
            )
        )
        
        return UniversalGameDefinition(
            meta = GameMeta("Test Game", intArrayOf(8, 12), intArrayOf(2, 4)),
            objectTypes = mapOf(
                "participant" to participantType,
                "game_controller" to gameControllerType,
                "player_state" to playerStateType,
                "hand" to handType,
                "deck" to deckType,
                "board" to boardType,
                "card" to cardType
            ),
            instances = instances
        )
    }
    
    @Test
    fun testCreateInitialWorld() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        val initializer = GameInitializer(definition, factory)
        
        val world = initializer.createInitialWorld(listOf("Alice", "Bob"))
        
        // Should have multiple objects: participants + game controller + board + player states + hands + decks + instances
        assertTrue(world.getObjectCount() >= 10) // At least 10 objects expected
        
        // Check participants (only objects of type "participant")
        val participants = world.getObjectsByType("participant")
        assertEquals(2, participants.size)
        
        val alice = world.getParticipant(0)
        assertNotNull(alice)
        assertEquals("participant_0", alice.id)
        assertEquals(StringValue("Alice"), alice.getProperty("name"))
        
        val bob = world.getParticipant(1)
        assertNotNull(bob)
        assertEquals("participant_1", bob.id)
        assertEquals(StringValue("Bob"), bob.getProperty("name"))
        
        // Check game controller
        val gameController = world.getObject("game_controller")
        assertNotNull(gameController)
        assertEquals("game_controller", gameController.type)
        
        // Check board
        val board = world.getObject("main_board")
        assertNotNull(board)
        assertEquals("board", board.type)
        
        // Check player states
        val playerStates = world.getObjectsByType("player_state")
        assertEquals(2, playerStates.size)
        
        // Check hands
        val hands = world.getObjectsByType("hand")
        assertEquals(2, hands.size)
        
        // Check decks
        val decks = world.getObjectsByType("deck")
        assertEquals(2, decks.size)
        
        // Check instances
        val instances = world.getObjectsByType("card")
        assertEquals(2, instances.size)
        assertTrue(instances.any { it.getProperty("name") == StringValue("Fire Card") })
        assertTrue(instances.any { it.getProperty("name") == StringValue("Water Card") })
    }
    
    @Test
    fun testCreateInitialWorldWithoutOptionalTypes() {
        val minimalDefinition = UniversalGameDefinition(
            meta = GameMeta("Minimal Game", intArrayOf(8, 12), intArrayOf(1, 4)),
            objectTypes = mapOf(
                "participant" to ObjectTypeDefinition(
                    properties = mapOf(
                        "participant_id" to PropertyDefinition(PropertyType.INT, IntValue(0)),
                        "name" to PropertyDefinition(PropertyType.STRING, StringValue("Player"))
                    )
                )
            )
        )
        
        val factory = ObjectFactory(minimalDefinition)
        val initializer = GameInitializer(minimalDefinition, factory)
        
        val world = initializer.createInitialWorld(listOf("Alice"))
        
        // Should only have participant
        assertEquals(1, world.getObjectCount())
        assertTrue(world.hasObject("participant_0"))
        assertFalse(world.hasObject("game_controller"))
        assertFalse(world.hasObject("main_board"))
    }
    
    @Test
    fun testCreateInitialWorldWithoutParticipantType() {
        val definition = UniversalGameDefinition(
            meta = GameMeta("No Participants", intArrayOf(8, 12), intArrayOf(1, 4)),
            objectTypes = mapOf(
                "game_controller" to ObjectTypeDefinition(
                    properties = mapOf(
                        "current_turn" to PropertyDefinition(PropertyType.INT, IntValue(0))
                    )
                )
            )
        )
        
        val factory = ObjectFactory(definition)
        val initializer = GameInitializer(definition, factory)
        
        val world = initializer.createInitialWorld(listOf("Alice"))
        
        // Should have game controller but no participants
        assertEquals(1, world.getObjectCount())
        assertTrue(world.hasObject("game_controller"))
        assertEquals(0, world.getParticipants().size)
    }
    
    @Test
    fun testValidateParticipantCount() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        val initializer = GameInitializer(definition, factory)
        
        // Valid participant counts
        initializer.createInitialWorld(listOf("Alice", "Bob")) // 2 participants (within 2-4 range)
        initializer.createInitialWorld(listOf("Alice", "Bob", "Charlie", "Dave")) // 4 participants
        
        // Invalid participant counts
        assertFailsWith<IllegalArgumentException> {
            initializer.createInitialWorld(listOf("Alice")) // 1 participant (below minimum of 2)
        }
        
        assertFailsWith<IllegalArgumentException> {
            initializer.createInitialWorld(listOf("A", "B", "C", "D", "E")) // 5 participants (above maximum of 4)
        }
    }
    
    @Test
    fun testCreateObjectSet() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        val initializer = GameInitializer(definition, factory)
        
        val cards = initializer.createObjectSet(
            type = "card",
            count = 5,
            propertyOverrides = mapOf("cost" to IntValue(2))
        )
        
        assertEquals(5, cards.size)
        cards.forEach { card ->
            assertEquals("card", card.type)
            assertEquals(IntValue(2), card.getProperty("cost"))
        }
        
        // Ensure unique IDs
        val ids = cards.map { it.id }.toSet()
        assertEquals(5, ids.size)
    }
    
    @Test
    fun testCreateDeckFromInstances() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        val initializer = GameInitializer(definition, factory)
        
        val deck = initializer.createDeckFromInstances(listOf("fire_card", "water_card", "fire_card"))
        
        assertEquals(3, deck.size)
        
        val fireCards = deck.filter { it.getProperty("name") == StringValue("Fire Card") }
        val waterCards = deck.filter { it.getProperty("name") == StringValue("Water Card") }
        
        assertEquals(2, fireCards.size)
        assertEquals(1, waterCards.size)
    }
    
    @Test
    fun testCreateShuffledDeck() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        val initializer = GameInitializer(definition, factory)
        
        val deck = initializer.createShuffledDeck(
            type = "card",
            count = 10,
            propertyOverrides = mapOf("cost" to IntValue(1))
        )
        
        assertEquals(10, deck.size)
        deck.forEach { card ->
            assertEquals("card", card.type)
            assertEquals(IntValue(1), card.getProperty("cost"))
        }
        
        // Note: We can't easily test that it's actually shuffled without making assumptions
        // about the random number generator, but we can verify the deck was created correctly
    }
    
    @Test
    fun testCreateParticipantObjects() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        val initializer = GameInitializer(definition, factory)
        
        val configs = listOf(
            ParticipantObjectConfig(
                type = "player_state",
                propertyPatterns = mapOf(
                    "participant_id" to "{participant_id}",
                    "name" to "{participant_name}"
                ),
                customId = "state_{participant_id}"
            ),
            ParticipantObjectConfig(
                type = "hand",
                propertyPatterns = mapOf(
                    "owner" to "{participant_id}"
                ),
                customId = "hand_{participant_id}"
            )
        )
        
        val objects = initializer.createParticipantObjectsWithPatterns(0, "Alice", configs)
        
        assertEquals(2, objects.size)
        
        val playerState = objects.find { it.id == "state_0" }
        assertNotNull(playerState)
        assertEquals("player_state", playerState.type)
        assertEquals(IntValue(0), playerState.getProperty("participant_id"))
        assertEquals(StringValue("Alice"), playerState.getProperty("name"))
        
        val hand = objects.find { it.id == "hand_0" }
        assertNotNull(hand)
        assertEquals("hand", hand.type)
        assertEquals(IntValue(0), hand.getProperty("owner"))
    }
    
    @Test
    fun testGetSetupInfo() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        val initializer = GameInitializer(definition, factory)
        
        val setupInfo = initializer.getSetupInfo(listOf("Alice", "Bob", "Charlie"))
        
        assertEquals(3, setupInfo.participantCount)
        assertTrue(setupInfo.hasParticipantType)
        assertTrue(setupInfo.hasGameController)
        assertTrue(setupInfo.hasBoard)
        
        val expectedTypes = setOf("participant", "game_controller", "player_state", "hand", "deck", "board", "card")
        assertEquals(expectedTypes, setupInfo.availableObjectTypes.toSet())
        
        val expectedInstances = setOf("fire_card", "water_card")
        assertEquals(expectedInstances, setupInfo.availableInstances.toSet())
    }
    
    @Test
    fun testGetSetupInfoMinimal() {
        val minimalDefinition = UniversalGameDefinition(
            meta = GameMeta("Minimal", intArrayOf(8, 12), intArrayOf(1, 2)),
            objectTypes = mapOf("card" to ObjectTypeDefinition())
        )
        
        val factory = ObjectFactory(minimalDefinition)
        val initializer = GameInitializer(minimalDefinition, factory)
        
        val setupInfo = initializer.getSetupInfo(listOf("Alice"))
        
        assertEquals(1, setupInfo.participantCount)
        assertFalse(setupInfo.hasParticipantType)
        assertFalse(setupInfo.hasGameController)
        assertFalse(setupInfo.hasBoard)
        assertEquals(listOf("card"), setupInfo.availableObjectTypes)
        assertEquals(emptyList(), setupInfo.availableInstances)
    }
    
    @Test
    fun testParticipantSpecificObjectsPatternSubstitution() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        val initializer = GameInitializer(definition, factory)
        
        val world = initializer.createInitialWorld(listOf("Alice", "Bob"))
        
        // Check that participant-specific objects have correct pattern substitutions
        val playerStates = world.getObjectsByType("player_state")
        assertEquals(2, playerStates.size)
        
        val aliceState = playerStates.find { it.getProperty("participant_id") == IntValue(0) }
        assertNotNull(aliceState)
        assertEquals(StringValue("Alice"), aliceState.getProperty("name"))
        assertEquals("player_state_0", aliceState.id)
        
        val bobState = playerStates.find { it.getProperty("participant_id") == IntValue(1) }
        assertNotNull(bobState)
        assertEquals(StringValue("Bob"), bobState.getProperty("name"))
        assertEquals("player_state_1", bobState.id)
        
        // Check hands
        val hands = world.getObjectsByType("hand")
        assertEquals(2, hands.size)
        
        val aliceHand = hands.find { it.getProperty("owner") == IntValue(0) }
        assertNotNull(aliceHand)
        assertEquals("hand_0", aliceHand.id)
        
        val bobHand = hands.find { it.getProperty("owner") == IntValue(1) }
        assertNotNull(bobHand)
        assertEquals("hand_1", bobHand.id)
    }
    
    @Test
    fun testCreateDeckFromInvalidInstance() {
        val definition = createTestGameDefinition()
        val factory = ObjectFactory(definition)
        val initializer = GameInitializer(definition, factory)
        
        assertFailsWith<IllegalArgumentException> {
            initializer.createDeckFromInstances(listOf("nonexistent_instance"))
        }
    }
}