# Cadherin-Catenin Integration Design

## Overview

This document describes how Cadherin (UI/Graphics layer) integrates with Catenin (Universal game logic engine). The integration follows a clean separation of concerns where Catenin handles pure game logic and Cadherin handles all visual representation and user interaction.

## Architecture Pattern

```
Catenin (Game Logic)     ←→     Cadherin (UI/Graphics)
- GameWorld state              - Visual representation  
- Trigger evaluation           - User input handling
- Effect execution             - Animation system
- Property changes             - Sound effects
```

## Core Integration Interfaces

### 1. State Synchronization

```kotlin
// Catenin provides read-only game state
interface GameStateProvider {
    fun getCurrentWorld(): GameWorld
    fun getObjectById(id: String): GameObject?
    fun getObjectsByType(type: String): List<GameObject>
    fun subscribeToChanges(listener: GameStateListener)
}

// Cadherin listens for changes
interface GameStateListener {
    fun onObjectCreated(obj: GameObject)
    fun onObjectDestroyed(objectId: String)
    fun onPropertyChanged(objectId: String, property: String, oldValue: PropertyValue?, newValue: PropertyValue)
    fun onObjectMoved(objectId: String, newParent: String?)
}
```

### 2. Action Input System

```kotlin
// Cadherin sends player actions to Catenin
interface GameActionHandler {
    fun submitAction(participantId: String, action: PlayerAction): ActionResult
    fun getValidActions(participantId: String): List<ActionType>
    fun canPerformAction(participantId: String, action: PlayerAction): Boolean
}

// Example actions from UI
sealed class PlayerAction {
    data class MoveObject(val objectId: String, val targetPosition: String) : PlayerAction()
    data class UseItem(val itemId: String, val targetId: String?) : PlayerAction()
    data class SelectOption(val dialogueNodeId: String, val responseIndex: Int) : PlayerAction()
    data class PlayCard(val cardId: String, val targetId: String?) : PlayerAction()
    data class EndTurn(val participantId: String) : PlayerAction()
}
```

### 3. Visual State Mapping

```kotlin
// Cadherin maps game objects to visual representations
interface VisualMapping {
    fun getVisualType(gameObject: GameObject): VisualType
    fun getPosition(gameObject: GameObject): Position
    fun getVisualState(gameObject: GameObject): VisualState
}

sealed class VisualType {
    data class Sprite(val spriteName: String) : VisualType()
    data class Tile(val tileType: String) : VisualType()
    data class Scene(val backgroundImage: String?) : VisualType()
    data class Character(val appearance: String) : VisualType()
    object Generic : VisualType()
    object Hidden : VisualType()
}
```

## Schema-Specific Integrations

### BoardGameSchema ↔ Board UI

```kotlin
// Catenin: Board state
val board = gameWorld.getObjectsByType("board_space")
val pieces = gameWorld.getObjectsByType("piece")

// Cadherin: Visual board
class BoardRenderer {
    fun renderGrid(spaces: List<GameObject>) {
        spaces.forEach { space ->
            val x = (space.properties["x"] as PropertyValue.IntValue).value
            val y = (space.properties["y"] as PropertyValue.IntValue).value
            val color = (space.properties["color"] as PropertyValue.StringValue).value
            drawTile(x, y, color)
        }
    }
    
    fun renderPieces(pieces: List<GameObject>) {
        pieces.forEach { piece ->
            val position = piece.properties["position"] as PropertyValue.StringValue
            val pieceType = piece.properties["piece_type"] as PropertyValue.StringValue
            val owner = piece.properties["owner"] as PropertyValue.IntValue
            drawPiece(position.value, pieceType.value, owner.value)
        }
    }
    
    fun highlightValidMoves(pieceId: String) {
        val validMoves = gameEngine.getValidMoves(pieceId)
        validMoves.forEach { spaceId ->
            highlightSpace(spaceId, "valid_move")
        }
    }
}

class BoardGameVisualMapping : VisualMapping {
    override fun getVisualType(obj: GameObject) = when (obj.type) {
        "piece" -> VisualType.Sprite(obj.properties["piece_type"]?.toString() ?: "default")
        "board_space" -> VisualType.Tile(obj.properties["color"]?.toString() ?: "white")
        else -> VisualType.Generic
    }
}
```

### AdventureGameSchema ↔ Scene UI

