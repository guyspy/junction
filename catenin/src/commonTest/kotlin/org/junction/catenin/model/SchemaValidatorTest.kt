package org.junction.catenin.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaValidatorTest {
    
    private val validator = SchemaValidator()
    
    private fun createValidGameMeta(): GameMeta {
        return GameMeta(
            name = "Test Game",
            targetAge = intArrayOf(8, 12),
            participantCount = intArrayOf(2, 4)
        )
    }
    
    private fun createValidObjectDefinition(): ObjectDefinition {
        return ObjectDefinition(
            properties = mapOf(
                "health" to PropertyDefinition(PropertyType.INT, IntValue(100), IntValue(0), IntValue(200)),
                "name" to PropertyDefinition(PropertyType.STRING, StringValue("Default"))
            ),
            states = mapOf(
                "activated" to PropertyDefinition(PropertyType.BOOL, BoolValue(false))
            )
        )
    }
    
    private fun createValidGameDefinition(): UniversalGameDefinition {
        return UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition())
        )
    }
    
    @Test
    fun testValidGameDefinition() {
        val definition = createValidGameDefinition()
        val result = validator.validate(definition)
        
        assertTrue(result.isValid)
        assertEquals(0, result.issues.size)
        assertEquals(0, result.getErrors().size)
        assertEquals(0, result.getWarnings().size)
    }
    
    @Test
    fun testInvalidMetaEmptyName() {
        val meta = GameMeta("", intArrayOf(8, 12), intArrayOf(2, 4))
        val definition = UniversalGameDefinition(meta, mapOf("test" to ObjectDefinition()))
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.message.contains("Game name cannot be empty") })
        assertTrue(result.issues.any { it.path == "meta.name" })
    }
    
    @Test
    fun testInvalidMetaTargetAge() {
        val meta = GameMeta("Test", intArrayOf(12, 8), intArrayOf(2, 4)) // min > max
        val definition = UniversalGameDefinition(meta, mapOf("test" to ObjectDefinition()))
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.message.contains("Target age minimum (12) cannot be greater than maximum (8)") })
    }
    
    @Test
    fun testInvalidMetaParticipantCount() {
        val meta = GameMeta("Test", intArrayOf(8, 12), intArrayOf(0, 4)) // min = 0
        val definition = UniversalGameDefinition(meta, mapOf("test" to ObjectDefinition()))
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.message.contains("Participant count values must be positive") })
    }
    
    @Test
    fun testInvalidMetaArraySizes() {
        val meta = GameMeta("Test", intArrayOf(8), intArrayOf(2, 4, 6)) // wrong array sizes
        val definition = UniversalGameDefinition(meta, mapOf("test" to ObjectDefinition()))
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.message.contains("Target age must be an array of exactly 2 integers") })
        assertTrue(result.issues.any { it.message.contains("Participant count must be an array of exactly 2 integers") })
    }
    
    @Test
    fun testNoObjectTypes() {
        val definition = UniversalGameDefinition(createValidGameMeta(), emptyMap())
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.message.contains("Game must define at least one object type") })
    }
    
    @Test
    fun testEmptyObjectType() {
        val emptyObjectType = ObjectDefinition()
        val definition = UniversalGameDefinition(
            createValidGameMeta(),
            mapOf("empty" to emptyObjectType)
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.message.contains("Object type 'empty' must define at least one property or state") })
    }
    
    @Test
    fun testInstanceWithInvalidTemplate() {
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            instances = mapOf(
                "invalid_instance" to ObjectInstance(template = "unknown_type")
            )
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Instance 'invalid_instance' references unknown object type 'unknown_type'") 
        })
    }
    
    @Test
    fun testInstanceWithInvalidProperty() {
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            instances = mapOf(
                "test_instance" to ObjectInstance(
                    template = "creature",
                    properties = mapOf("unknown_prop" to "value")
                )
            )
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Instance 'test_instance' overrides unknown property 'unknown_prop'") 
        })
    }
    
    @Test
    fun testInstanceWithInvalidPropertyValue() {
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            instances = mapOf(
                "test_instance" to ObjectInstance(
                    template = "creature",
                    properties = mapOf("health" to "invalid_number")
                )
            )
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Instance 'test_instance' has invalid value for property 'health'") 
        })
    }
    
    @Test
    fun testInstanceWithInvalidState() {
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            instances = mapOf(
                "test_instance" to ObjectInstance(
                    template = "creature",
                    states = mapOf("unknown_state" to "value")
                )
            )
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Instance 'test_instance' overrides unknown state 'unknown_state'") 
        })
    }
    
    @Test
    fun testTriggerWithInvalidObjectType() {
        val triggers = listOf(
            TriggerDefinition(
                `when` = TriggerCondition(objectType = "unknown_type"),
                effects = listOf(LogEffect("test"))
            )
        )
        
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            triggers = triggers
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Trigger references unknown object type 'unknown_type'") 
        })
    }
    
    @Test
    fun testTriggerWithInvalidProperty() {
        val triggers = listOf(
            TriggerDefinition(
                `when` = TriggerCondition(
                    objectType = "creature",
                    propertyChanged = "unknown_property"
                ),
                effects = listOf(LogEffect("test"))
            )
        )
        
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            triggers = triggers
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Trigger references unknown property/state 'unknown_property'") 
        })
    }
    
    @Test
    fun testTriggerWithNoEffects() {
        val triggers = listOf(
            TriggerDefinition(
                `when` = TriggerCondition(objectType = "creature"),
                effects = emptyList()
            )
        )
        
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            triggers = triggers
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Trigger must define at least one effect") 
        })
    }
    
    @Test
    fun testLogEffectWithEmptyMessage() {
        val triggers = listOf(
            TriggerDefinition(
                `when` = TriggerCondition(objectType = "creature"),
                effects = listOf(LogEffect(""))
            )
        )
        
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            triggers = triggers
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Log effect message cannot be empty") 
        })
    }
    
    @Test
    fun testModifyPropertyEffectWithInvalidConfiguration() {
        val triggers = listOf(
            TriggerDefinition(
                `when` = TriggerCondition(objectType = "creature"),
                effects = listOf(
                    ModifyPropertyEffect(
                        target = "this",
                        property = "",
                        delta = "10"
                    )
                )
            )
        )
        
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            triggers = triggers
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Modify property effect must specify a property name") 
        })
    }
    
    @Test
    fun testModifyPropertyEffectWithBothDeltaAndValue() {
        val triggers = listOf(
            TriggerDefinition(
                `when` = TriggerCondition(objectType = "creature"),
                effects = listOf(
                    ModifyPropertyEffect(
                        target = "this",
                        property = "health",
                        delta = "10",
                        value = "100"
                    )
                )
            )
        )
        
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            triggers = triggers
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Modify property effect cannot specify both 'delta' and 'value'") 
        })
    }
    
    @Test
    fun testModifyPropertyEffectWithNeitherDeltaNorValue() {
        val triggers = listOf(
            TriggerDefinition(
                `when` = TriggerCondition(objectType = "creature"),
                effects = listOf(
                    ModifyPropertyEffect(
                        target = "this",
                        property = "health"
                    )
                )
            )
        )
        
        val definition = UniversalGameDefinition(
            meta = createValidGameMeta(),
            objectTypes = mapOf("creature" to createValidObjectDefinition()),
            triggers = triggers
        )
        
        val result = validator.validate(definition)
        
        assertFalse(result.isValid)
        assertTrue(result.issues.any { 
            it.message.contains("Modify property effect must specify either 'delta' or 'value'") 
        })
    }
    
    @Test
    fun testValidationResultHelpers() {
        val error = ValidationError("Error message", "path", ValidationSeverity.ERROR)
        val warning = ValidationError("Warning message", "path", ValidationSeverity.WARNING)
        
        val result = ValidationResult(listOf(error, warning))
        
        assertFalse(result.isValid)
        assertEquals(1, result.getErrors().size)
        assertEquals(1, result.getWarnings().size)
        assertEquals("Error message", result.getErrors()[0].message)
        assertEquals("Warning message", result.getWarnings()[0].message)
    }
    
    @Test
    fun testValidationResultCompanionFunctions() {
        val validResult = ValidationResult.valid()
        assertTrue(validResult.isValid)
        assertEquals(0, validResult.issues.size)
        
        val errorResult = ValidationResult.error("Test error", "test.path")
        assertFalse(errorResult.isValid)
        assertEquals(1, errorResult.issues.size)
        assertEquals(ValidationSeverity.ERROR, errorResult.issues[0].severity)
        
        val warningResult = ValidationResult.warning("Test warning", "test.path")
        assertTrue(warningResult.isValid) // Warnings don't make result invalid
        assertEquals(1, warningResult.issues.size)
        assertEquals(ValidationSeverity.WARNING, warningResult.issues[0].severity)
    }
}