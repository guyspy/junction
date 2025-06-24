# Junction - Educational Gaming Platform

## Project Overview

Junction is an educational gaming platform monorepo, inspired by cell junction biological concepts. It enables educators to create online 2D card-based games with AI assistance and provides a complete gaming ecosystem.

**Current Status**: 🎉 **MVP Complete + Multi-Service Monorepo Ready**

## Core Features

- **Game Creation Engine**: YAML DSL with AI-assisted card game creation  
- **Multi-Platform Gaming**: Cross-platform game engine (JVM + JavaScript)
- **Community Features**: Rating systems, sharing mechanisms
- **Crowdfunding**: Platform sustainability
- **Social Good**: Charitable credit system for underprivileged children

## Technology Architecture

**Monorepo Design**: Multi-technology services with independent build systems

- **Cadherin (Game Engine)**: Kotlin Multiplatform + YAML DSL
- **Future Occludin (Server)**: Quarkus + Java + MongoDB  
- **Future Renderers**: Pure JavaScript + Phaser/Three.js
- **Future AI Services**: Python + ML models
- **Development Method**: Test-driven development

## Service Naming Convention

Based on cell junction biological concepts:
- **`cadherin`**: Game engine SDK (Kotlin Multiplatform)
- **`occludin`**: Server platform services (Future: Quarkus + Java)
- **Future services**: Will follow cell junction protein naming

## Development Status

### ✅ **Phase 1: Game Engine Core (COMPLETED)**
1. **Cadherin SDK**: YAML DSL + Kotlin Multiplatform engine
2. **Multi-Platform Support**: JVM + JavaScript targets  
3. **Example Projects**: CLI, Browser, Node.js demos
4. **Maven Publishing**: Ready for external consumption

### 🚧 **Phase 2: Platform Infrastructure (PLANNED)**
1. **Occludin Server**: Quarkus + MongoDB backend
2. **User Management System**: Authentication & authorization
3. **Game Room Management**: Real-time multiplayer
4. **Communication System**: WebSocket + event streaming

### 📋 **Phase 3: Creation Tools (PLANNED)**
1. **Game Editor**: Web-based YAML editor
2. **AI-Assisted Creation**: Game generation with LLMs
3. **Asset Management**: Card art and audio system

### 💰 **Phase 4: Community & Business (PLANNED)**
1. **Rating & Sharing System**: Community features
2. **Crowdfunding Features**: Platform sustainability  
3. **Charitable Credit System**: Social impact features

## Monorepo Structure

```
junction/
├── cadherin/                        # 🎮 Game Engine SDK (Kotlin Multiplatform)
│   ├── src/                         # Core engine code
│   │   ├── commonMain/kotlin/       # Shared game logic  
│   │   ├── jvmMain/kotlin/         # JVM platform code
│   │   └── jsMain/kotlin/          # JavaScript platform code
│   ├── examples/                    # Platform demos
│   │   ├── jvm-cli-demo/           # Command-line demo
│   │   ├── js-browser-demo/        # Web browser demo
│   │   └── js-node-demo/           # Node.js server demo
│   └── game-samples/               # YAML game templates
├── docs/                           # 📚 Documentation
│   ├── monorepo-architecture.md    # Monorepo design
│   ├── cadherin/                   # Game engine docs
│   └── overview.md                 # Project overview
├── gradle/                         # 🔧 Shared Gradle config
│   ├── libs.versions.toml         # Version catalog
│   └── wrapper/                    # Gradle wrapper
├── build.gradle.kts               # Root build configuration
├── settings.gradle.kts            # Project structure
└── CLAUDE.md                      # AI assistant guidance

# Future services will be added as siblings to cadherin/
├── occludin/                      # 🚧 Future: Quarkus server
├── phaser-renderer/               # 🚧 Future: JS game renderer  
└── ai-agent/                      # 🚧 Future: Python ML service
```

## Quick Start

### Prerequisites
- JDK 21+ for Cadherin development
- Node.js 18+ for JavaScript demos

### Build & Run
```bash
# Build entire monorepo
./gradlew build

# Run JVM command-line demo
./gradlew :cadherin:examples:jvm-cli-demo:run

# Run web browser demo  
./gradlew :cadherin:examples:js-browser-demo:jsBrowserRun

# Run Node.js server demo
./gradlew :cadherin:examples:js-node-demo:jsNodeRun
```

### Game Development
```kotlin
// Create game from YAML
val engine = GameEngine.fromYaml(yamlContent, playerNames)

// Process player actions
val result = engine.processAction(playerAction)

// Get game state
val gameState = engine.getGameState()
```

## Contributing

See individual service documentation:
- [Cadherin Game Engine](cadherin/README.md)
- [Monorepo Architecture](docs/monorepo-architecture.md)
- [Development Guidelines](CLAUDE.md)

## License

MIT License - see individual service licenses for details.