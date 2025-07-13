package org.junction.catenin.js

import org.junction.catenin.core.createGameEngineFromYaml
import org.junction.catenin.model.CardFactory
import org.junction.catenin.parser.GameDefinitionParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * JavaScript Library API Test
 * 
 * Tests the exact JavaScript library functions used in the HTML demo to ensure:
 * - Proper JavaScript exports work
 * - Array return types for JavaScript compatibility
 * - Complete workflow scenarios
 * - Error handling
 */
class JavaScriptLibraryTest {
    
    private val validGameYaml = """
        meta:
          name: "Browser Demo Game"
          target_age: [8, 12]
          player_count: [2, 2]

        cards:
          attack_card:
            count: 8
            properties:
              damage: {type: int, min: 2, max: 5}
              element: {type: enum, values: [fire, water, earth]}

        mechanics:
          setup:
            players:
              health: 15
              hand_size: 4
          win_conditions:
            - type: "health_depleted"
              message: "{winner} wins!"
    """.trimIndent()
    
    @Test
    fun testCreateGameEngineFromYamlBasic() {
        // Test the factory function used in HTML demo
        val engine = createGameEngineFromYaml(validGameYaml, arrayOf("Alice", "Bob"))
        
        assertNotNull(engine)
        val definition = engine.getGameDefinition()
        assertEquals("Browser Demo Game", definition.meta.name)
    }
    
    @Test
    fun testCreateGameEngineFromYamlWithArrayParams() {
        // Test that it accepts Array<String> as expected in JavaScript
        val playerNames = arrayOf("Player1", "Player2", "Player3")
        val engine = createGameEngineFromYaml(validGameYaml, playerNames)
        
        val players = engine.getPlayers()
        assertEquals(3, players.size)
        assertEquals("Player1", players[0].name)
        assertEquals("Player2", players[1].name)
        assertEquals("Player3", players[2].name)
    }
    
    @Test
    fun testCreateGameEngineFromYamlInvalidYaml() {
        // Test error handling
        val invalidYaml = "invalid: yaml: structure:"
        
        assertFailsWith<Exception> {
            createGameEngineFromYaml(invalidYaml, arrayOf("Alice", "Bob"))
        }
    }
    
    @Test
    fun testGameDefinitionParserBasic() {
        // Test the parser used in HTML demo
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(validGameYaml)
        
        assertNotNull(definition)
        assertEquals("Browser Demo Game", definition.meta.name)
        assertEquals(1, definition.cards.size)
        assertTrue(definition.cards.containsKey("attack_card"))
    }
    
    @Test
    fun testGameDefinitionParserErrorHandling() {
        val parser = GameDefinitionParser()
        
        assertFailsWith<Exception> {
            parser.parseFromString("invalid yaml")
        }
        
        assertFailsWith<Exception> {
            parser.parseFromString("")
        }
    }
    
    @Test
    fun testCardFactoryReturnsJavaScriptArray() {
        // Test that CardFactory.generateCards() returns Array, not List
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(validGameYaml)
        val cardFactory = CardFactory.fromDefinition(definition)
        
        val cards = cardFactory.generateCards()
        
        // Verify it's an Array (has size property and indexing)
        assertEquals(8, cards.size)  // 8 attack cards as defined in YAML
        
        // Verify each card has expected properties
        cards.forEach { card ->
            assertEquals("attack_card", card.type)
            assertTrue(card.id.startsWith("attack_card_"))
            
            val damage = card.getIntProperty("damage")
            assertNotNull(damage)
            assertTrue(damage in 2..5)
            
            val element = card.getStringProperty("element")
            assertNotNull(element)
            assertTrue(element in listOf("fire", "water", "earth"))
        }
    }
    
    @Test
    fun testPlayersArrayReturnsJavaScriptArray() {
        // Test that GameEngine.getPlayers() returns Array, not List
        val engine = createGameEngineFromYaml(validGameYaml, arrayOf("Alice", "Bob"))
        val players = engine.getPlayers()
        
        // Verify it's an Array
        assertEquals(2, players.size)
        assertEquals("Alice", players[0].name)
        assertEquals("Bob", players[1].name)
        
        // Test that players have expected health from YAML
        assertEquals(15, players[0].health)  // From mechanics.setup.players.health
        assertEquals(15, players[1].health)
        
        // Test player IDs are generated correctly
        assertEquals("player_0", players[0].id)
        assertEquals("player_1", players[1].id)
    }
    
