package org.junction.catenin.core

import org.junction.catenin.model.*
import org.junction.catenin.parser.GameDefinitionParser
import kotlin.js.JsExport

@JsExport
class GameEngine private constructor(
    private val gameDefinition: GameDefinition,
    private val players: List<Player>
) {
    companion object {
        fun fromYaml(yamlContent: String, playerNames: List<String>): GameEngine {
            val parser = GameDefinitionParser()
            val definition = parser.parseFromString(yamlContent)
            
            val players = playerNames.mapIndexed { index, name ->
                Player(
                    id = "player_$index",
                    name = name,
                    health = definition.mechanics?.setup?.players?.health ?: 10
                )
            }
            
            return GameEngine(definition, players)
        }
    }
    
    fun getGameDefinition(): GameDefinition = gameDefinition
    
    // JavaScript-friendly method that returns Array by default
    fun getPlayers(): Array<Player> = players.toTypedArray()
    
    // These methods will be implemented in subsequent days
    fun drawCard(playerId: String): ActionResult = TODO("Day 2")
    fun playCard(playerId: String, cardId: String): ActionResult = TODO("Day 2")
    fun getUIState(): UIState = TODO("Day 2")
}

@JsExport
data class ActionResult(
    val success: Boolean,
    val message: String,
    val effects: List<String> = emptyList()
)

@JsExport
data class UIState(
    val players: List<Player>,
    val currentPlayer: String,
    val gamePhase: String
)

// Top-level factory function for JavaScript usage
@JsExport
fun createGameEngineFromYaml(yamlContent: String, playerNames: Array<String>): GameEngine {
    return GameEngine.fromYaml(yamlContent, playerNames.toList())
}