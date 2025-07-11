# Catenin Game Engine

A Kotlin Multiplatform game engine for creating 2D card-based educational games, optimized for AI Agent driven game creation.

## 🎯 Overview

Catenin is part of the Junction educational gaming platform. It provides a unified API that compiles to both JVM (server) and JavaScript (frontend), allowing shared game logic across platforms.

### Key Features

- **🔄 Kotlin Multiplatform**: Same game logic runs on JVM and JavaScript
- **📝 YAML DSL**: AI-friendly game definition format
- **🎮 Card-Based Games**: Optimized for educational card games
- **🤖 AI-First Design**: Built for AI agent modification and creation
- **⚡ Fast Builds**: Optimized build configuration

## 🚀 Quick Start

### Prerequisites

- Java 21+ (JDK)
- Gradle 8.5+

### Installation

```bash
git clone <repository-url>
cd junction  # Root of the monorepo
./gradlew :catenin:build
```

### Commands

**Note**: Catenin is part of the Junction monorepo. All commands should be run from the monorepo root using the `:catenin` prefix.

```bash
# Build all platforms (JVM + JS)
./gradlew :catenin:build

# Run tests
./gradlew :catenin:allTests        # All tests
./gradlew :catenin:jvmTest         # JVM tests only
./gradlew :catenin:jsTest          # All JavaScript tests
./gradlew :catenin:jsNodeTest      # JavaScript tests (Node.js environment)
./gradlew :catenin:jsBrowserTest   # JavaScript tests (Browser environment)

# Generate JavaScript library
./gradlew :catenin:jsBrowserProductionLibraryDistribution

# Clean build
./gradlew :catenin:clean

# Running Examples
./gradlew :catenin:examples:jvm-cli-demo:run                            # JVM CLI demo
./gradlew :catenin:examples:js-browser-demo:serve                       # JS browser demo (copies files)
./gradlew :catenin:examples:js-node-demo:jsNodeDevelopmentRun           # Kotlin/JS Node.js demo
cd catenin/examples/typescript-server-demo && npm start                 # TypeScript server demo

# Note: This project uses npm (not yarn) for JavaScript dependencies.
# If you encounter package lock issues, use:
# ./gradlew :catenin:examples:js-node-demo:jsNodeDevelopmentRun -x kotlinStorePackageLock
```

## 📁 Project Structure

```
catenin/
├── src/
│   ├── commonMain/kotlin/           # Shared game logic
│   │   ├── model/                   # Game data models
│   │   ├── parser/                  # YAML parsing
│   │   ├── core/                    # Game engine
│   │   └── actions/                 # Player actions
│   └── commonTest/kotlin/           # Tests (run on both JVM & JS)
│       ├── core/                    # Core engine tests
│       ├── js/                      # JavaScript library tests
│       ├── model/                   # Model tests
│       └── parser/                  # Parser tests
├── examples/                       # Usage examples
│   ├── jvm-cli-demo/              # JVM command-line demo
│   ├── js-browser-demo/           # JavaScript browser demo
│   ├── js-node-demo/              # Kotlin/JS Node.js demo
│   └── typescript-server-demo/    # TypeScript server demo
├── game-samples/                   # Example YAML games
├── build.gradle.kts               # Build configuration
└── README.md                      # This file
```

## 🎮 Game Definition Format

Games are defined using YAML with a structured format:

```yaml
meta:
  name: "Simple Combat"
  target_age: [8, 12]
  player_count: [2, 4]

cards:
  attack_card:
    count: 10
    properties:
      damage:
        type: int
        min: 2
        max: 5
      element:
        type: enum
        values: [fire, water, earth]
    events:
      on_play:
        action: "deal_damage"
        target: "opponent"
        amount: "{damage}"

mechanics:
  setup:
    players:
      health: 20
      hand_size: 5
  win_conditions:
    - type: "health_depleted"
      message: "{winner} wins by defeating all opponents!"

ai_hints:
  difficulty_factors: 
    - "cards.attack_card.properties.damage.max"
    - "mechanics.setup.players.health"
  common_modifications:
    easier: {damage_max: 3, health: 25}
    harder: {damage_max: 7, health: 15}
```

## 💻 Usage Examples

Catenin is a library. To use it in your project, add it as a dependency. For local testing, see the `examples/` directory which contains runnable demo applications.

### NPM Package Distribution

