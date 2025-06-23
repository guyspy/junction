package org.junction.cadherin.application

import org.junction.cadherin.core.GameEngine
import org.junction.cadherin.parser.GameDefinitionParser
import org.junction.cadherin.parser.ValidationResult
import java.io.File

/**
 * Application-level service for loading games from files.
 * This handles file I/O and delegates game logic to the core engine.
 */
class GameLoader {
    private val parser = GameDefinitionParser()
    
    /**
     * Load a game from a YAML file path.
     * @param filePath Path to the YAML file
     * @param playerNames List of player names
     * @return GameEngine instance
     * @throws GameLoadException if file cannot be read or parsed
     */
    fun loadFromFile(filePath: String, playerNames: List<String>): GameEngine {
        try {
            val yamlContent = File(filePath).readText()
            return loadFromContent(yamlContent, playerNames)
        } catch (e: Exception) {
            throw GameLoadException("Failed to load game from file '$filePath': ${e.message}", e)
        }
    }
    
    /**
     * Load a game from YAML content string.
     * @param yamlContent YAML content as string
     * @param playerNames List of player names
     * @return GameEngine instance
     * @throws GameLoadException if content cannot be parsed
     */
    fun loadFromContent(yamlContent: String, playerNames: List<String>): GameEngine {
        try {
            // Validate the game definition first
            val definition = parser.parseFromString(yamlContent)
            when (val validationResult = parser.validate(definition)) {
                is ValidationResult.Failure -> {
                    throw GameLoadException("Game definition validation failed: ${validationResult.errors.joinToString(", ")}")
                }
                ValidationResult.Success -> {
                    // Validation passed, create the game engine
                    return GameEngine.fromYaml(yamlContent, playerNames)
                }
            }
        } catch (e: GameLoadException) {
            throw e
        } catch (e: Exception) {
            throw GameLoadException("Failed to load game from content: ${e.message}", e)
        }
    }
}

/**
 * Exception thrown when game loading fails
 */
class GameLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)