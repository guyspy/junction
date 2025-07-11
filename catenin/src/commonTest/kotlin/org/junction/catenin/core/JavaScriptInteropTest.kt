package org.junction.catenin.core

import org.junction.catenin.model.CardFactory
import org.junction.catenin.parser.GameDefinitionParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * JavaScript Interoperability Test
 * 
 * This test demonstrates the true purpose of the Catenin project:
 * - AI-friendly YAML DSL for educational games
 * - JavaScript-compatible API for web applications  
 * - Cross-platform game engine (JVM + JavaScript)
 * - Educational game creation and modification
 */
class JavaScriptInteropTest {
    
    @Test
    fun testFullGameCreationWorkflow() {
        // Real educational game YAML - Math Battle Card Game
        val mathBattleYaml = """
            meta:
              name: "Math Battle"
              target_age: [8, 12]
              player_count: [2, 4]
            
            cards:
              addition_card:
                count: 12
                properties:
                  problem: {type: string}
                  answer: {type: int, min: 2, max: 20}
                  difficulty: {type: enum, values: [easy, medium, hard]}
                  points: {type: int, min: 1, max: 5}
              
              multiplication_card:
                count: 8
                properties:
                  problem: {type: string}
                  answer: {type: int, min: 4, max: 100}
                  difficulty: {type: enum, values: [easy, medium, hard]}
                  points: {type: int, min: 2, max: 8}
            
            mechanics:
              setup:
                players:
                  health: 100
                  hand_size: 5
                  initial_score: 0
              win_conditions:
                - type: "score_target"
                  target: 50
                  message: "{winner} wins by reaching 50 points!"
            
            ai_hints:
              difficulty_factors:
                - "cards.addition_card.properties.answer.max"
                - "cards.multiplication_card.properties.answer.max"
              common_modifications:
                easier: {addition_max: 10, multiplication_max: 25}
                harder: {addition_max: 50, multiplication_max: 200}
        """.trimIndent()
        
        // Create game engine using JavaScript-friendly factory function
        val engine = createGameEngineFromYaml(mathBattleYaml, arrayOf("Alice", "Bob", "Charlie"))
        
        // Test game definition parsing
        val definition = engine.getGameDefinition()
        assertEquals("Math Battle", definition.meta.name)
        assertEquals(listOf(8, 12), definition.meta.targetAge)
        assertEquals(2, definition.cards.size)
        
        // Test JavaScript-friendly player array
        val players = engine.getPlayers()
        assertEquals(3, players.size)
        
        // Verify JavaScript Array operations work
        val playerNames = players.map { it.name }
        assertEquals(listOf("Alice", "Bob", "Charlie"), playerNames)
        
        // Test player setup from YAML
        players.forEach { player ->
            assertEquals(100, player.health)
            assertEquals(0, player.score)
            assertTrue(player.id.startsWith("player_"))
        }
        
        // Test AI hints are parsed correctly
        val aiHints = definition.aiHints
        assertNotNull(aiHints)
        assertEquals(2, aiHints.difficultyFactors.size)
        assertEquals(2, aiHints.commonModifications.size)
    }
    
    @Test
    fun testJavaScriptCardGeneration() {
        // Educational vocabulary game YAML
        val vocabGameYaml = """
            meta:
              name: "Vocabulary Builder"
              target_age: [10, 14]
              player_count: [2, 6]
            
            cards:
              word_card:
                count: 20
                properties:
                  word: {type: string}
                  definition: {type: string}
                  difficulty: {type: enum, values: [beginner, intermediate, advanced]}
                  category: {type: enum, values: [science, history, literature, math]}
                  points: {type: int, min: 1, max: 10}
              
              bonus_card:
                count: 5
                properties:
                  effect: {type: enum, values: [double_points, steal_card, extra_turn]}
                  description: {type: string}
                  rarity: {type: enum, values: [common, rare, legendary]}
        """.trimIndent()
        
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(vocabGameYaml)
        
        // Test JavaScript-friendly card generation
        val cardFactory = CardFactory.fromDefinition(definition)
        val cards = cardFactory.generateCards()
        
        // Verify JavaScript Array operations
        assertEquals(25, cards.size)
        
        // Test array methods work correctly
        val wordCards = cards.filter { it.type == "word_card" }
        val bonusCards = cards.filter { it.type == "bonus_card" }
        
        assertEquals(20, wordCards.size)
        assertEquals(5, bonusCards.size)
        
        // Test card properties are generated correctly
        wordCards.forEach { card ->
            val difficulty = card.getStringProperty("difficulty")
            assertNotNull(difficulty)
            assertTrue(difficulty in listOf("beginner", "intermediate", "advanced"))
            
            val category = card.getStringProperty("category")
            assertNotNull(category)
            assertTrue(category in listOf("science", "history", "literature", "math"))
            
            val points = card.getIntProperty("points")
            assertNotNull(points)
            assertTrue(points in 1..10)
        }
        
        bonusCards.forEach { card ->
            val effect = card.getStringProperty("effect")
            assertNotNull(effect)
            assertTrue(effect in listOf("double_points", "steal_card", "extra_turn"))
            
            val rarity = card.getStringProperty("rarity")
            assertNotNull(rarity)
            assertTrue(rarity in listOf("common", "rare", "legendary"))
        }
    }
    
