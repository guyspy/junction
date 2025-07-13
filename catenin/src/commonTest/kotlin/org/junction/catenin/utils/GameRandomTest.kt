package org.junction.catenin.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class GameRandomTest {
    
    @Test
    fun testDeterministicBehaviorWithSeed() {
        // Set seed for deterministic testing
        GameRandom.setSeed(12345)
        
        // Generate some values
        val firstRun = listOf(
            GameRandom.nextInt(1, 10),
            GameRandom.nextInt(1, 10),
            GameRandom.nextInt(1, 10)
        )
        
        // Reset with same seed
        GameRandom.setSeed(12345)
        
        // Generate same values again
        val secondRun = listOf(
            GameRandom.nextInt(1, 10),
            GameRandom.nextInt(1, 10),
            GameRandom.nextInt(1, 10)
        )
        
        // Should be identical
        assertEquals(firstRun, secondRun)
        
        // Reset to default
        GameRandom.resetToDefault()
    }
    
    @Test
    fun testRandomRangeGeneration() {
        GameRandom.setSeed(42)
        
        repeat(100) {
            val value = GameRandom.nextInt(5, 15)
            assertTrue(value in 5..15, "Value $value should be in range 5-15")
        }
        
        repeat(100) {
            val value = GameRandom.nextInt(20)
            assertTrue(value in 0 until 20, "Value $value should be in range 0-19")
        }
        
        GameRandom.resetToDefault()
    }
    
    @Test
    fun testArrayChoiceFunction() {
        GameRandom.setSeed(123)
        
        val testArray = arrayOf("fire", "water", "earth", "air")
        val choices = mutableSetOf<String>()
        
        // Generate multiple choices to ensure randomness
        repeat(50) {
            val choice = GameRandom.choose(testArray)
            assertNotNull(choice)
            assertTrue(choice in testArray)
            choices.add(choice)
        }
        
        // Should have seen multiple different elements (probabilistic test)
        assertTrue(choices.size > 1, "Should choose different elements, got: $choices")
        
        GameRandom.resetToDefault()
    }
    
    @Test
    fun testListChoiceFunction() {
        GameRandom.setSeed(456)
        
        val testList = listOf(1, 2, 3, 4, 5)
        val choices = mutableSetOf<Int>()
        
        repeat(50) {
            val choice = GameRandom.choose(testList)
            assertNotNull(choice)
            assertTrue(choice in testList)
            choices.add(choice)
        }
        
        // Should have seen multiple different elements
        assertTrue(choices.size > 1, "Should choose different elements, got: $choices")
        
        GameRandom.resetToDefault()
    }
    
    @Test
    fun testEmptyCollectionChoice() {
        val emptyArray = emptyArray<String>()
        val emptyList = emptyList<Int>()
        
        assertNull(GameRandom.choose(emptyArray))
        assertNull(GameRandom.choose(emptyList))
    }
    
    @Test
    fun testArrayShuffle() {
        GameRandom.setSeed(789)
        
        val original = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val toShuffle = original.copyOf()
        
        val shuffled = GameRandom.shuffle(toShuffle)
        
        // Same array instance returned
        assertEquals(toShuffle, shuffled)
        
        // Same elements, different order (probabilistic)
        assertEquals(original.sorted(), shuffled.sorted())
        
        // Should be different order (with high probability)
        assertNotEquals(original.toList(), shuffled.toList())
        
        GameRandom.resetToDefault()
    }
    
    @Test
    fun testListShuffle() {
        GameRandom.setSeed(101112)
        
        val original = mutableListOf("a", "b", "c", "d", "e", "f")
        val toShuffle = original.toMutableList()
        
        val shuffled = GameRandom.shuffle(toShuffle)
        
        // Same list instance returned
        assertEquals(toShuffle, shuffled)
        
        // Same elements, different order (probabilistic)
        assertEquals(original.sorted(), shuffled.sorted())
        
        GameRandom.resetToDefault()
    }
    
    @Test
    fun testGameIdGeneration() {
        GameRandom.setSeed(999)
        
        val gameIds = mutableSetOf<String>()
        
        repeat(10) {
            val gameId = GameRandom.generateGameId()
            
            // Check format: "game_XXXXXXX_XXXX"
            assertTrue(gameId.startsWith("game_"))
            
            val parts = gameId.split("_")
            assertEquals(3, parts.size)
            assertEquals("game", parts[0])
            
            // Check timestamp part (7 digits)
            val timestamp = parts[1].toIntOrNull()
            assertNotNull(timestamp)
            assertTrue(timestamp in 1000000..9999999)
            
            // Check random part (4 digits)
            val random = parts[2].toIntOrNull()
            assertNotNull(random)
            assertTrue(random in 1000..9999)
            
            gameIds.add(gameId)
        }
        
        // All IDs should be unique (with deterministic seed)
        assertEquals(10, gameIds.size)
        
        GameRandom.resetToDefault()
    }
    
    @Test
    fun testResetToDefault() {
        // Set a specific seed
        GameRandom.setSeed(12345)
        val seededValue = GameRandom.nextInt(1, 1000)
        
        // Reset to default
        GameRandom.resetToDefault()
        val defaultValue1 = GameRandom.nextInt(1, 1000)
        val defaultValue2 = GameRandom.nextInt(1, 1000)
        
        // Default behavior should be non-deterministic
        // (This is probabilistic but very likely to pass)
        assertNotEquals(defaultValue1, defaultValue2)
        
        // Reset with same seed again
        GameRandom.setSeed(12345)
        val seededValueAgain = GameRandom.nextInt(1, 1000)
        
        // Should match the first seeded value
        assertEquals(seededValue, seededValueAgain)
        
        GameRandom.resetToDefault()
    }
    
    @Test
    fun testCrossRangeConsistency() {
        GameRandom.setSeed(55555)
        
        // Test that different range methods are consistent
        val rangeValues = (1..20).map { GameRandom.nextInt(1, 6) }
        val untilValues = (1..20).map { GameRandom.nextInt(6) + 1 }
        
        // Reset and generate same sequence with different method
        GameRandom.setSeed(55555)
        val rangeValues2 = (1..20).map { GameRandom.nextInt(1, 6) }
        
        assertEquals(rangeValues, rangeValues2)
        
        GameRandom.resetToDefault()
    }
}