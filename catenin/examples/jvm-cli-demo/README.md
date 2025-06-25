# JVM CLI Demo

This example demonstrates how to use the Catenin game engine in a JVM command-line application.

## Features

- Command-line game execution
- YAML parsing and validation
- Complete game flow demonstration
- File system I/O operations

## Running the Demo

```bash
# From the root project directory
./gradlew :catenin:examples:jvm-cli-demo:run

# Or from this directory
../../../gradlew run
```

## How it Works

The demo loads a YAML game definition, creates a game engine, and runs a complete game session in the terminal with interactive player input.

## Dependencies

This example uses:
- `implementation(project(":catenin"))` for development
- For external projects: `implementation("org.junction.catenin:catenin:1.0.0")`