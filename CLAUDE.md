# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Junction is an educational gaming platform monorepo featuring multiple services:

- **Catenin**: Kotlin Multiplatform game engine for creating 2D card-based educational games
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

**Catenin Service Commands:**
```bash
# Build Catenin (JVM + JS)
./gradlew :catenin:build

# Run tests
./gradlew :catenin:allTests

# Run JVM command-line demo
./gradlew :catenin:examples:jvm-cli-demo:run

# Run JS browser demo
./gradlew :catenin:examples:js-browser-demo:serve

# Run Kotlin/JS Node.js demo
./gradlew :catenin:examples:kotlin-js-node-demo:jsNodeDevelopmentRun

# Run TypeScript server demo
cd catenin/examples/typescript-node-demo && npm start

# Note: This project uses npm (not yarn) for JavaScript dependencies.
# If you encounter package lock issues, use:
# ./gradlew :catenin:examples:kotlin-js-node-demo:jsNodeDevelopmentRun -x kotlinStorePackageLock

# Generate JavaScript library files
./gradlew :catenin:jsBrowserDevelopmentLibraryDistribution

# Create NPM package
./gradlew :catenin:createNpmPackage

# Pack NPM package for distribution
./gradlew :catenin:packNpmPackage
```

**Future Service Commands:**
```bash
# Occludin (Quarkus) - when added
./gradlew :occludin:quarkusDev

# Pure JS services use their own tooling (npm, etc.)
```

### Testing Commands
```bash
# Run all tests
./gradlew :catenin:allTests

# Run platform-specific tests
./gradlew :catenin:jvmTest        # JVM tests
./gradlew :catenin:jsTest         # All JavaScript tests

# Run JavaScript tests by environment
./gradlew :catenin:jsNodeTest     # Node.js environment (library API tests)
./gradlew :catenin:jsBrowserTest  # Browser environment (headless)

# Run with coverage
./gradlew :catenin:test :catenin:jacocoTestReport
```

## Architecture & Components

### Monorepo Structure
Junction is organized as a multi-technology monorepo where each service is independent:

### Catenin Service (Kotlin Multiplatform)
The core game engine that compiles to both JVM and JavaScript:

```
catenin/
├── src/
│   ├── commonMain/kotlin/           # Shared game logic
│   │   ├── model/                   # Game data models
│   │   ├── parser/                  # YAML parsing
│   │   ├── core/                    # Game engine
│   │   └── actions/                 # Player actions
│   └── commonTest/kotlin/           # Cross-platform tests
│       ├── core/                    # Core engine tests
│       ├── js/                      # JavaScript library tests
│       ├── model/                   # Model tests
│       └── parser/                  # Parser tests
├── examples/
│   ├── jvm-cli-demo/               # JVM command-line demo
│   ├── js-browser-demo/            # JavaScript browser demo
│   ├── js-node-demo/               # Kotlin/JS Node.js demo
│   └── typescript-server-demo/     # TypeScript server demo
├── game-samples/                   # YAML game definitions
└── npm-package.json                # NPM package configuration
```

### Technology Stack

**Monorepo Architecture:**
- **Build Systems**: Gradle (Kotlin/Java services), npm (JS services), Maven (optional)
- **Service Independence**: Each service uses appropriate technology stack
- **Coordination**: Shared version catalog, unified documentation

**Catenin Service:**
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

```javascript
// Browser/ES6 module import
import { createGameEngineFromYaml, GameDefinitionParser, CardFactory } from './junction-catenin.mjs'

// NPM package import (TypeScript/Node.js)
import { createGameEngineFromYaml, GameDefinitionParser, CardFactory } from '@junction/catenin'

// Create game from YAML
const engine = createGameEngineFromYaml(yamlContent, ['Alice', 'Bob'])

// Parse game definition
const parser = new GameDefinitionParser()
const definition = parser.parseFromString(yamlContent)

// Generate cards
const cardFactory = new CardFactory(definition)
const cards = cardFactory.generateCards()  // Returns JavaScript Array

// Get players
const players = engine.getPlayers()  // Returns JavaScript Array

// All methods return JavaScript-friendly Arrays by default
players.map(p => p.name)  // Works perfectly!
cards.filter(c => c.type === 'spell')  // Standard Array methods!
```

### JavaScript API Design Principles

1. **Arrays Instead of Lists**: All exported methods that return collections return JavaScript Arrays by default, not Kotlin Lists. This ensures JavaScript developers can use standard Array methods (map, filter, slice) without issues.

