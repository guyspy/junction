# Catenin Examples

This directory contains examples demonstrating how to use the Catenin game engine across different platforms and technology stacks.

## Demo Comparison

| Demo | Language | Target | Build System | Use Case |
|------|----------|--------|--------------|----------|
| `jvm-cli-demo` | Kotlin | JVM | Gradle | Command-line tools, backend services |
| `js-browser-demo` | Kotlin/JS | Browser | Gradle | Web frontends, client-side games |
| `kotlin-js-node-demo` | Kotlin | Node.js | Gradle | Kotlin-first server development |
| `typescript-node-demo` | TypeScript | Node.js | npm/tsc | TypeScript-first server development |

## Technology Stacks

### JVM Ecosystem
- **`jvm-cli-demo/`** - Pure Kotlin targeting JVM
  - ✅ Native Kotlin development experience
  - ✅ Full JVM ecosystem access
  - ✅ Ideal for server applications and CLI tools

### Browser Ecosystem  
- **`js-browser-demo/`** - Kotlin/JS targeting browser
  - ✅ Write Kotlin, run in browser
  - ✅ DOM manipulation with Kotlin
  - ✅ Shared logic with server-side Kotlin

### Node.js Ecosystem - Two Approaches

#### Kotlin-First Approach
- **`kotlin-js-node-demo/`** - Kotlin → Kotlin/JS → Node.js
  - ✅ Stay in Kotlin ecosystem
  - ✅ Share code between JVM and Node.js
  - ✅ Kotlin type safety and tooling
  - ✅ Ideal for Kotlin teams

#### TypeScript-First Approach  
- **`typescript-node-demo/`** - TypeScript → JavaScript → Node.js
  - ✅ Standard npm package consumption
  - ✅ TypeScript type safety via .d.ts files
  - ✅ Integrate with existing TypeScript projects
  - ✅ Ideal for TypeScript/JavaScript teams

## Running Examples

### Prerequisites
- **JDK 21+** for JVM and Kotlin/JS demos
- **Node.js 18+** for JavaScript demos

### Commands

Run from the **monorepo root** (`/junction/`):

```bash
# JVM command-line demo
./gradlew :catenin:examples:jvm-cli-demo:run

# JavaScript browser demo
./gradlew :catenin:examples:js-browser-demo:serve

# Kotlin/JS Node.js demo
./gradlew :catenin:examples:kotlin-js-node-demo:jsNodeDevelopmentRun

# TypeScript Node.js demo (uses NPM package)
cd catenin/examples/typescript-node-demo && npm start
```

**Note**: For package lock issues with Kotlin/JS demos:
```bash
./gradlew :catenin:examples:js-browser-demo:serve -x kotlinStorePackageLock
./gradlew :catenin:examples:kotlin-js-node-demo:jsNodeDevelopmentRun -x kotlinStorePackageLock
```

## Architecture Patterns

### Shared Game Logic
All demos use the **same Catenin game engine**, demonstrating true cross-platform development:

```kotlin
// Same API across all platforms
val engine = GameEngine.fromYaml(yamlContent, playerNames)
val result = engine.processAction(playerAction)
val gameState = engine.getGameState()
```

### Platform-Specific APIs

**JVM (Kotlin)**:
```kotlin
import org.junction.catenin.core.GameEngine

val engine = GameEngine.fromYaml(yamlContent, listOf("Alice", "Bob"))
val players = engine.getPlayers()  // Returns Array<Player>
```

**JavaScript (Kotlin/JS)**:
```javascript
import { createGameEngineFromYaml } from './catenin-core.js'

const engine = createGameEngineFromYaml(yamlContent, ['Alice', 'Bob'])
const players = engine.getPlayers()  // Returns JavaScript Array
```

**TypeScript (NPM Package)**:
```typescript
import { createGameEngineFromYaml, Player } from '@junction/catenin'

const engine = createGameEngineFromYaml(yamlContent, ['Alice', 'Bob'])
const players: Player[] = engine.getPlayers()  // Full type safety
```

## Development Workflows

### Monorepo Development
For internal development, all examples use project dependencies:
```kotlin
dependencies {
    implementation(project(":catenin"))
}
```

### External Usage
For using Catenin in your own projects:

1. **JVM Projects**: Add Maven dependency
   ```kotlin
   dependencies {
       implementation("org.junction.catenin:catenin:1.0.0")
   }
   ```

2. **TypeScript Projects**: Install NPM package
   ```bash
   npm install @junction/catenin
   ```

## Example Features Demonstrated

### Core Game Engine
- ✅ YAML game definition parsing
- ✅ Player state management  
- ✅ Card generation and properties
- ✅ Action processing (draw, play, end turn)
- ✅ Game state tracking and validation

### Platform Integration
- ✅ **JVM**: File I/O, console interaction
- ✅ **Browser**: DOM manipulation, event handling
- ✅ **Kotlin/JS Node**: Server architecture, room management
- ✅ **TypeScript**: Type safety, npm ecosystem integration

### Development Experience
- ✅ **Hot reload** in development
- ✅ **Source maps** for debugging
- ✅ **Type safety** across platforms
- ✅ **Modern tooling** integration

## Next Steps

1. **Run the demos** to understand different approaches
2. **Choose your stack** based on team preferences and project requirements
3. **Copy patterns** from the most relevant demo for your use case
4. **Integrate Catenin** into your project using the appropriate method

---

**Choose Your Adventure**: Whether you're building with Kotlin, TypeScript, or need browser compatibility, there's a demo that matches your technology preferences! 🎮