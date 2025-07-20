# BoardGameSchema Design

## Overview

BoardGameSchema is a high-level schema for creating board games and card games that feature:
- Discrete game boards (grids, graphs, or tracks)
- Game pieces with movement rules
- Turn-based gameplay with phases
- Spatial relationships between game elements

This schema extends TurnBasedSchema and transpiles to UniversalGameSchema.

## Key Concepts

### 1. **Board Definition**
The game board can be represented in multiple ways:
- **Grid**: Rectangular grid (Chess, Checkers, Go)
- **Graph**: Connected nodes (Risk, Pandemic)
- **Track**: Linear or circular paths (Monopoly, Sorry!)
- **Hex**: Hexagonal grid (Settlers of Catan)

### 2. **Pieces**
Game pieces that exist on the board with:
- Position (which space they occupy)
- Movement rules (how they can move)
- Capture rules (how they interact with other pieces)
- Special abilities

### 3. **Spaces**
Board locations that can:
- Hold pieces
- Have special properties (safe zones, teleports)
- Trigger effects when landed on

## Schema Structure

```yaml
# BoardGameSchema YAML Structure
board_game:
  extends: turn_based  # Inherits all turn-based features
  
  # Board Definition
  board:
    type: "grid"  # grid, graph, track, hex
    dimensions: [8, 8]  # for grid/hex
    
    # For graph boards
    nodes:
      - id: "start"
        connections: ["path1", "path2"]
        
    # For track boards  
    track:
      spaces: 40
      loop: true  # Circular track like Monopoly
      
    # Special spaces
    special_spaces:
      - position: [0, 0]
        type: "safe_zone"
        effect: "protect_from_capture"
        
  # Piece Definitions
  pieces:
    pawn:
      movement:
        type: "step"
        directions: ["forward"]
        distance: 1
        first_move: 2  # Special first move
      capture:
        type: "diagonal"
        distance: 1
        
    knight:
      movement:
        type: "L-shape"
        
    # Card game example - cards as pieces
    creature_card:
      placement: "battlefield_zone"
      states:
        tapped: false
        summoning_sick: true
        
  # Setup
  setup:
    # Initial piece placement
    initial_positions:
      - piece: "pawn"
        player: 0
        positions: [[0,1], [1,1], [2,1], [3,1], [4,1], [5,1], [6,1], [7,1]]
      - piece: "king"
        player: 0
        position: [4,0]
        
    # For card games
    zones:
      - name: "deck"
        per_player: true
        visibility: "owner"
        shuffle: true
      - name: "hand"
        per_player: true
        visibility: "owner"
        max_size: 7
      - name: "battlefield"
        shared: true
        visibility: "all"
        
  # Movement System
  movement_rules:
    must_move: false  # Required to move each turn
    capture_required: false  # Must capture if possible
    
  # Win Conditions
  win_conditions:
    - type: "capture"
      target: "king"
    - type: "reach_position"
      piece: "pawn"
      position: "opponent_back_rank"
    - type: "eliminate_all"
      piece_type: "any"
```

## Transpilation to UniversalGameSchema

### Board Spaces → Objects
```yaml
# High-level
board:
  type: "grid"
  dimensions: [8, 8]

# Transpiles to:
object_types:
  board_space:
    properties:
      x: {type: INT}
      y: {type: INT}
      color: {type: STRING}
      occupied_by: {type: STRING}  # piece ID
    states:
      highlighted: {type: BOOL, initial: false}
      threatened: {type: BOOL, initial: false}

setup:
  spawn_objects:
    - for: "range(0, 64)"
      type: board_space
      id: "space_{x}_{y}"
      properties:
        x: "{index % 8}"
        y: "{Math.floor(index / 8)}"
        color: "{((x + y) % 2 == 0) ? 'white' : 'black'}"
```

### Pieces → Objects
```yaml
# High-level
pieces:
  pawn:
    movement:
      type: "step"
      directions: ["forward"]
      distance: 1

# Transpiles to:
object_types:
  piece:
    properties:
      piece_type: {type: STRING}
      owner: {type: INT}
      position: {type: STRING}  # space ID
      has_moved: {type: BOOL, initial: false}
    states:
      selected: {type: BOOL, initial: false}
      can_move: {type: BOOL, initial: false}

# Movement validation trigger
triggers:
  - name: "validate_pawn_move"
    when:
      object_type: piece
      property_changed: "position"
      condition: "this.piece_type == 'pawn'"
    effects:
      - conditional:
          condition: "!is_valid_pawn_move(old_position, new_position, this.owner)"
          effects:
            - revert_property_change:
                target: {id: "this"}
                property: "position"
```

