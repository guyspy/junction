# Catenin Examples

This directory contains examples demonstrating how to use the Catenin game engine across different platforms.

## Structure

- **`jvm-cli-demo/`** - JVM command-line demo showing basic library usage
- **`js-browser-demo/`** - Web browser demo with DOM manipulation
- **`js-node-demo/`** - Node.js server demo for multiplayer scenarios

## Running Examples

These examples are **part of the monorepo** and use the local Catenin library directly via project dependencies.

### Prerequisites
- JDK 21+ for JVM demos
- Node.js 18+ for JavaScript demos

### Commands

Run from the **monorepo root** (`/junction/`):

```bash
# JVM command-line demo
./gradlew :catenin:examples:jvm-cli-demo:run

# JavaScript browser demo (opens dev server)
./gradlew :catenin:examples:js-browser-demo:jsBrowserDevelopmentRun

# JavaScript Node.js demo
./gradlew :catenin:examples:js-node-demo:jsNodeDevelopmentRun
```

**Note**: This project uses npm for JavaScript dependencies. If you encounter package lock issues:
```bash
./gradlew :catenin:examples:js-browser-demo:jsBrowserDevelopmentRun -x kotlinStorePackageLock
./gradlew :catenin:examples:js-node-demo:jsNodeDevelopmentRun -x kotlinStorePackageLock
```

## Example Usage

### JVM (Server/CLI)
```kotlin
import org.junction.catenin.core.GameEngine

// Create game from YAML
val engine = GameEngine.fromYaml(yamlContent, listOf("Alice", "Bob"))

// Process player actions
val result = engine.processAction(playerAction)

// Get game state
val gameState = engine.getGameState()
```

### JavaScript (Browser/Node.js)
```javascript
import { GameEngine } from './catenin-core.js'

// Create game from YAML
const engine = GameEngine.fromYaml(yamlContent, ['Alice', 'Bob'])

// Process player actions  
const result = engine.processAction(playerAction)

// Get UI state for rendering
const uiState = engine.getUIState()
```

## External Usage

To use Catenin in your own projects:

1. **Build and publish locally**:
   ```bash
   ./gradlew publishToMavenLocal
   ```

2. **Add dependency** to your `build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation("org.junction.catenin:catenin:1.0.0")
   }
   ```

3. **Use the same API** as shown in the examples above