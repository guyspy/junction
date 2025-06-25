# JS Node.js Demo

This example demonstrates how to use the Catenin game engine in a Node.js server-side application.

## Features

- Server-side game logic
- File system operations
- HTTP API implementation (simulated)
- Multi-player game room management

## Running the Demo

```bash
# From the root project directory
./gradlew :catenin:examples:js-node-demo:jsNodeRun

# Or from this directory
../../../gradlew jsNodeRun
```

## How it Works

The demo creates a simple game server that manages game rooms, processes player actions, and maintains game state. This demonstrates how the Catenin engine can be used for server-side multiplayer game logic.

## Dependencies

This example uses:
- `implementation(project(":catenin"))` for development
- For external projects: `implementation("org.junction.catenin:catenin:1.0.0")`

## Use Cases

This server-side setup is ideal for:
- Multiplayer game backends
- Game state validation
- Anti-cheat mechanisms
- Game session management
- Real-time game APIs