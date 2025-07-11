package org.junction.catenin.core

import org.junction.catenin.model.*
import org.junction.catenin.actions.*
import org.junction.catenin.parser.GameDefinitionParser
import org.junction.catenin.utils.GameRandom
import kotlin.js.JsExport

@JsExport
class GameEngine private constructor(
    private var gameState: GameState
) {
    companion object {
        fun fromYaml(yamlContent: String, playerNames: List<String>): GameEngine {
            val parser = GameDefinitionParser()
            val definition = parser.parseFromString(yamlContent)
            
            val gameState = initializeGame(definition, playerNames)
            return GameEngine(gameState)
        }
        
        fun loadGameState(json: String): GameEngine {
            val gameState = kotlinx.serialization.json.Json.decodeFromString(GameState.serializer(), json)
            return GameEngine(gameState)
        }
        
        fun loadGameStateFromJson(json: String): GameEngine = loadGameState(json)
        
        private fun initializeGame(definition: GameDefinition, playerNames: List<String>): GameState {
            // Generate all cards
            val cardFactory = CardFactory.fromDefinition(definition)
            val allCards = cardFactory.generateCards().toMutableList()
            GameRandom.shuffle(allCards)
            
            // Create players
            val defaultHealth = definition.mechanics?.setup?.players?.health ?: 10
            val handSize = definition.mechanics?.setup?.players?.handSize ?: 5
            
            val players = playerNames.mapIndexed { index, name ->
                Player(
                    id = "player_$index",
                    name = name,
                    health = defaultHealth
                )
            }
            
            // Deal initial hand to each player (immutable approach)
            val playersWithCards = players.map { player ->
                var updatedPlayer = player
                repeat(handSize) {
                    if (allCards.isNotEmpty()) {
                        val card = allCards.removeFirst()
                        updatedPlayer = updatedPlayer.addCard(card)
                    }
                }
                updatedPlayer
            }
            
            return GameState(
                gameId = GameRandom.generateGameId(),
                definition = definition,
                players = playersWithCards.toTypedArray(),
                deck = allCards.toTypedArray(),
                discardPile = emptyArray(),
                currentPlayerIndex = 0,
                gamePhase = GamePhase.PLAYING,
                turnNumber = 1
            )
        }
    }
    
    fun processAction(action: PlayerAction): ActionResult {
        // Validate action first
        val validation = isValidAction(action)
        if (!validation.isValid) {
            return ActionResult.failure(getActionType(action), validation.errors)
        }
        
        return when (action) {
            is PlayerAction.DrawCard -> processDrawCard(action)
            is PlayerAction.PlayCard -> processPlayCard(action)
            is PlayerAction.EndTurn -> processEndTurn(action)
        }
    }
    
    fun isValidAction(action: PlayerAction): ValidationResult {
        // Use structured validation internally, convert to legacy format for API compatibility
        return isValidActionStructured(action).toLegacy()
    }
    
    private fun isValidActionStructured(action: PlayerAction): StructuredValidationResult {
        val player = gameState.getPlayer(action.playerId)
            ?: return StructuredValidationResult.invalid(GameError.PlayerNotFound(action.playerId))
        
        if (gameState.getCurrentPlayer().id != action.playerId) {
            return StructuredValidationResult.invalid(
                GameError.NotPlayerTurn(action.playerId, gameState.getCurrentPlayer().id)
            )
        }
        
        if (gameState.isGameOver()) {
            return StructuredValidationResult.invalid(
                GameError.GameAlreadyOver(gameState.gamePhase.name)
            )
        }
        
        return when (action) {
            is PlayerAction.DrawCard -> validateDrawCardStructured(player)
            is PlayerAction.PlayCard -> validatePlayCardStructured(player, action.cardId)
            is PlayerAction.EndTurn -> StructuredValidationResult.valid()
        }
    }
    
    private fun validateDrawCard(player: Player): ValidationResult {
        return validateDrawCardStructured(player).toLegacy()
    }
    
    private fun validateDrawCardStructured(player: Player): StructuredValidationResult {
        // TODO: Move to YAML config - deck behavior rules 
        if (gameState.getDeckSize() == 0) {
            return StructuredValidationResult.invalid(GameError.DeckEmpty(player.id))
        }
        // TODO: Move to YAML config - hand size constraints
        if (player.hand.size >= 10) { // Hard-coded hand limit
            return StructuredValidationResult.invalid(
                GameError.HandFull(player.id, player.hand.size, 10)
            )
        }
        return StructuredValidationResult.valid()
    }
    
    private fun validatePlayCard(player: Player, cardId: String): ValidationResult {
        return validatePlayCardStructured(player, cardId).toLegacy()
    }
    
    private fun validatePlayCardStructured(player: Player, cardId: String): StructuredValidationResult {
        if (!player.hasCard(cardId)) {
            return StructuredValidationResult.invalid(
                GameError.CardNotInHand(cardId, player.id)
            )
        }
        return StructuredValidationResult.valid()
    }
    
    private fun processDrawCard(action: PlayerAction.DrawCard): ActionResult {
        val player = gameState.getPlayer(action.playerId)!!
        val drawnCard = gameState.deck.first()
        
        // Update player with new card (immutable)
        val updatedPlayer = player.addCard(drawnCard)
        
        // Update game state with new player and reduced deck
        gameState = gameState
            .withUpdatedPlayer(updatedPlayer)
            .withRemovedFromDeck(1)
        
        val effect = GameEffect(
            type = EffectType.CARD_DRAWN,
            targetPlayerId = action.playerId,
            description = "Drew a card from deck"
        )
        
        return ActionResult.success(ActionType.DRAW_CARD, arrayOf(effect))
    }
    
    private fun processPlayCard(action: PlayerAction.PlayCard): ActionResult {
        val player = gameState.getPlayer(action.playerId)!!
        val (updatedPlayer, card) = player.removeCard(action.cardId)
        
        if (card == null) {
            return ActionResult.failure(ActionType.PLAY_CARD, arrayOf("Card not found: ${action.cardId}"))
        }
        
        // Update game state with new player and card in discard pile
        gameState = gameState
            .withUpdatedPlayer(updatedPlayer)
            .withAddedToDiscard(arrayOf(card))
        
        val effect = GameEffect(
            type = EffectType.CARD_PLAYED,
            targetPlayerId = action.playerId,
            sourceCardId = action.cardId,
            description = "Played card: ${card.type}"
        )
        
        return ActionResult.success(ActionType.PLAY_CARD, arrayOf(effect))
    }
    
    private fun processEndTurn(action: PlayerAction.EndTurn): ActionResult {
        gameState = gameState.withNextPlayer()
        
        val effect = GameEffect(
            type = EffectType.TURN_ENDED,
            targetPlayerId = action.playerId,
            description = "Turn ended"
        )
        
        return ActionResult.success(ActionType.END_TURN, arrayOf(effect))
    }
    
    private fun getActionType(action: PlayerAction): ActionType {
        return when (action) {
            is PlayerAction.DrawCard -> ActionType.DRAW_CARD
            is PlayerAction.PlayCard -> ActionType.PLAY_CARD
            is PlayerAction.EndTurn -> ActionType.END_TURN
        }
    }
    
    fun getGameState(): GameState = gameState
    
    fun getCurrentPlayer(): Player = gameState.getCurrentPlayer()
    
    fun getUIState(): UIState {
        return UIState(
            players = gameState.players,
            currentPlayerId = gameState.getCurrentPlayer().id,
            gamePhase = gameState.gamePhase.name,
            turnNumber = gameState.turnNumber,
            deckSize = gameState.getDeckSize(),
            discardPileSize = gameState.getDiscardSize()
        )
    }
    
    fun getGameDefinition(): GameDefinition = gameState.definition
    
    // JavaScript-friendly method that returns Array by default
    fun getPlayers(): Array<Player> = gameState.players
    
    // Query methods for common validation checks
    fun canPlayerDrawCard(playerId: String): Boolean {
        val validation = isValidActionStructured(PlayerAction.DrawCard(playerId))
        return validation.isValid
    }
    
    fun canPlayerPlayCard(playerId: String, cardId: String): Boolean {
        val validation = isValidActionStructured(PlayerAction.PlayCard(playerId, cardId))
        return validation.isValid
    }
    
    fun canPlayerEndTurn(playerId: String): Boolean {
        val validation = isValidActionStructured(PlayerAction.EndTurn(playerId))
        return validation.isValid
    }
    
    fun getPlayableCards(playerId: String): Array<Card> {
        val player = gameState.getPlayer(playerId) ?: return emptyArray()
        if (gameState.getCurrentPlayer().id != playerId) return emptyArray()
        
        return player.hand.filter { card ->
            canPlayerPlayCard(playerId, card.id)
        }.toTypedArray()
    }
    
    fun getPlayerHandSize(playerId: String): Int {
        return gameState.getPlayer(playerId)?.hand?.size ?: 0
    }
    
    fun getPlayerHealth(playerId: String): Int {
        return gameState.getPlayer(playerId)?.health ?: 0
    }
    
    fun getPlayerScore(playerId: String): Int {
        return gameState.getPlayer(playerId)?.score ?: 0
    }
    
    fun isPlayerAlive(playerId: String): Boolean {
        return gameState.getPlayer(playerId)?.isAlive() ?: false
    }
    
    fun isPlayerTurn(playerId: String): Boolean {
        return gameState.getCurrentPlayer().id == playerId
    }
    
    fun getRemainingDeckSize(): Int = gameState.getDeckSize()
    
    fun getDiscardPileSize(): Int = gameState.getDiscardSize()
    
    fun getTurnNumber(): Int = gameState.turnNumber
    
    fun getGamePhase(): String = gameState.gamePhase.name
    
    // Game state serialization support
    fun saveGameState(): String {
        return kotlinx.serialization.json.Json.encodeToString(GameState.serializer(), gameState)
    }
    
    fun saveGameStateToJson(): String = saveGameState()
}

@JsExport
data class UIState(
    val players: Array<Player>,
    val currentPlayerId: String,
    val gamePhase: String,
    val turnNumber: Int,
    val deckSize: Int,
    val discardPileSize: Int
)

// Top-level factory function for JavaScript usage
@JsExport
fun createGameEngineFromYaml(yamlContent: String, playerNames: Array<String>): GameEngine {
    return GameEngine.fromYaml(yamlContent, playerNames.toList())
}

// Top-level serialization functions for JavaScript usage
@JsExport
fun saveGameEngineState(engine: GameEngine): String {
    return engine.saveGameState()
}

@JsExport
fun loadGameEngineFromState(json: String): GameEngine {
    return GameEngine.loadGameState(json)
}