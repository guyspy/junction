# JVM CLI Demo

This example demonstrates how to use the Catenin game engine in a JVM command-line application.

## Features

- **Command-line Interface**: Terminal-based demo with console output
- **YAML Parsing**: Demonstrates parsing of YAML game definitions
- **Card Generation**: Shows how to generate cards from game definitions
- **Game Engine Creation**: Creates a functional game engine instance
- **Cross-platform Logic**: Uses the same game engine that runs in browsers

## Running the Demo

### Recommended: Use the Run Scripts (Clean CLI Experience)

For the best interactive experience without Gradle's progress bar:

```bash
# From this directory
./run.sh          # Unix/Mac/Linux
run.bat           # Windows
```

These scripts build the demo and run it using the standalone executable, providing a clean CLI interface without Gradle's build output interfering with the game.

### Alternative: Direct Gradle Execution

If you prefer using Gradle directly, use the `--console=plain` flag for a cleaner experience:

```bash
# From the monorepo root (/junction/)
./gradlew :catenin:examples:jvm-cli-demo:run --console=plain

# Or from this directory
../../../gradlew run --console=plain
```

**Note**: Without `--console=plain`, Gradle's progress indicators may interfere with the interactive CLI experience.

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

## Requirements

- **Java 21 or higher** (the project uses JVM toolchain 21)
- If using the run scripts, ensure `JAVA_HOME` points to Java 21+

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