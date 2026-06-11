package org.junction.catenin.parser

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.serializer

/**
 * Generic YAML parser using Kaml library
 */
class YamlParser {
    
    /**
     * Parse YAML string to typed object
     */
    inline fun <reified T> parseFromString(yamlContent: String): T {
        try {
            return Yaml.default.decodeFromString(serializer<T>(), yamlContent)
        } catch (e: Exception) {
            throw YamlParseException("Failed to parse YAML: ${e.message}", e)
        }
    }
}

/**
 * Exception thrown when YAML parsing fails
 */
class YamlParseException(message: String, cause: Throwable? = null) : Exception(message, cause)