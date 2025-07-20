# High-Level Game Schema Hierarchy

## Overview

This document describes the hierarchy of high-level game schemas that build upon the Universal Game Schema. Each high-level schema provides game-specific abstractions that transpile down to the universal object/property/trigger system.

## Schema Hierarchy

```
UniversalGameSchema (base - everything is objects/properties/triggers)
├── TurnBasedSchema (adds turn/phase management)
│   ├── CardGameSchema (zones, hands, decks)
│   ├── BoardGameSchema (grid/spaces, pieces, movement)
│   └── TacticalRPGSchema (grid combat + story)
├── TimedSchema (real-time or timer-based)
│   ├── PuzzleGameSchema (time limits, combos)
│   └── TowerDefenseSchema (waves, spawning)
└── NarrativeSchema (story/choice focus)
    ├── AdventureGameSchema (rooms, items, puzzles)
    ├── VisualNovelSchema (dialogue trees, branching)
    └── InteractiveFictionSchema (text-based, parser)
```

## Core Design Principles

### 1. **Inheritance Through Composition**
Higher-level schemas don't use traditional OOP inheritance. Instead, they:
- Include all features of their parent schema
- Add new domain-specific abstractions
- Everything ultimately transpiles to UniversalGameSchema

### 2. **Transpilation, Not Runtime**
High-level schemas are compile-time conveniences:
- Author games using familiar concepts (boards, cards, rooms)
- System transpiles to universal triggers and objects
- Runtime only knows about universal concepts

### 3. **Mix-and-Match Capabilities**
Games can combine features from multiple schemas:
- A tactical RPG uses BoardGameSchema + NarrativeSchema
- An adventure card game uses CardGameSchema + AdventureGameSchema

## Common Patterns (Mixins)

### Spatial Games Pattern
Used by: BoardGameSchema, AdventureGameSchema, TacticalRPGSchema
```yaml
spatial_mixin:
  location_system:
    type: "grid" # or "graph", "continuous"
  movement_rules:
    type: "step" # or "pathfinding", "free"
  collision: true
```

### Inventory Pattern
Used by: AdventureGameSchema, RPGSchema, SurvivalGameSchema
```yaml
inventory_mixin:
  containers:
    player_inventory:
      max_weight: 100
      slots: 20
  item_interactions:
    combine: true
    use_on_world: true
    trade: true
```

### Narrative Pattern
Used by: AdventureGameSchema, VisualNovelSchema, InteractiveFictionSchema
```yaml
narrative_mixin:
  dialogue_system:
    branching: true
    state_tracking: true
  story_flags:
    type: "boolean_flags"
  journal:
    track_quests: true
```

### Resource Management Pattern
Used by: CardGameSchema, StrategyGameSchema, TowerDefenseSchema
```yaml
resource_mixin:
  resources:
    - name: "mana"
      regenerate: "per_turn"
    - name: "gold"
      persistent: true
  costs:
    actions_have_cost: true
```

## Schema Validation Approach

Each high-level schema must prove it can represent classic games from its genre:

### BoardGameSchema Validation
- ✅ Chess (pieces, grid movement, capture rules)
- ✅ Checkers (diagonal movement, multi-capture)
- ✅ Go (stone placement, territory control)
- ✅ Monopoly (track movement, property ownership)

### CardGameSchema Validation
- ✅ Poker (hidden hands, betting rounds)
- ✅ Magic: The Gathering (zones, mana, combat)
- ✅ Uno (draw pile, discard, special effects)
- ✅ Hearthstone (digital-first, automatic resolution)

### AdventureGameSchema Validation
- ✅ Monkey Island (point-and-click, inventory puzzles)
- ✅ King's Quest (parser-based, world exploration)
- ✅ Myst (environmental puzzles, no inventory)
- ✅ Phoenix Wright (dialogue-focused, evidence system)

## Implementation Strategy

### Phase 1: Core Schemas
1. **TurnBasedSchema** - Foundation for many game types
2. **BoardGameSchema** - Spatial games with discrete positions
3. **AdventureGameSchema** - Narrative and exploration games

### Phase 2: Specialized Schemas
4. **CardGameSchema** - Zone-based card games
5. **PuzzleGameSchema** - Time/move-limited puzzle games
6. **TacticalRPGSchema** - Grid-based combat with story

### Phase 3: Advanced Schemas
7. **TimedSchema** - Real-time elements
8. **VisualNovelSchema** - Branching narrative focus
9. **TowerDefenseSchema** - Wave-based defense games

## Transpilation Examples

### Simple Turn Phases → Universal Triggers
High-level BoardGameSchema:
```yaml
turn_structure:
  phases: ["move", "attack", "end"]
```

Transpiles to UniversalGameSchema:
```yaml
object_types:
  game_controller:
    properties:
      current_phase: {type: STRING}

triggers:
  - name: "advance_phase"
    when:
      object_type: game_controller
      property_changed: "current_phase"
    effects:
      - modify_property:
          target: {type: game_controller}
          property: "current_phase"
          value: "{next_phase(this.current_phase)}"
```

### Board Spaces → Universal Objects
High-level BoardGameSchema:
```yaml
board:
  type: "grid"
  size: [8, 8]
```

Transpiles to UniversalGameSchema:
```yaml
setup:
  spawn_objects:
    - for: "range(0, 64)"
      type: board_space
      properties:
        x: "{index % 8}"
        y: "{index / 8}"
        color: "{(x + y) % 2 == 0 ? 'white' : 'black'}"
```

## Benefits of This Approach

1. **Authoring Speed**: Game designers use familiar concepts
2. **Learning Curve**: Each schema matches its genre's conventions
3. **Debugging**: Can view both high-level and transpiled versions
4. **Extensibility**: New schemas can be added without changing the core
5. **Validation**: High-level schemas can enforce genre-specific rules

## Next Steps

See the individual schema documentation:
- [BoardGameSchema Design](./board-game-schema.md)
- [AdventureGameSchema Design](./adventure-game-schema.md)
- [CardGameSchema Design](./card-game-schema.md) (Coming soon)