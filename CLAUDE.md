# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Junction is an educational gaming platform monorepo featuring multiple services:

- **Cadherin**: Kotlin Multiplatform game engine for creating 2D card-based educational games
- **Future Services**: Occludin (Quarkus server), Phaser renderers, etc.

The platform uses YAML DSL for game definitions and is optimized for AI Agent driven game creation. Each service is technology-independent with its own build system.

## Commands

### Development Commands

**Monorepo Commands:**
```bash
# Build all Gradle-based services
./gradlew build

# Show all projects in monorepo
./gradlew projects
```

**Cadherin Service Commands:**
```bash
# Build Cadherin (JVM + JS)
./gradlew :cadherin:build

# Run tests
./gradlew :cadherin:test

# Run JVM command-line demo
./gradlew :cadherin:examples:jvm-cli-demo:run

# Run JS browser demo
./gradlew :cadherin:examples:js-browser-demo:jsBrowserRun

# Run JS Node.js demo
./gradlew :cadherin:examples:js-node-demo:jsNodeRun

# Generate JavaScript module for frontend
./gradlew :cadherin:jsBrowserDistribution

# Generate TypeScript definitions
./gradlew :cadherin:jsTypeScriptDeclarations
```

**Future Service Commands:**
```bash
# Occludin (Quarkus) - when added
./gradlew :occludin:quarkusDev

# Pure JS services use their own tooling (npm, etc.)
```

### Testing Commands
```bash
# Run platform-specific tests
./gradlew :cadherin:jvmTest      # JVM tests
./gradlew :cadherin:jsTest       # JavaScript tests

# Run with coverage
./gradlew :cadherin:test :cadherin:jacocoTestReport
```

## Architecture & Components

### Monorepo Structure
Junction is organized as a multi-technology monorepo where each service is independent:

### Cadherin Service (Kotlin Multiplatform)
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
├── examples/
│   ├── jvm-cli-demo/               # JVM command-line demo
│   ├── js-browser-demo/            # JS browser demo
│   └── js-node-demo/               # JS Node.js demo
└── game-samples/                   # YAML game definitions
```

### Technology Stack

**Monorepo Architecture:**
- **Build Systems**: Gradle (Kotlin/Java services), npm (JS services), Maven (optional)
- **Service Independence**: Each service uses appropriate technology stack
- **Coordination**: Shared version catalog, unified documentation

**Cadherin Service:**
- **Core**: Kotlin Multiplatform
- **Frontend**: Kotlin/JS compiles to TypeScript-compatible JavaScript
- **Backend**: Kotlin/JVM 
- **DSL Format**: YAML (AI-friendly structure)
- **Development**: Test-driven development

**Future Services:**
- **Occludin**: Quarkus + Java + MongoDB
- **Renderers**: Pure JS/TS + Phaser/Three.js
- **APIs**: GraphQL, REST, WebSocket

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

**Current Phase**: ✅ MVP + MONOREPO READY!
- Day 1: ✅ Kotlin Multiplatform setup + YAML parsing
- Day 2: ✅ Player state and actions  
- Day 3: ✅ Event system implementation (card effects)
- Day 4: ✅ Turn management and scoring
- Day 5: ✅ Win conditions and complete game
- Day 1.5: ✅ SDK monorepo restructure with examples
- **Monorepo Config**: ✅ Multi-service ready (Kotlin, Java, JS)

**🎉 MVP完成！** 現在有了完整的多服務 monorepo 架構，準備好添加新服務

## Key Files

- `/docs/cadherin/README.md` - Overall project design
- `/docs/cadherin/day1-kotlin-multiplatform-setup.md` - Day 1 implementation
- `/docs/cadherin/day2-player-state-and-actions.md` - Day 2 implementation  
- `/docs/cadherin/day3-event-system.md` - Day 3 implementation
- `/docs/cadherin/day4-turn-management-and-scoring.md` - Day 4 implementation
- `/docs/cadherin/day5-win-conditions-and-complete-game.md` - Day 5 implementation
- `/game-samples/` - Example YAML game definitions

The project prioritizes practical functionality over architectural complexity, with a focus on creating a working cross-platform game engine that AI agents can easily understand and modify.