# GameWorld API Guide

GameWorld uses the expect/actual pattern for cross-platform compatibility with coroutines.

## Core Design

The `GameWorld` is defined as an expect class with a suspend function:

```kotlin
// commonMain
expect class GameWorld {
    suspend fun applyUpdate(update: WorldUpdate): GameWorld
}
```

Platform-specific actual implementations handle the details.

## JavaScript/TypeScript Usage

The JavaScript implementation provides a Promise-based wrapper:

```javascript
import { createGameWorld, applyUpdateAsPromise, AddObjectUpdate, GameObject } from '@junction/catenin'

// Create a game world
const world = createGameWorld()

// Apply updates asynchronously
async function updateGame() {
    const obj = new GameObject("test1", "testType")
    const update = new AddObjectUpdate(obj)
    
    // Returns a Promise
    const newWorld = await applyUpdateAsPromise(world, update)
}
```

The actual JavaScript implementation includes:

```kotlin
// jsMain/GameWorld.kt
actual class GameWorld {
    actual suspend fun applyUpdate(update: WorldUpdate): GameWorld { /* ... */ }
    
    // Promise wrapper for JavaScript
    fun applyUpdateAsPromise(update: WorldUpdate): Promise<GameWorld> = 
        GlobalScope.promise { applyUpdate(update) }
}
```

## JVM/Kotlin Usage

The JVM implementation works directly with coroutines:

```kotlin
// In a coroutine context
suspend fun updateGame(world: GameWorld) {
    val obj = GameObject("test1", "testType")
    val update = AddObjectUpdate(obj)
    val newWorld = world.applyUpdate(update)
}

// For tests or main functions
fun main() = runBlocking {
    val world = GameWorld.empty()
    val newWorld = world.applyUpdate(update)
}
```

## Platform Details

### Common (expect)
- Defines the interface contract
- Contains WorldUpdate sealed class hierarchy
- Platform-agnostic

### JVM (actual)
- Direct coroutine implementation
- No blocking code or Java compatibility layers
- Clean, minimal implementation

### JS (actual)
- Same implementation as JVM
- Additional `applyUpdateAsPromise` method for JavaScript
- `@JsExport` on factory functions

## Design Benefits

1. **Clean separation**: Platform-specific code isolated in actual implementations
2. **Coroutine-first**: Native async support without blocking
3. **JavaScript-friendly**: Promise API available where needed
4. **Minimal surface**: Single method, single responsibility
5. **Type-safe**: Full type safety across platforms

This design follows Kotlin Multiplatform best practices for async operations.