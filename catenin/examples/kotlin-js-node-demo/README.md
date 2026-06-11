# Kotlin/JS Node.js Demo

> **Status: NOT YET IMPLEMENTED** -- This demo is planned but not yet built. There is no `src/` directory or working Kotlin/JS application. The README below describes the intended design.

**Technology Stack**: Kotlin → Kotlin/JS → Node.js

This example demonstrates how to write **Kotlin code** that compiles to Node.js using Kotlin/JS compilation.

## What This Is
- ✅ Write your server code in **Kotlin**
- ✅ Compile to Node.js using Kotlin/JS
- ✅ Deploy as standard Node.js application
- ✅ Same language for game logic and server logic

## When to Use This
- You prefer Kotlin over TypeScript/JavaScript
- You want to share code between JVM and Node.js deployments  
- You're building in a Kotlin-first environment
- You want type safety with Kotlin's type system

## Features

- **Real Game Engine Usage**: Uses actual `createGameEngineFromYaml()` and `CardFactory`
- **Server-side Architecture**: Demonstrates Node.js server structure
- **Game Room Management**: Creates and manages multiple game sessions
- **Console Logging**: Detailed logging of server operations
- **Actual Game Logic**: Real YAML parsing, card generation, and player management
- **Cross-platform Logic**: Uses the same game engine that runs in browsers

## Running the Demo

```bash
# From the monorepo root (/junction/)
./gradlew :catenin:examples:kotlin-js-node-demo:jsNodeDevelopmentRun

# Or from this directory
../../../gradlew jsNodeDevelopmentRun
```

**Note**: This project uses npm for JavaScript dependencies. If you encounter package lock issues:
```bash
./gradlew :catenin:examples:kotlin-js-node-demo:jsNodeDevelopmentRun -x kotlinStorePackageLock
```

## How it Works

The demo:
1. **Uses Real YAML**: Defines actual game definitions with attack and heal cards
2. **Creates Game Engines**: Uses `createGameEngineFromYaml()` to create real game instances
3. **Manages Game Rooms**: Creates multiple game rooms with different players
4. **Generates Cards**: Uses `CardFactory` to create and display actual game cards
5. **Tracks Player State**: Shows real player health, hand sizes, and game state
6. **Demonstrates Server Logic**: Shows how to structure a multiplayer game server

## Node.js Compatibility

The demo works with:
- Node.js 18+ (recommended)
- ES6 module support
- File system access
- Console output for game state visualization

## Dependencies

This example uses:
- `implementation(project(":catenin"))` for monorepo development
- For external projects: `implementation("org.junction.catenin:catenin:1.0.0")`

## Development

The Kotlin/JS code compiles to Node.js-compatible JavaScript:
- **Output**: CommonJS modules for Node.js
- **TypeScript**: Auto-generated .d.ts files for IDE support
- **Source Maps**: Full debugging support in Node.js
- **Hot Reload**: Development server with automatic restarts

## Use Cases

This server-side setup is ideal for:
- **Multiplayer Game Backends**: Real-time multiplayer game servers
- **Game State Validation**: Server-side rule enforcement
- **Anti-cheat Mechanisms**: Authoritative game logic
- **Game Session Management**: Player matchmaking and room management
- **Real-time APIs**: WebSocket or HTTP game APIs
- **Game Analytics**: Server-side game event tracking
- **Tournament Systems**: Competitive game management