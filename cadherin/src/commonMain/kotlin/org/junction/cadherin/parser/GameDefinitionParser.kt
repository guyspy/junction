package org.junction.cadherin.parser

import org.junction.cadherin.model.GameDefinition
import org.junction.cadherin.platform.readPlatformFile

class GameDefinitionParser {
    private val yamlParser = YamlParser()
    
    fun parseFromString(yamlContent: String): GameDefinition {
        return yamlParser.parseGameDefinition(yamlContent)
    }
    
    fun parseFromFile(filePath: String): GameDefinition {
        val content = readFileContent(filePath)
        return parseFromString(content)
    }
    
    fun validate(definition: GameDefinition): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Basic validation
        if (definition.meta.name.isBlank()) {
            errors.add("Game name cannot be empty")
        }
        
        if (definition.cards.isEmpty()) {
            errors.add("Game must define at least one card type")
        }
        
        // Validate card definitions
        definition.cards.forEach { (cardType, cardDef) ->
            if (cardDef.count <= 0) {
                errors.add("Card type '$cardType' must have count > 0")
            }
            
            if (cardDef.properties.isEmpty()) {
                errors.add("Card type '$cardType' must have at least one property")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    private fun readFileContent(filePath: String): String {
        return readPlatformFile(filePath)
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<String>) : ValidationResult()
}

// Platform-specific file reading, implemented in each platform
expect fun readPlatformFile(filePath: String): String