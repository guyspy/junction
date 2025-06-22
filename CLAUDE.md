# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Junction is an educational gaming platform featuring **Cadherin**, a Kotlin Multiplatform game engine for creating 2D card-based educational games. The platform uses YAML DSL for game definitions and is optimized for AI Agent driven game creation.

## Commands

### Development Commands
```bash
# Build all platforms (JVM + JS)
./gradlew build

# Run tests
./gradlew test

# Run JVM command-line demo
./gradlew jvmRun

# Generate JavaScript module for frontend
./gradlew jsBrowserDistribution

# Generate TypeScript definitions
./gradlew jsTypeScriptDeclarations
```

### Testing Commands
```bash
# Run platform-specific tests
./gradlew jvmTest      # JVM tests
./gradlew jsTest       # JavaScript tests

# Run with coverage
./gradlew test jacocoTestReport
```

## Architecture & Components

### Cadherin Engine (Kotlin Multiplatform)
The core game engine that compiles to both JVM and JavaScript:

```
cadherin/
├── src/
│   ├── commonMain/kotlin/           # Shared game logic
│   │   ├── model/                   # Game data models
│   │   ├── parser/                  # YAML parsing
│   │   ├── core/                    # Game engine
│   │   └── actions/                 # Player actions
│   ├── jvmMain/kotlin/             # JVM-specific (server)
│   │   ├── cli/                    # Command-line interface
│   │   └── platform/               # File I/O
│   └── jsMain/kotlin/              # JS-specific (frontend)
│       └── platform/               # Browser APIs
```

### Technology Stack
- **Core**: Kotlin Multiplatform
- **Frontend**: Kotlin/JS compiles to TypeScript-compatible JavaScript
- **Backend**: Kotlin/JVM with Quarkus
- **Database**: MongoDB (stores YAML as JSON)
- **DSL Format**: YAML (AI-friendly structure)
- **Development**: Test-driven development

### Data Architecture
- **GameDefinition**: YAML-defined game rules and cards
- **GameState**: Runtime game state (players, deck, actions)
- **GameEngine**: Core logic engine (shared between frontend/backend)

## Development Philosophy

### AI-First Design
The engine prioritizes AI Agent usability:
- **YAML DSL**: Structured format AI can easily understand and modify
- **Semantic naming**: Clear, descriptive property names
- **AI Hints system**: Built-in guidance for AI modifications
- **Unified API**: Same game logic on frontend and backend

### Incremental Development
Each day delivers a working game:
- **Day 1**: YAML parsing + basic card display
- **Day 2**: Player actions (draw, play cards)
- **Day 3**: Event system (card effects)
- **Day 4**: Turn management and scoring
- **Day 5**: Win conditions and complete game

### Key Design Decisions

#### Event System Simplification (Day 3)
Decided on minimal viable event system:
- **Triggers**: Only `on_play` events initially
- **Targets**: Fixed targets (`self`, `opponent`, `all_opponents`)
- **Parameters**: Simple `{property}` substitution
- **Display**: Clear command-line effect visualization

Example:
```yaml
fire_spell:
  properties:
    damage: {type: int, min: 2, max: 5}
  events:
    on_play:
      action: "deal_damage"
      target: "opponent"
      amount: "{damage}"
```

## Frontend Integration

JavaScript compilation provides TypeScript-compatible API:

```typescript
import { GameEngine, PlayerAction } from './cadherin-core'

// Create game from YAML
const engine = GameEngine.fromYaml(yamlContent, playerNames)

// Process player actions  
const result = engine.processAction({
  type: 'PlayCard',
  playerId: 'player_0', 
  cardId: 'fire_spell_1'
})

// Get UI state for rendering
const uiState = engine.getUIState()
```

## Backend Integration

JVM version provides server functionality:

```kotlin
// Same engine, server context
val engine = GameEngine.fromYaml(yamlContent, playerIds)
val result = engine.processAction(playerAction)

// Save to MongoDB
mongoAdapter.saveGameState(engine.getGameState())
```

## AI Agent Integration

The engine provides AI-friendly modification points:

```yaml
ai_hints:
  difficulty_factors: 
    - "cards.attack_card.properties.damage.max"
    - "mechanics.setup.players.health"
  common_modifications:
    easier: {damage_max: 3, health: 20}
    harder: {damage_max: 7, health: 10}
```

## Game Definition Format

Standard YAML structure for game definitions:

```yaml
meta:
  name: "Game Name"
  target_age: [8, 12]
  player_count: [2, 4]

cards:
  card_type_name:
    count: 10
    properties:
      property_name: {type: int, min: 1, max: 5}
    events:
      on_play:
        action: "action_name"
        target: "target_type"

mechanics:
  setup:
    players:
      health: 10
      hand_size: 5
  win_conditions:
    - type: "health_depleted"
      message: "{winner} wins!"

ai_hints:
  difficulty_factors: [list of YAML paths]
  common_modifications:
    easier: {modifications}
    harder: {modifications}
```

## Testing Approach

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test complete game flows
- **Cross-Platform**: Same tests run on JVM and JS
- **Game Scenarios**: Test actual gameplay sequences

## Development Status

**Current Phase**: ✅ MVP COMPLETED! (Week 1)
- Day 1: ✅ Kotlin Multiplatform setup + YAML parsing
- Day 2: ✅ Player state and actions  
- Day 3: ✅ Event system implementation (card effects)
- Day 4: ✅ Turn management and scoring
- Day 5: ✅ Win conditions and complete game

**🎉 MVP完成！** 現在有了完整可用的跨平台卡牌遊戲引擎

## Key Files

- `/docs/cadherin/README.md` - Overall project design
- `/docs/cadherin/day1-kotlin-multiplatform-setup.md` - Day 1 implementation
- `/docs/cadherin/day2-player-state-and-actions.md` - Day 2 implementation  
- `/docs/cadherin/day3-event-system.md` - Day 3 implementation
- `/docs/cadherin/day4-turn-management-and-scoring.md` - Day 4 implementation
- `/docs/cadherin/day5-win-conditions-and-complete-game.md` - Day 5 implementation
- `/game-samples/` - Example YAML game definitions

The project prioritizes practical functionality over architectural complexity, with a focus on creating a working cross-platform game engine that AI agents can easily understand and modify.