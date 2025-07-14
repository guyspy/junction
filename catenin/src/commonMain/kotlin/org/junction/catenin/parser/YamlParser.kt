package org.junction.catenin.parser

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.serializer
import org.junction.catenin.model.UniversalGameDefinition

class YamlParser {
    
    inline fun <reified T> parseFromString(yamlContent: String): T {
        try {
            return Yaml.default.decodeFromString(serializer<T>(), yamlContent)
        } catch (e: Exception) {
            throw GameDefinitionParseException("Failed to parse YAML: ${e.message}", e)
        }
    }
}

class GameDefinitionParseException(message: String, cause: Throwable? = null) : Exception(message, cause)