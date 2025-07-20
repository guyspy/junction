package org.junction.catenin.parser

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YamlParserTest {

    @Serializable
    data class TestData(
        val name: String,
        val value: Int
    )

    @Test
    fun testParseValidYaml() {
        val parser = YamlParser()
        val yaml = """
            name: "Test"
            value: 42
        """.trimIndent()

        val result = parser.parseFromString<TestData>(yaml)

        assertEquals("Test", result.name)
        assertEquals(42, result.value)
    }

    @Test
    fun testParseInvalidYaml() {
        val parser = YamlParser()
        val invalidYaml = """
            name: "Test"
            value: not_a_number
        """.trimIndent()

        assertFailsWith<YamlParseException> {
            parser.parseFromString<TestData>(invalidYaml)
        }
    }

    @Test
    fun testParseMalformedYaml() {
        val parser = YamlParser()
        val malformedYaml = """
            name: "Test
            value: 42
        """.trimIndent()

        assertFailsWith<YamlParseException> {
            parser.parseFromString<TestData>(malformedYaml)
        }
    }
}