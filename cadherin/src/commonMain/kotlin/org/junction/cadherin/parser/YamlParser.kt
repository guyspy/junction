package org.junction.cadherin.parser

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import org.junction.cadherin.model.*

class YamlParser {
    private val yaml = Yaml.default
    
    fun parseGameDefinition(yamlContent: String): GameDefinition {
        try {
            return yaml.decodeFromString(GameDefinition.serializer(), yamlContent)
        } catch (e: Exception) {
            throw GameDefinitionParseException("Failed to parse YAML: ${e.message}", e)
        }
    }
}

class GameDefinitionParseException(message: String, cause: Throwable? = null) : Exception(message, cause)