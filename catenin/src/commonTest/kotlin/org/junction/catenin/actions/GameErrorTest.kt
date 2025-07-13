package org.junction.catenin.actions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GameErrorTest {
    
    @Test
    fun testPlayerNotFoundError() {
        val error = GameError.PlayerNotFound("player_123")
        
        assertEquals("PLAYER_NOT_FOUND", error.code)
        assertEquals("Player not found: player_123", error.message)
        assertEquals(mapOf("playerId" to "player_123"), error.context)
    }
    
    @Test
    fun testNotPlayerTurnError() {
        val error = GameError.NotPlayerTurn("player_1", "player_2")
        
        assertEquals("NOT_PLAYER_TURN", error.code)
        assertEquals("Not your turn. Expected: player_1, but current player is: player_2", error.message)
        assertEquals(
            mapOf(
                "expectedPlayerId" to "player_1",
                "actualPlayerId" to "player_2"
            ), 
            error.context
        )
    }
    
    @Test
    fun testCardNotInHandError() {
        val error = GameError.CardNotInHand("card_456", "player_789")
        
        assertEquals("CARD_NOT_IN_HAND", error.code)
        assertEquals("Card card_456 not found in player player_789's hand", error.message)
        assertEquals(
            mapOf(
                "cardId" to "card_456",
                "playerId" to "player_789"
            ), 
            error.context
        )
    }
    
    @Test
    fun testDeckEmptyError() {
        val error = GameError.DeckEmpty("player_001")
        
        assertEquals("DECK_EMPTY", error.code)
        assertEquals("Cannot draw card - deck is empty", error.message)
        assertEquals(mapOf("playerId" to "player_001"), error.context)
    }
    
    @Test
    fun testHandFullError() {
        val error = GameError.HandFull("player_002", 10, 10)
        
        assertEquals("HAND_FULL", error.code)
        assertEquals("Cannot draw card - hand is full (10/10)", error.message)
        assertEquals(
            mapOf(
                "playerId" to "player_002",
                "currentHandSize" to "10",
                "maxHandSize" to "10"
            ), 
            error.context
        )
    }
    
    @Test
    fun testGameAlreadyOverError() {
        val error = GameError.GameAlreadyOver("FINISHED")
        
        assertEquals("GAME_ALREADY_OVER", error.code)
        assertEquals("Game is already over (phase: FINISHED)", error.message)
        assertEquals(mapOf("currentPhase" to "FINISHED"), error.context)
    }
    
    @Test
    fun testInvalidGamePhaseError() {
        val error = GameError.InvalidGamePhase("PLAYING", "SETUP", "draw_card")
        
        assertEquals("INVALID_GAME_PHASE", error.code)
        assertEquals("Cannot perform draw_card in SETUP phase (expected: PLAYING)", error.message)
        assertEquals(
            mapOf(
                "expectedPhase" to "PLAYING",
                "actualPhase" to "SETUP",
                "action" to "draw_card"
            ), 
            error.context
        )
    }
    
    @Test
    fun testGenericError() {
        val error = GameError.GenericError("Something went wrong")
        
        assertEquals("GENERIC_ERROR", error.code)
        assertEquals("Something went wrong", error.message)
        assertEquals(emptyMap(), error.context)
    }
    
    @Test
    fun testGenericErrorWithCustomCode() {
        val error = GameError.GenericError(
            "Custom error message", 
            "CUSTOM_ERROR_CODE"
        )
        
        assertEquals("CUSTOM_ERROR_CODE", error.code)
        assertEquals("Custom error message", error.message)
        assertEquals(emptyMap(), error.context)
    }
    
    @Test
    fun testGenericErrorWithContext() {
        val context = mapOf("field1" to "value1", "field2" to "value2")
        val error = GameError.GenericError(
            "Error with context", 
            "CONTEXT_ERROR", 
            context
        )
        
        assertEquals("CONTEXT_ERROR", error.code)
        assertEquals("Error with context", error.message)
        assertEquals(context, error.context)
    }
    
    @Test
    fun testStructuredValidationResultValid() {
        val result = StructuredValidationResult.valid()
        
        assertTrue(result.isValid)
        assertEquals(0, result.errors.size)
    }
    
    @Test
    fun testStructuredValidationResultInvalidSingle() {
        val error = GameError.PlayerNotFound("test_player")
        val result = StructuredValidationResult.invalid(error)
        
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertEquals(error, result.errors[0])
    }
    
    @Test
    fun testStructuredValidationResultInvalidMultiple() {
        val error1 = GameError.PlayerNotFound("player1")
        val error2 = GameError.DeckEmpty("player2")
        val error3 = GameError.HandFull("player3", 5, 5)
        
        val result = StructuredValidationResult.invalid(error1, error2, error3)
        
        assertFalse(result.isValid)
        assertEquals(3, result.errors.size)
        assertEquals(error1, result.errors[0])
        assertEquals(error2, result.errors[1])
        assertEquals(error3, result.errors[2])
    }
    
    @Test
    fun testStructuredValidationResultFromStrings() {
        val result = StructuredValidationResult.invalidString("Error 1", "Error 2")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        
        assertEquals("GENERIC_ERROR", result.errors[0].code)
        assertEquals("Error 1", result.errors[0].message)
        
        assertEquals("GENERIC_ERROR", result.errors[1].code)
        assertEquals("Error 2", result.errors[1].message)
    }
    
    @Test
    fun testLegacyValidationConversion() {
        // Test valid conversion
        val validLegacy = ValidationResult.valid()
        val validStructured = validLegacy.toStructured()
        
        assertTrue(validStructured.isValid)
        assertEquals(0, validStructured.errors.size)
        
        // Test invalid conversion
        val invalidLegacy = ValidationResult.invalid("Error 1", "Error 2")
        val invalidStructured = invalidLegacy.toStructured()
        
        assertFalse(invalidStructured.isValid)
        assertEquals(2, invalidStructured.errors.size)
        assertEquals("Error 1", invalidStructured.errors[0].message)
        assertEquals("Error 2", invalidStructured.errors[1].message)
    }
    
    @Test
    fun testStructuredToLegacyConversion() {
        // Test valid conversion
        val validStructured = StructuredValidationResult.valid()
        val validLegacy = validStructured.toLegacy()
        
        assertTrue(validLegacy.isValid)
        assertEquals(0, validLegacy.errors.size)
        
        // Test invalid conversion
        val error1 = GameError.PlayerNotFound("player1")
        val error2 = GameError.DeckEmpty("player2")
        val invalidStructured = StructuredValidationResult.invalid(error1, error2)
        val invalidLegacy = invalidStructured.toLegacy()
        
        assertFalse(invalidLegacy.isValid)
        assertEquals(2, invalidLegacy.errors.size)
        assertEquals(error1.message, invalidLegacy.errors[0])
        assertEquals(error2.message, invalidLegacy.errors[1])
    }
    
    @Test
    fun testRoundTripConversion() {
        // Test that legacy -> structured -> legacy preserves information
        val originalLegacy = ValidationResult.invalid("Test error message")
        val convertedStructured = originalLegacy.toStructured()
        val backToLegacy = convertedStructured.toLegacy()
        
        assertEquals(originalLegacy.isValid, backToLegacy.isValid)
        assertEquals(originalLegacy.errors.toList(), backToLegacy.errors.toList())
        
        // Test that structured -> legacy -> structured preserves core information
        val originalStructured = StructuredValidationResult.invalid(
            GameError.PlayerNotFound("test_player")
        )
        val convertedLegacy = originalStructured.toLegacy()
        val backToStructured = convertedLegacy.toStructured()
        
        assertEquals(originalStructured.isValid, backToStructured.isValid)
        assertEquals(originalStructured.errors.size, backToStructured.errors.size)
        // Note: Error types will be GenericError after round-trip, but messages preserved
        assertEquals(originalStructured.errors[0].message, backToStructured.errors[0].message)
    }
    
    @Test
    fun testErrorCodeUniqueness() {
        val errors = listOf(
            GameError.PlayerNotFound("p1"),
            GameError.NotPlayerTurn("p1", "p2"),
            GameError.CardNotInHand("c1", "p1"),
            GameError.DeckEmpty("p1"),
            GameError.HandFull("p1", 5, 10),
            GameError.GameAlreadyOver("FINISHED"),
            GameError.InvalidGamePhase("PLAYING", "SETUP", "action"),
            GameError.GenericError("test")
        )
        
        val codes = errors.map { it.code }
        val uniqueCodes = codes.toSet()
        
        assertEquals(codes.size, uniqueCodes.size, "All error codes should be unique")
    }
}