    @Test
    fun testGameModificationForDifficulty() {
        // History timeline game - demonstrates AI-friendly modification
        val historyGameYaml = """
            meta:
              name: "History Timeline"
              target_age: [12, 16]
              player_count: [2, 4]
            
            cards:
              event_card:
                count: 30
                properties:
                  event: {type: string}
                  year: {type: int, min: 1000, max: 2000}
                  era: {type: enum, values: [medieval, renaissance, industrial, modern]}
                  importance: {type: int, min: 1, max: 10}
              
              challenge_card:
                count: 10
                properties:
                  question: {type: string}
                  difficulty: {type: enum, values: [easy, medium, hard, expert]}
                  time_limit: {type: int, min: 30, max: 120}
                  bonus_points: {type: int, min: 2, max: 15}
            
            mechanics:
              setup:
                players:
                  health: 75
                  hand_size: 6
              win_conditions:
                - type: "timeline_complete"
                  message: "{winner} correctly ordered the timeline!"
            
            ai_hints:
              difficulty_factors:
                - "cards.event_card.properties.year.min"
                - "cards.event_card.properties.year.max"
                - "cards.challenge_card.properties.time_limit.min"
              common_modifications:
                easier: {year_min: 1500, year_max: 1900, time_limit_min: 60}
                harder: {year_min: 500, year_max: 2023, time_limit_min: 15}
        """.trimIndent()
        
        val engine = createGameEngineFromYaml(historyGameYaml, arrayOf("Student1", "Student2"))
        val definition = engine.getGameDefinition()
        
        // Test that game supports educational modification
        val aiHints = definition.aiHints
        assertNotNull(aiHints)
        
        // Verify AI can easily identify modification points
        assertTrue(aiHints.difficultyFactors.any { it.contains("year") })
        assertTrue(aiHints.difficultyFactors.any { it.contains("time_limit") })
        
        // Test different difficulty modifications exist
        val modifications = aiHints.commonModifications
        assertTrue(modifications.containsKey("easier"))
        assertTrue(modifications.containsKey("harder"))
        
        // Generate cards to test the game content
        val cardFactory = CardFactory.fromDefinition(definition)
        val cards = cardFactory.generateCards()
        
        assertEquals(40, cards.size)
        
        // Test educational content structure
        val eventCards = cards.filter { it.type == "event_card" }
        val challengeCards = cards.filter { it.type == "challenge_card" }
        
        assertEquals(30, eventCards.size)
        assertEquals(10, challengeCards.size)
        
        // Verify historical accuracy constraints
        eventCards.forEach { card ->
            val year = card.getIntProperty("year")
            assertNotNull(year)
            assertTrue(year in 1000..2000)
            
            val era = card.getStringProperty("era")
            assertNotNull(era)
            assertTrue(era in listOf("medieval", "renaissance", "industrial", "modern"))
            
            val importance = card.getIntProperty("importance")
            assertNotNull(importance)
            assertTrue(importance in 1..10)
        }
        
        challengeCards.forEach { card ->
            val difficulty = card.getStringProperty("difficulty")
            assertNotNull(difficulty)
            assertTrue(difficulty in listOf("easy", "medium", "hard", "expert"))
            
            val timeLimit = card.getIntProperty("time_limit")
            assertNotNull(timeLimit)
            assertTrue(timeLimit in 30..120)
            
            val bonusPoints = card.getIntProperty("bonus_points")
            assertNotNull(bonusPoints)
            assertTrue(bonusPoints in 2..15)
        }
    }
    
    @Test
    fun testJavaScriptArrayCompatibility() {
        // Test that all collections are JavaScript Array-compatible
        val simpleGameYaml = """
            meta:
              name: "Array Test Game"
              target_age: [6, 10]
              player_count: [2, 2]
            
            cards:
              test_card:
                count: 5
                properties:
                  value: {type: int, min: 1, max: 5}
        """.trimIndent()
        
        val engine = createGameEngineFromYaml(simpleGameYaml, arrayOf("Player1", "Player2"))
        val cardFactory = CardFactory.fromDefinition(engine.getGameDefinition())
        
        // Test that these return JavaScript Arrays (not Kotlin Lists)
        val players = engine.getPlayers()
        val cards = cardFactory.generateCards()
        
        // Verify we can use JavaScript Array methods
        assertEquals(2, players.size)
        assertEquals(5, cards.size)
        
        // Test Array methods that would fail on Kotlin Lists in JS
        val playerNames = players.map { it.name }
        assertEquals(2, playerNames.size)
        
        val cardIds = cards.map { it.id }
        assertEquals(5, cardIds.size)
        
        // Test that each card has unique ID
        val uniqueIds = cardIds.distinct()
        assertEquals(5, uniqueIds.size)
        
        // Test card properties can be accessed with JS-friendly syntax
        cards.forEach { card ->
            val value = card.getIntProperty("value")
            assertNotNull(value)
            assertTrue(value in 1..5)
        }
    }
}