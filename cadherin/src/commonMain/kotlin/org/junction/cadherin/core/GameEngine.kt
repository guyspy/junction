package org.junction.cadherin.core

import org.junction.cadherin.model.*
import org.junction.cadherin.parser.GameDefinitionParser

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
    fun getPlayers(): List<Player> = players
    
    // These methods will be implemented in subsequent days
    fun drawCard(playerId: String): ActionResult = TODO("Day 2")
    fun playCard(playerId: String, cardId: String): ActionResult = TODO("Day 2")
    fun getUIState(): UIState = TODO("Day 2")
}

data class ActionResult(
    val success: Boolean,
    val message: String,
    val effects: List<String> = emptyList()
)

data class UIState(
    val players: List<Player>,
    val currentPlayer: String,
    val gamePhase: String
)