2. **@JsExport Usage**: All classes and functions that need to be available in JavaScript must use the `@JsExport` annotation with proper import:
   ```kotlin
   import kotlin.js.JsExport
   
   @JsExport
   class GameEngine { ... }
   ```

3. **Factory Functions**: Since companion object methods aren't directly accessible in JavaScript, we provide top-level factory functions like `createGameEngineFromYaml()`.

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
- **JavaScript Library Tests**: Validate browser/HTML demo compatibility

### JavaScript Library Testing

The `JavaScriptLibraryTest` ensures that the JavaScript exports work correctly:

```bash
# Run JavaScript tests specifically
./gradlew :catenin:jsNodeTest    # Node.js environment
./gradlew :catenin:jsBrowserTest # Browser environment (headless)
```

**Test Coverage:**
- `createGameEngineFromYaml()` factory function
- JavaScript Array compatibility for `getPlayers()` and `generateCards()`
- Complete HTML demo workflow simulation
- Error handling scenarios
- YAML parsing and game definition validation

**Key Benefits:**
- Validates that JavaScript exports work in real browser/Node.js environments
- Tests the exact API used by the HTML demo
- Ensures Array methods (map, filter, slice) work correctly
- Catches JavaScript-specific issues early in development

## Development Status

**Current Phase**: ✅ PHASE 1 COMPLETE → Ready for PHASE 2!
Following the TDD Implementation Plan from `/docs/catenin/tdd-implementation-plan.md`

### Phase 1: Data Models (Week 1) - ✅ COMPLETE
- **Day 1**: ✅ Property Values and Basic Objects
  - PropertyValue sealed class (INT, STRING, BOOL, OBJECT_REF)
  - GameObject with immutable properties and states
  - Type-safe property access
- **Day 2**: ✅ Game Definition and Object Types  
  - Universal object system (not limited to cards/players)
  - ObjectTypeDefinition with properties and states
  - UniversalGameSchema with YAML parsing
  - Schema validation
- **Day 3**: ✅ Object Factory and World State
  - ObjectFactory for creating objects from schemas
  - GameWorld with immutable state management
  - Configuration-driven initialization (no hardcoded "participant")
  - Trigger and Effect definitions

### Phase 2: Trigger System (Week 2) - 🚀 NEXT
- **Day 4**: ⏳ Trigger Conditions - NEXT
- **Day 5**: ⏳ Effect System Foundation
- **Day 6**: ⏳ Complete Trigger-Effect Pipeline
- **Day 7**: ⏳ Game Actions and Turn Management

### Phase 3: High-Level Schemas (Week 3) - 📅 PLANNED
- **Day 8**: ⏳ Schema Compiler Foundation
- **Day 9**: ⏳ BoardGameSchema Compiler
- **Day 10**: ⏳ AdventureGameSchema Compiler

### Phase 4: Integration and Polish (Week 4) - 📅 PLANNED
- **Day 11**: ⏳ Expression Engine Integration
- **Day 12**: ⏳ Schema Validation and Error Handling
- **Day 13**: ⏳ Performance and Optimization
- **Day 14**: ⏳ Cross-Platform and Examples

**🎉 PHASE 1 ACHIEVEMENTS:**
- Universal object system (beyond original card/player design)
- Immutable GameWorld architecture with functional updates
- Configuration-driven initialization (removed hardcoded assumptions)
- Trigger system foundation (TriggerDefinition, EffectDefinition, TargetResolver)
- Turn-based CLI demo proving system flexibility
- 4 working demos (JVM CLI, Browser, Node.js, TypeScript)
- NPM package distribution maintained
- **Monorepo Config**: ✅ Multi-service ready (Kotlin, Java, JS, TypeScript)

## Key Files

### Planning Documents
- `/docs/catenin/tdd-implementation-plan.md` - Current development roadmap
- `/docs/catenin/architectural-boundaries.md` - Core design principles
- `/docs/catenin/turn-based-first-architecture.md` - Turn-based focus rationale
- `/docs/catenin/universal-yaml-schema.md` - YAML schema specification

### Implementation Guides
- `/docs/catenin/high-level-game-schemas.md` - High-level schema designs
- `/docs/catenin/board-game-schema.md` - BoardGameSchema specification
- `/docs/catenin/adventure-game-schema.md` - AdventureGameSchema specification
- `/docs/catenin/js-usage-example.md` - JavaScript integration guide

### Archive (Historical - DO NOT USE)
- `/docs/catenin/archive/` - Old day-by-day plans (deprecated)

The project prioritizes practical functionality over architectural complexity, with a focus on creating a working cross-platform game engine that AI agents can easily understand and modify.