```kotlin
// Catenin: Room state
val currentRoom = gameWorld.objects["current_room"]
val roomItems = gameWorld.getObjectsByParent(currentRoom.id)

// Cadherin: Scene renderer
class SceneRenderer {
    fun renderRoom(room: GameObject) {
        val description = room.properties["description"] as PropertyValue.StringValue
        val background = room.properties["background_image"] as? PropertyValue.StringValue
        val state = room.states["current_state"] as? PropertyValue.StringValue
        
        showScene(background?.value, description.value, state?.value)
    }
    
    fun renderHotspots(items: List<GameObject>) {
        items.forEach { item ->
            val interactive = item.properties["interactive"] as? PropertyValue.BoolValue
            if (interactive?.value == true) {
                val screenPos = item.properties["screen_position"]
                addClickableHotspot(item.id, screenPos)
            }
        }
    }
    
    fun renderInventory(inventory: GameObject) {
        val items = gameWorld.getObjectsByParent(inventory.id)
        items.forEach { item ->
            val icon = item.properties["icon"] as? PropertyValue.StringValue
            drawInventoryItem(item.id, icon?.value)
        }
    }
}

class AdventureGameVisualMapping : VisualMapping {
    override fun getVisualType(obj: GameObject) = when (obj.type) {
        "room" -> VisualType.Scene(obj.properties["background_image"]?.toString())
        "item" -> VisualType.Sprite(obj.properties["sprite_name"]?.toString() ?: "item_generic")
        "character" -> VisualType.Character(obj.properties["appearance"]?.toString() ?: "npc_default")
        else -> VisualType.Hidden
    }
}
```

### CardGameSchema ↔ Zone UI

```kotlin
// Catenin: Zone/card state
val hand = gameWorld.getObjectsByParent("hand_p0")
val battlefield = gameWorld.getObjectsByParent("battlefield")

// Cadherin: Card UI
class CardRenderer {
    fun renderHand(cards: List<GameObject>) {
        cards.forEachIndexed { index, card ->
            val cardData = CardVisual(
                name = card.properties["name"] as PropertyValue.StringValue,
                cost = card.properties["cost"] as PropertyValue.IntValue,
                art = card.properties["card_art"] as? PropertyValue.StringValue,
                playable = canPlayCard(card.id)
            )
            drawCardInHand(index, cardData)
        }
    }
    
    fun renderBattlefield(creatures: List<GameObject>) {
        creatures.forEach { creature ->
            val position = creature.properties["battlefield_position"]
            val tapped = creature.states["tapped"] as? PropertyValue.BoolValue
            drawCreatureCard(creature.id, position, tapped?.value ?: false)
        }
    }
    
    fun renderZones(zones: List<GameObject>) {
        zones.forEach { zone ->
            val zoneType = zone.properties["zone_type"] as PropertyValue.StringValue
            val cardCount = gameWorld.getObjectsByParent(zone.id).size
            drawZone(zoneType.value, cardCount)
        }
    }
}
```

## Technical Integration Patterns

### 1. Event-Driven Architecture

```kotlin
// Catenin publishes events
class GameEventBus {
    private val listeners = mutableMapOf<KClass<*>, MutableList<(Any) -> Unit>>()
    
    fun <T : Any> subscribe(eventType: KClass<T>, handler: (T) -> Unit) {
        listeners.getOrPut(eventType) { mutableListOf() }.add { event ->
            @Suppress("UNCHECKED_CAST")
            handler(event as T)
        }
    }
    
    fun publish(event: Any) {
        listeners[event::class]?.forEach { handler -> handler(event) }
    }
}

// Game events
data class PropertyChangeEvent(
    val objectId: String,
    val propertyPath: String,
    val oldValue: PropertyValue?,
    val newValue: PropertyValue
)

data class ObjectCreatedEvent(val obj: GameObject)
data class ObjectDestroyedEvent(val objectId: String)
data class TriggerFiredEvent(val triggerName: String, val targetObjects: List<String>)

// Cadherin subscribes to relevant events
class UIController(private val eventBus: GameEventBus) {
    init {
        eventBus.subscribe<PropertyChangeEvent> { event ->
            when (event.propertyPath) {
                "properties.position" -> animateMovement(event.objectId, event.newValue)
                "states.tapped" -> showTappedState(event.objectId)
                "properties.health" -> updateHealthBar(event.objectId, event.newValue)
                "properties.mana" -> updateManaDisplay(event.objectId, event.newValue)
            }
        }
        
        eventBus.subscribe<ObjectCreatedEvent> { event ->
            createVisualRepresentation(event.obj)
        }
        
        eventBus.subscribe<TriggerFiredEvent> { event ->
            playTriggerAnimation(event.triggerName, event.targetObjects)
        }
    }
}
```

### 2. Reactive State Management

```kotlin
// Catenin provides reactive state
interface ReactiveGameState {
    fun observeObject(objectId: String): Flow<GameObject?>
    fun observeProperty(objectId: String, property: String): Flow<PropertyValue?>
    fun observeQuery(query: ObjectQuery): Flow<List<GameObject>>
    fun observeGamePhase(): Flow<String>
}

// Cadherin builds reactive UI
class ReactiveUI(private val reactiveState: ReactiveGameState) {
    
    fun bindHealthBar(playerId: String, healthBar: HealthBarComponent) {
        reactiveState.observeProperty(playerId, "health")
            .map { it as? PropertyValue.IntValue }
            .filterNotNull()
            .collect { health -> 
                healthBar.updateHealth(health.value)
                if (health.value <= 0) {
                    healthBar.showDeathAnimation()
                }
            }
    }
    
    fun bindCardHand(playerId: String, handUI: HandUIComponent) {
        reactiveState.observeQuery(ObjectQuery.ByParent("hand_$playerId"))
            .collect { cards ->
                handUI.updateCards(cards.map { CardUIData.from(it) })
            }
    }
    
    fun bindTurnIndicator(turnIndicator: TurnIndicatorComponent) {
        reactiveState.observeProperty("game_controller", "current_player")
            .map { it as? PropertyValue.IntValue }
            .filterNotNull()
            .collect { currentPlayer ->
                turnIndicator.highlightPlayer(currentPlayer.value)
            }
    }
}
```

