# Catenin Library Structure

## What Gets Published

The Catenin library contains **only** the core game engine functionality:

### Core Library (commonMain)
```
src/commonMain/kotlin/org/junction/catenin/
├── core/
│   └── GameEngine.kt          # Main game engine API
├── model/
│   ├── Card.kt               # Card data models
│   ├── GameDefinition.kt     # YAML structure definitions  
│   └── Player.kt             # Player data models
└── parser/
    ├── GameDefinitionParser.kt # YAML parsing logic
    └── YamlParser.kt          # Low-level YAML parsing
```

### What's NOT Included
- ❌ File I/O operations (application layer responsibility)
- ❌ Console/UI output (application layer responsibility)  
- ❌ Demo applications (separate examples)
- ❌ Platform-specific utilities (not needed for core)

## Usage

```kotlin
// Add dependency to your project
implementation("org.junction:catenin:1.0.0-SNAPSHOT")

// Use the library
import org.junction.catenin.core.GameEngine

val engine = GameEngine.fromYaml(yamlContent, playerNames)
```

## Design Principles

1. **Pure business logic** - No I/O, no UI, no platform dependencies
2. **String-based API** - Accepts YAML content as strings
3. **Cross-platform compatibility** - Compiles to JVM and JavaScript
4. **Zero application concerns** - Library consumers handle file loading, display, etc.

This makes the library:
- ✅ Easy to test (no file mocking needed)
- ✅ Platform agnostic (works in any environment)
- ✅ Focused responsibility (just game logic)
- ✅ Clean API surface (minimal dependencies)