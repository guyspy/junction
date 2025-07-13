package org.junction.catenin.parser

import org.junction.catenin.model.GameDefinition
import kotlin.js.JsExport

@JsExport
class GameDefinitionParser {
    private val yamlParser = YamlParser()
    
    fun parseFromString(yamlContent: String): GameDefinition {
        return yamlParser.parseGameDefinition(yamlContent)
    }
    
    
    fun validate(definition: GameDefinition): ParseResult {
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
            ParseResult.Success
        } else {
            ParseResult.Failure(errors)
        }
    }
    
}

@JsExport
sealed class ParseResult {
    object Success : ParseResult()
    data class Failure(val errors: List<String>) : ParseResult()
}