You can also use Catenin as an npm package for JavaScript/TypeScript projects:

```bash
# Create the npm package
./gradlew :catenin:createNpmPackage

# Install in your project
npm install /path/to/catenin/build/npm-package
```

### JVM (Server)

```kotlin
import org.junction.catenin.core.GameEngine

// Load game from YAML file
val yamlContent = File("game-samples/simple-combat.yaml").readText()
val engine = GameEngine.fromYaml(yamlContent, listOf("Alice", "Bob"))

// Get game state
val players = engine.getPlayers()
val definition = engine.getGameDefinition()

println("Game: ${definition.meta.name}")
println("Players: ${players.map { it.name }}")
```

### JavaScript (Frontend)

```javascript
import { GameEngine } from './catenin-core.js'

// Create game from YAML
const engine = GameEngine.fromYaml(yamlContent, ['Alice', 'Bob'])

// Get UI state for rendering
const players = engine.getPlayers()
const definition = engine.getGameDefinition()

console.log(`Game: ${definition.meta.name}`)
console.log(`Players: ${players.map(p => p.name)}`)
```

### TypeScript (Server)

```typescript
import { GameEngine, CardFactory, createGameEngineFromYaml } from '@junction/catenin'

// Create game engine with full type safety
const engine: GameEngine = createGameEngineFromYaml(yamlContent, ['Alice', 'Bob'])
const players: Player[] = engine.getPlayers()
const cardFactory: CardFactory = new CardFactory(engine.getGameDefinition())

// Type-safe operations
players.forEach((player: Player) => {
  console.log(`${player.name}: ${player.health} HP`)
})
```

## 🏗️ Architecture

### Technology Stack

- **Core**: Kotlin Multiplatform 1.9.20
- **Frontend**: Kotlin/JS compiles to JavaScript
- **Backend**: Kotlin/JVM 
- **Serialization**: kotlinx.serialization + kaml (YAML)
- **Testing**: JUnit 5 (JVM), Mocha (JS)

### Design Principles

1. **AI-First**: Structured YAML format that AI can easily understand and modify
2. **Cross-Platform**: Same game logic on frontend and backend
3. **Incremental**: Each development day delivers a working game
4. **Performance**: Fast compilation and optimized builds

## 🧪 Testing

### Running Tests

```bash
# From the monorepo root:
# All tests
./gradlew :catenin:allTests

# Platform-specific tests
./gradlew :catenin:jvmTest      # JVM tests (fast)
./gradlew :catenin:jsNodeTest   # JavaScript tests (Node.js)
```

### Test Structure

- **Unit Tests**: Individual component testing
- **Integration Tests**: Complete game flow testing
- **Cross-Platform**: Same tests run on JVM and JS

## 🔧 Development

### Build Configuration

The project uses Kotlin Multiplatform with optimized build settings:

- **JVM Target**: Java 21 toolchain
- **JS Target**: Library binaries for browser compatibility
- **Dependencies**: Minimal, focused on game engine needs

### Performance Notes

- **JVM Build**: ~15-20 seconds
- **JS Build**: ~50-60 seconds (includes npm setup)
- **JS Tests**: Node.js tests may timeout due to npm dependencies

## 🤖 AI Integration

Catenin is designed for AI agent modification:

```yaml
ai_hints:
  difficulty_factors: 
    - "cards.spell.properties.damage.max"
    - "mechanics.setup.players.health"
  common_modifications:
    easier: {damage_max: 3, health: 30}
    harder: {damage_max: 8, health: 10}
```

AI agents can:
- Parse game definitions
- Modify difficulty parameters
- Create new card types
- Adjust game mechanics

## 📚 Examples

See `game-samples/` directory for example games:

- `simple-combat.yaml`: Basic combat game
- `number-war.yaml`: Number comparison game

## 🐛 Troubleshooting

### Common Issues

**JS tests hang**: This is due to npm/yarn dependency resolution, not game logic issues. JS compilation works fine.

**Build errors**: Ensure Java 21+ is installed and `JAVA_HOME` is set correctly.

**YAML parsing errors**: Check YAML syntax and required fields in game definitions.

### Getting Help

1. Check existing game samples in `game-samples/`
2. Review test files for usage examples
3. Consult the main project documentation

## 📄 License

Part of the Junction educational gaming platform.

---

**Catenin** - *Building educational games, one card at a time* 🎴