    @Test
    fun testJavaScriptArrayMethodsOnPlayers() {
        // Test that returned arrays can use JavaScript Array methods
        val engine = createGameEngineFromYaml(validGameYaml, arrayOf("Alice", "Bob", "Charlie"))
        val players = engine.getPlayers()
        
        // Test Array.size property
        assertEquals(3, players.size)
        
        // Test Array indexing
        assertEquals("Alice", players[0].name)
        assertEquals("Bob", players[1].name)
        assertEquals("Charlie", players[2].name)
        
        // Test that we can slice the array (JavaScript compatibility)
        val firstTwoPlayers = players.sliceArray(0..1)
        assertEquals(2, firstTwoPlayers.size)
        assertEquals("Alice", firstTwoPlayers[0].name)
        assertEquals("Bob", firstTwoPlayers[1].name)
    }
    
    @Test
    fun testJavaScriptArrayMethodsOnCards() {
        // Test that card arrays support JavaScript Array operations
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(validGameYaml)
        val cardFactory = CardFactory.fromDefinition(definition)
        val cards = cardFactory.generateCards()
        
        // Test Array.size
        assertEquals(8, cards.size)
        
        // Test Array indexing
        assertEquals("attack_card", cards[0].type)
        
        // Test slicing (like cards.slice(0, 5) in JavaScript)
        val firstFiveCards = cards.sliceArray(0..4)
        assertEquals(5, firstFiveCards.size)
        firstFiveCards.forEach { card ->
            assertEquals("attack_card", card.type)
        }
        
        // Test that we can access card properties
        val firstCard = cards[0]
        assertNotNull(firstCard.getIntProperty("damage"))
        assertNotNull(firstCard.getStringProperty("element"))
    }
    
    @Test
    fun testCompleteHTMLDemoWorkflow() {
        // Test the exact workflow used in the HTML demo
        val yamlInput = validGameYaml
        val playerNames = arrayOf("Alice", "Bob")
        
        // Step 1: Parse YAML and create game engine (like loadGame() function)
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(yamlInput)
        val engine = createGameEngineFromYaml(yamlInput, playerNames)
        
        // Step 2: Verify game info display
        assertEquals("Browser Demo Game", definition.meta.name)
        val players = engine.getPlayers()
        assertEquals(2, players.size)
        assertEquals("Alice", players[0].name)
        assertEquals("Bob", players[1].name)
        
        // Step 3: Test showing players (like showPlayers() function)
        players.forEach { player ->
            assertNotNull(player.name)
            assertEquals(15, player.health)
            assertEquals(4, player.hand.size)  // Players dealt 4 cards as per hand_size: 4
        }
        
        // Step 4: Test generating cards (like generateCards() function)
        val cardFactory = CardFactory.fromDefinition(definition)
        val cards = cardFactory.generateCards()
        assertEquals(8, cards.size)
        
        // Test getting first 5 cards (like cards.slice(0, 5) in HTML demo)
        val displayCards = cards.sliceArray(0..4)
        assertEquals(5, displayCards.size)
        
        displayCards.forEach { card ->
            assertEquals("attack_card", card.type)
            assertTrue(card.id.startsWith("attack_card_"))
            
            // Verify properties can be accessed and displayed
            val damage = card.getIntProperty("damage")
            val element = card.getStringProperty("element")
            assertNotNull(damage)
            assertNotNull(element)
        }
    }
    
    @Test
    fun testErrorHandlingLikeHTMLDemo() {
        // Test error scenarios that might occur in HTML demo
        
        // Test invalid YAML
        assertFailsWith<Exception> {
            val parser = GameDefinitionParser()
            parser.parseFromString("invalid: yaml: [")
        }
        
        // Test empty YAML
        assertFailsWith<Exception> {
            createGameEngineFromYaml("", arrayOf("Alice"))
        }
        
        // Test with empty string YAML in parser
        assertFailsWith<Exception> {
            val parser = GameDefinitionParser()
            parser.parseFromString("")
        }
    }
    
    @Test
    fun testGameDefinitionCardTypes() {
        // Test that we can access card type information like in HTML demo
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(validGameYaml)
        
        // Test Object.keys(definition.cards) equivalent
        val cardTypes = definition.cards.keys
        assertEquals(1, cardTypes.size)
        assertTrue(cardTypes.contains("attack_card"))
        
        // Test card definition details
        val attackCardDef = definition.cards["attack_card"]
        assertNotNull(attackCardDef)
        assertEquals(8, attackCardDef.count)
        assertEquals(2, attackCardDef.properties.size)
        assertTrue(attackCardDef.properties.containsKey("damage"))
        assertTrue(attackCardDef.properties.containsKey("element"))
    }
    
    @Test
    fun testPlayerHandProperties() {
        // Test player hand properties used in HTML demo
        val engine = createGameEngineFromYaml(validGameYaml, arrayOf("Alice", "Bob"))
        val players = engine.getPlayers()
        
        players.forEach { player ->
            // Test hand.length property used in HTML
            assertEquals(4, player.hand.size)  // Players dealt 4 cards as per hand_size: 4
            
            // Test other properties displayed in HTML
            assertEquals(15, player.health)
            assertEquals(0, player.score)
            assertTrue(player.id.startsWith("player_"))
        }
    }
}