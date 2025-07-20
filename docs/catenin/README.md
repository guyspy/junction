# Catenin Documentation

Catenin is a universal game engine that uses a powerful object/property/trigger system to represent any type of game through declarative YAML definitions.

## Core Documentation

### Universal System Design
- **[Universal YAML Schema](./universal-yaml-schema.md)** - The foundational schema that can represent any game
- **[High-Level Game Schemas](./high-level-game-schemas.md)** - Layered architecture with game-specific abstractions

### Specific Game Schemas
- **[BoardGameSchema](./board-game-schema.md)** - For board games and card games (Chess, Magic: The Gathering, etc.)
- **[AdventureGameSchema](./adventure-game-schema.md)** - For narrative games (Monkey Island, Zork, etc.)

### Integration
- **[Cadherin Integration](./cadherin-integration.md)** - How the UI layer connects to game logic

## Key Concepts

### Universal Object System
Everything in Catenin is an **object** with **properties** and **states**:
- Players, cards, board spaces, rooms, items - all just objects
- Properties define static characteristics (health, name, cost)
- States track dynamic runtime values (tapped, selected, current_room)
- Triggers react to property changes and execute effects

### Participant Separation
Following Roblox's design philosophy:
- **Participants**: Abstract users/seats (participant 0, participant 1)
- **Player Objects**: Optional in-game representations with `participant_id`
- Enables one participant controlling multiple objects

### Layered Architecture
```
UniversalGameSchema (base - pure objects/properties/triggers)
├── TurnBasedSchema (adds turn/phase management)
│   ├── BoardGameSchema (grids, pieces, zones)
│   └── AdventureGameSchema (rooms, items, dialogue)
├── TimedSchema (real-time mechanics)
└── NarrativeSchema (story and choice systems)
```

### Transpilation Strategy
High-level schemas compile down to universal primitives:
- Author games using familiar concepts (boards, cards, rooms)
- System transpiles to universal objects and triggers
- Runtime only knows about the universal system

## Game Examples

Each schema includes validation against classic games:

**BoardGameSchema:**
- ✅ Chess (complex pieces, capture rules)
- ✅ Magic: The Gathering (zones, mana, creatures)
- ✅ Monopoly (track movement, properties)

**AdventureGameSchema:**
- ✅ Monkey Island (point-and-click, inventory)
- ✅ Zork (text parser, exploration)
- ✅ Phoenix Wright (dialogue, investigation)

## Implementation Status

### ✅ Completed (Universal System)
- Universal object/property/trigger foundation
- GameWorld immutable state management
- TargetResolver for object targeting
- TriggerEngine for condition matching
- EffectEngine for state changes
- Cross-platform compilation (JVM + JavaScript)

### 📋 Next Steps
- High-level schema compilers (BoardGameSchema → Universal)
- Demo implementations
- Cadherin UI integration
- Additional schemas (PuzzleGameSchema, TacticalRPGSchema)

## Architecture Benefits

1. **Truly Universal**: Any game type through same primitives
2. **AI-Friendly**: Structured YAML for LLM understanding
3. **Educational Focus**: Perfect for learning game development
4. **Cross-Platform**: Same logic runs everywhere
5. **Layered Design**: Familiar authoring, powerful runtime

## Historical Context

See `./archive/` for the evolutionary journey from game-specific models to the current universal approach. This represents a major architectural breakthrough that makes Catenin uniquely powerful.

---

*Catenin: Where any game is just objects, properties, and the changes between them.* 🎮