### 3. Configuration-Driven Rendering

```yaml
# Visual configuration for game schemas
visual_config:
  board_game:
    grid:
      tile_size: 64
      spacing: 2
      highlight_valid_moves: true
      animation_speed: 0.3
    pieces:
      scale: 1.0
      hover_scale: 1.1
      animation_duration: 0.5
      
  adventure_game:
    scenes:
      transition_type: "fade"
      transition_duration: 0.8
      text_speed: "normal"
      auto_advance: false
    inventory:
      layout: "grid"
      max_columns: 4
      item_size: 48
      
  card_game:
    hand:
      max_visible_cards: 10
      card_spacing: 12
      hover_lift: 20
    battlefield:
      creature_rows: 2
      spacing_x: 80
      spacing_y: 120
```

## UI Paradigm Examples

### Point-and-Click Adventure

```kotlin
class PointClickUI(private val gameEngine: GameActionHandler) {
    
    fun onItemClick(itemId: String) {
        when (currentMode) {
            InteractionMode.EXAMINE -> {
                val action = ExamineAction(itemId)
                gameEngine.submitAction(currentPlayer, action)
            }
            InteractionMode.USE -> {
                if (selectedItem != null) {
                    val action = UseItemAction(selectedItem!!, itemId)
                    gameEngine.submitAction(currentPlayer, action)
                } else {
                    selectItem(itemId)
                }
            }
            InteractionMode.TAKE -> {
                val action = TakeItemAction(itemId)
                gameEngine.submitAction(currentPlayer, action)
            }
        }
    }
    
    fun onVerbClick(verb: String) {
        currentMode = when (verb) {
            "look" -> InteractionMode.EXAMINE
            "use" -> InteractionMode.USE
            "take" -> InteractionMode.TAKE
            "talk" -> InteractionMode.TALK
            else -> InteractionMode.EXAMINE
        }
        updateCursor(currentMode)
    }
}
```

### Drag-and-Drop Board Game

```kotlin
class DragDropUI(private val gameEngine: GameActionHandler) {
    
    fun onPieceDragStart(pieceId: String) {
        val validMoves = gameEngine.getValidMoves(currentPlayer, pieceId)
        highlightValidTargets(validMoves)
    }
    
    fun onPieceDragged(pieceId: String, targetSpaceId: String) {
        val action = MovePieceAction(pieceId, targetSpaceId)
        if (gameEngine.canPerformAction(currentPlayer, action)) {
            val result = gameEngine.submitAction(currentPlayer, action)
            if (result.success) {
                animateMovement(pieceId, targetSpaceId)
            } else {
                snapBackToOriginalPosition(pieceId)
                showErrorMessage(result.error)
            }
        } else {
            snapBackToOriginalPosition(pieceId)
        }
        clearHighlights()
    }
}
```

### Card Game Interface

```kotlin
class CardGameUI(private val gameEngine: GameActionHandler) {
    
    fun onCardPlayed(cardId: String, targetId: String?) {
        val action = PlayCardAction(cardId, targetId)
        val result = gameEngine.submitAction(currentPlayer, action)
        
        if (result.success) {
            animateCardPlay(cardId, targetId)
        } else {
            showErrorMessage(result.error)
            returnCardToHand(cardId)
        }
    }
    
    fun onCreatureAttack(attackerId: String, targetId: String) {
        val action = AttackAction(attackerId, targetId)
        val result = gameEngine.submitAction(currentPlayer, action)
        
        if (result.success) {
            animateCombat(attackerId, targetId)
        }
    }
    
    fun onEndTurnClick() {
        val action = EndTurnAction(currentPlayer)
        gameEngine.submitAction(currentPlayer, action)
    }
}
```

## Key Benefits

1. **Clean Separation**: Catenin handles pure game logic, Cadherin handles pure presentation
2. **Multiple UIs**: Same game logic can support different visual styles (mobile vs desktop vs web)
3. **Live Updates**: Real-time synchronization perfect for multiplayer games
4. **Debugging**: Can inspect pure game state separate from visuals using Catenin directly
5. **Platform Independence**: Catenin runs anywhere, Cadherin adapts per platform
6. **Testing**: Game logic can be unit tested without UI dependencies
7. **Performance**: UI only updates when relevant game state changes

## Implementation Notes

1. **State Ownership**: Catenin owns all game state, Cadherin is purely reactive
2. **Action Validation**: All game rules enforced in Catenin, UI just provides interface
3. **Animation Timing**: Cadherin can animate state changes without affecting game logic
4. **Error Handling**: Game logic errors bubble up to UI for user feedback
5. **Save/Load**: Only Catenin's GameWorld needs serialization, UI rebuilds from state

This architecture ensures that game logic remains pure and testable while providing maximum flexibility for different UI implementations across platforms.