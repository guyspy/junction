# JVM CLI Demo

This example demonstrates how to use the Catenin game engine in a JVM command-line application.

## Features

- **Command-line Interface**: Terminal-based demo with console output
- **YAML Parsing**: Demonstrates parsing of YAML game definitions
- **Card Generation**: Shows how to generate cards from game definitions
- **Game Engine Creation**: Creates a functional game engine instance
- **Cross-platform Logic**: Uses the same game engine that runs in browsers

## Running the Demo

```bash
# From the monorepo root (/junction/)
./gradlew :catenin:examples:jvm-cli-demo:run

# Or from this directory
../../../gradlew run
```

## How it Works

The demo:
1. **Defines** a hardcoded YAML game definition for testing
2. **Parses** the YAML using the GameDefinitionParser
3. **Creates** a game engine with multiple players (Alice, Bob)
4. **Generates** cards using the CardFactory
5. **Displays** card information and game state in the terminal

## Game Definition

The demo uses a simple hardcoded YAML definition:
```yaml
meta:
  name: "Day 1 測試遊戲"
  target_age: [8, 12]

cards:
  number_card:
    count: 8
    properties:
      value: {type: int, min: 1, max: 5}
      color: {type: enum, values: [red, blue, green]}
```

## Dependencies

This example uses:
- `implementation(project(":catenin"))` for monorepo development
- For external projects: `implementation("org.junction.catenin:catenin:1.0.0")`

## Use Cases

This JVM setup is ideal for:
- **Server-side Game Logic**: Authoritative game state management
- **Game Testing**: Automated testing of game rules and mechanics
- **CLI Tools**: Command-line game creation and validation tools
- **Backend Services**: Game logic services for web/mobile frontends