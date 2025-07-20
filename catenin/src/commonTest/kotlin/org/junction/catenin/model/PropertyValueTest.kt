package org.junction.catenin.model

import org.junction.catenin.model.values.*

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PropertyValueTest {

    @Test
    fun testIntValue() {
        val value = IntValue(42)
        assertEquals(42, value.value)
        assertEquals("42", value.toString())
    }

    @Test
    fun testStringValue() {
        val value = StringValue("test")
        assertEquals("test", value.value)
        assertEquals("test", value.toString())
    }

    @Test
    fun testBoolValue() {
        val trueBool = BoolValue(true)
        val falseBool = BoolValue(false)
        assertTrue(trueBool.value)
        assertFalse(falseBool.value)
        assertEquals("true", trueBool.toString())
        assertEquals("false", falseBool.toString())
    }

    @Test
    fun testObjectRefValue() {
        val ref = ObjectRefValue("player_1")
        assertEquals("player_1", ref.objectId)
        assertEquals("player_1", ref.toString())
    }

    @Test
    fun testPropertyValueEquality() {
        val int1 = IntValue(42)
        val int2 = IntValue(42)
        val int3 = IntValue(43)
        
        assertEquals(int1, int2)
        assertTrue(int1 != int3)
        
        val str1 = StringValue("test")
        val str2 = StringValue("test")
        assertEquals(str1, str2)
        
        assertTrue(int1 != str1) // Different types
    }

    @Test
    fun testPropertyValueComparison() {
        val small = IntValue(1)
        val large = IntValue(10)
        
        assertTrue(small.compareTo(large) < 0)
        assertTrue(large.compareTo(small) > 0)
        assertEquals(0, small.compareTo(IntValue(1)))
        
        // String comparison
        val strA = StringValue("a")
        val strB = StringValue("b")
        assertTrue(strA.compareTo(strB) < 0)
    }

    @Test
    fun testIncompatibleComparison() {
        val intVal = IntValue(42)
        val strVal = StringValue("test")
        
        assertFailsWith<IllegalArgumentException> {
            intVal.compareTo(strVal)
        }
    }

    @Test
    fun testPropertyValueTypeChecking() {
        val intVal = IntValue(42)
        val strVal = StringValue("test")
        val boolVal = BoolValue(true)
        val refVal = ObjectRefValue("obj_1")
        
        assertTrue(intVal.isInt())
        assertFalse(intVal.isString())
        assertFalse(intVal.isBool())
        assertFalse(intVal.isObjectRef())
        
        assertTrue(strVal.isString())
        assertTrue(boolVal.isBool())
        assertTrue(refVal.isObjectRef())
    }

    @Test
    fun testPropertyValueConversion() {
        val intVal = IntValue(42)
        val strVal = StringValue("test")
        val boolVal = BoolValue(true)
        val refVal = ObjectRefValue("obj_1")
        
        assertEquals(42, intVal.asInt())
        assertEquals("test", strVal.asString())
        assertTrue(boolVal.asBool())
        assertEquals("obj_1", refVal.asObjectRef())
        
        // Test invalid conversions
        assertFailsWith<IllegalStateException> {
            strVal.asInt()
        }
        assertFailsWith<IllegalStateException> {
            intVal.asString()
        }
    }
}