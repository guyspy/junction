package org.junction.catenin.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class PropertyDefinitionTest {

    @Test
    fun testBasicPropertyDefinition() {
        val definition = PropertyDefinition(
            type = PropertyType.INT,
            initial = IntValue(10)
        )
        
        assertEquals(PropertyType.INT, definition.type)
        assertEquals(IntValue(10), definition.initial)
    }

    @Test
    fun testPropertyDefinitionWithConstraints() {
        val definition = PropertyDefinition(
            type = PropertyType.INT,
            initial = IntValue(5),
            min = IntValue(1),
            max = IntValue(10)
        )
        
        assertEquals(IntValue(1), definition.min)
        assertEquals(IntValue(10), definition.max)
    }

    @Test
    fun testPropertyValidation() {
        val definition = PropertyDefinition(
            type = PropertyType.INT,
            min = IntValue(1),
            max = IntValue(10)
        )
        
        assertTrue(definition.isValid(IntValue(5)))
        assertTrue(definition.isValid(IntValue(1)))
        assertTrue(definition.isValid(IntValue(10)))
        assertFalse(definition.isValid(IntValue(0)))
        assertFalse(definition.isValid(IntValue(11)))
        assertFalse(definition.isValid(StringValue("test"))) // Wrong type
    }

    @Test
    fun testStringPropertyValidation() {
        val definition = PropertyDefinition(
            type = PropertyType.STRING,
            initial = StringValue("default")
        )
        
        assertTrue(definition.isValid(StringValue("test")))
        assertTrue(definition.isValid(StringValue("")))
        assertFalse(definition.isValid(IntValue(42))) // Wrong type
    }

    @Test
    fun testBoolPropertyValidation() {
        val definition = PropertyDefinition(
            type = PropertyType.BOOL,
            initial = BoolValue(false)
        )
        
        assertTrue(definition.isValid(BoolValue(true)))
        assertTrue(definition.isValid(BoolValue(false)))
        assertFalse(definition.isValid(StringValue("true"))) // Wrong type
    }

    @Test
    fun testObjectRefPropertyValidation() {
        val definition = PropertyDefinition(
            type = PropertyType.OBJECT_REF,
            initial = ObjectRefValue("default_obj")
        )
        
        assertTrue(definition.isValid(ObjectRefValue("player_1")))
        assertTrue(definition.isValid(ObjectRefValue("card_42")))
        assertFalse(definition.isValid(StringValue("player_1"))) // Wrong type
    }

    @Test
    fun testPropertyTypeFromValue() {
        assertEquals(PropertyType.INT, PropertyType.fromValue(IntValue(42)))
        assertEquals(PropertyType.STRING, PropertyType.fromValue(StringValue("test")))
        assertEquals(PropertyType.BOOL, PropertyType.fromValue(BoolValue(true)))
        assertEquals(PropertyType.OBJECT_REF, PropertyType.fromValue(ObjectRefValue("obj_1")))
    }

    @Test
    fun testValidateConstraints() {
        // Test that min/max constraints match property type
        assertFailsWith<IllegalArgumentException> {
            PropertyDefinition(
                type = PropertyType.INT,
                min = StringValue("invalid") // Wrong type for constraint
            )
        }
        
        assertFailsWith<IllegalArgumentException> {
            PropertyDefinition(
                type = PropertyType.STRING,
                max = IntValue(10) // Wrong type for constraint
            )
        }
    }

    @Test
    fun testValidateInitialValue() {
        // Test that initial value matches property type
        assertFailsWith<IllegalArgumentException> {
            PropertyDefinition(
                type = PropertyType.INT,
                initial = StringValue("invalid") // Wrong type for initial
            )
        }
    }

    @Test
    fun testValidateMinMaxOrder() {
        // Test that min <= max for numeric types
        assertFailsWith<IllegalArgumentException> {
            PropertyDefinition(
                type = PropertyType.INT,
                min = IntValue(10),
                max = IntValue(5) // Max < min
            )
        }
    }

    @Test
    fun testStringConstraintsAreIgnored() {
        // String properties should ignore min/max constraints
        val definition = PropertyDefinition(
            type = PropertyType.STRING,
            initial = StringValue("test"),
            min = StringValue("a"), // These should be ignored
            max = StringValue("z")
        )
        
        assertTrue(definition.isValid(StringValue("anything")))
    }

    @Test
    fun testGetDefaultValue() {
        val withInitial = PropertyDefinition(
            type = PropertyType.INT,
            initial = IntValue(42)
        )
        assertEquals(IntValue(42), withInitial.getDefaultValue())
        
        val withoutInitial = PropertyDefinition(type = PropertyType.INT)
        assertEquals(IntValue(0), withoutInitial.getDefaultValue()) // Default for INT
        
        val stringDefault = PropertyDefinition(type = PropertyType.STRING)
        assertEquals(StringValue(""), stringDefault.getDefaultValue()) // Default for STRING
        
        val boolDefault = PropertyDefinition(type = PropertyType.BOOL)
        assertEquals(BoolValue(false), boolDefault.getDefaultValue()) // Default for BOOL
    }
}