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
cd catenin
./gradlew build
```

### Commands

```bash
# Build all platforms (JVM + JS)
./gradlew build

# Run tests
./gradlew test                    # All tests
./gradlew jvmTest                # JVM tests only
./gradlew jsNodeTest             # JavaScript tests (Node.js)

# Run JVM command-line demo
./gradlew jvmRun

# Generate JavaScript library
./gradlew jsBrowserProductionLibraryDistribution

# Clean build
./gradlew clean
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
│   ├── jvmMain/kotlin/             # JVM-specific (server)
│   │   ├── cli/                    # Command-line interface
│   │   └── platform/               # File I/O
│   └── jsMain/kotlin/              # JS-specific (frontend)
│       └── platform/               # Browser APIs
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
# All tests
./gradlew test

# Platform-specific tests
./gradlew jvmTest      # JVM tests (fast)
./gradlew jsNodeTest   # JavaScript tests (Node.js)
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