### Turn Phases → Game Controller
```yaml
# High-level (inherited from turn_based)
phases:
  - name: "move"
    actions_allowed: ["move_piece", "castle"]
  - name: "check_win"
    auto_advance: true

# Transpiles to:
object_types:
  game_controller:
    properties:
      current_phase: {type: STRING, initial: "move"}
      current_player: {type: INT, initial: 0}
      
triggers:
  - name: "enforce_phase_actions"
    when:
      object_type: action_request
      property_changed: "status"
      new_value: "pending"
    effects:
      - conditional:
          condition: "!phase_allows_action(game.current_phase, this.action_type)"
          effects:
            - modify_property:
                target: {id: "this"}
                property: "status"
                value: "rejected"
                reason: "Action not allowed in current phase"
```

### Card Game Zones → Containers
```yaml
# High-level
zones:
  - name: "hand"
    per_player: true
    visibility: "owner"
    max_size: 7

# Transpiles to:
object_types:
  container:
    properties:
      zone_type: {type: STRING}
      owner: {type: INT}
      visibility: {type: STRING}
      max_size: {type: INT}
      current_size: {type: INT, initial: 0}

setup:
  spawn_objects:
    - for_each: participant
      type: container
      id: "hand_p{participant_id}"
      properties:
        zone_type: "hand"
        owner: "{participant_id}"
        visibility: "owner"
        max_size: "7"

triggers:
  - name: "enforce_hand_limit"
    when:
      object_type: card
      property_changed: "parent"
      condition: "target.zone_type == 'hand'"
    effects:
      - conditional:
          condition: "count_children(target) > target.max_size"
          effects:
            - force_discard:
                player: "{target.owner}"
                count: "{count_children(target) - target.max_size}"
```

## Example Games

### Chess
```yaml
board_game:
  board:
    type: "grid"
    dimensions: [8, 8]
    
  pieces:
    king:
      movement:
        type: "step"
        directions: ["all"]
        distance: 1
      special_rules: ["cannot_move_into_check", "castle"]
      
    queen:
      movement:
        type: "slide"
        directions: ["all"]
        distance: "unlimited"
```

### Magic: The Gathering (Card Game)
```yaml
board_game:
  zones:
    - name: "library"
      per_player: true
      visibility: "hidden"
      shuffle: true
    - name: "hand"
      per_player: true
      visibility: "owner"
    - name: "battlefield"
      shared: true
      visibility: "all"
    - name: "graveyard"
      per_player: true
      visibility: "all"
      
  pieces:  # Cards are pieces
    creature:
      placement: "battlefield"
      properties:
        power: {type: INT}
        toughness: {type: INT}
      states:
        tapped: false
        summoning_sick: true
```

### Monopoly (Track Board)
```yaml
board_game:
  board:
    type: "track"
    spaces: 40
    loop: true
    
    special_spaces:
      - position: 0
        name: "GO"
        effect: "collect_200"
      - position: 10
        name: "Jail"
        
  pieces:
    token:
      movement:
        type: "dice_roll"
        dice: "2d6"
```

## Key Benefits

1. **Genre-Familiar**: Uses terminology board game designers know
2. **Flexible**: Supports many board game types
3. **Card Game Support**: Zones make it perfect for card games too
4. **Clear Transpilation**: Each concept maps cleanly to universal objects
5. **Extensible**: Easy to add new piece types or board layouts

## Validation Checklist

- ✅ Can represent Chess (complex pieces, grid board)
- ✅ Can represent Checkers (capture chains, promotion)
- ✅ Can represent Go (stone placement, territory)
- ✅ Can represent Monopoly (track movement, properties)
- ✅ Can represent Magic: The Gathering (zones, card states)
- ✅ Can represent Hearthstone (digital card game)

## Implementation Notes

1. **Movement Validation**: Complex movement rules transpile to validation triggers
2. **Capture Logic**: Handled through triggers on position changes
3. **Zone Management**: Containers with properties handle all zone logic
4. **Turn Structure**: Inherited from TurnBasedSchema
5. **Win Conditions**: Specific patterns transpile to victory triggers