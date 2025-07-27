# JavaScript Usage Example

With the new expect/actual pattern implementation, here's how to use GameWorld from JavaScript:

```javascript
import { 
    createGameWorld, 
    createAddObjectUpdate,
    createRemoveObjectUpdate,
    GameObject,
    IntValue
} from '@junction/catenin'

// Create a new game world
const world = createGameWorld()

// Create a game object
const player = new GameObject("player1", "player", {
    health: new IntValue(100),
    name: new StringValue("Alice")
})

// Create an update to add the player
const addPlayerUpdate = createAddObjectUpdate(player)

// Apply the update asynchronously
async function updateGame() {
    // Returns a Promise<GameWorldJs>
    const newWorld = await world.applyUpdate(addPlayerUpdate)
    
    // Chain multiple updates
    const removeUpdate = createRemoveObjectUpdate("player1")
    const finalWorld = await newWorld.applyUpdate(removeUpdate)
    
    return finalWorld
}

// Use it
updateGame().then(finalWorld => {
    console.log("Game updated!")
})
```

## Why This Design?

1. **No Code Duplication**: All game logic is in `GameWorldImpl` in common code
2. **Clean Exports**: `GameWorldJs` wrapper provides JavaScript-friendly API
3. **Promise-based**: All updates return Promises for async/await compatibility
4. **Type-safe**: TypeScript definitions are generated automatically

## Architecture

```
commonMain/
  - GameWorld.kt (expect class)
  - GameWorldImpl.kt (shared implementation)
  - WorldUpdate.kt (update types)

jsMain/
  - GameWorld.kt (actual class with Promise method)
  - GameWorldJs.kt (exported wrapper for JavaScript)
  - WorldUpdateJs.kt (factory functions for updates)

jvmMain/
  - GameWorld.kt (actual class, minimal implementation)
```

This follows Kotlin Multiplatform best practices where platform-specific code is minimal and shared logic stays in common code.