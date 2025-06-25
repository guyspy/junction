# Catenin Examples

This directory contains examples demonstrating how to use the Catenin game engine library.

## Structure

- `jvm-demo/` - JVM command-line demo showing basic library usage

## Running Examples

These examples are **not part of the published library**. They are standalone demonstrations that show how to use the published Catenin library.

To run the examples:

1. First build and publish the library locally (from the main project):
   ```bash
   ./gradlew publishToMavenLocal
   ```

2. Then create standalone example projects that depend on the published library

## Example Usage

The JVM demo shows the basic pattern for using Catenin:

```kotlin
import org.junction.catenin.core.GameEngine
import org.junction.catenin.parser.GameDefinitionParser

// Parse YAML game definition
val parser = GameDefinitionParser()
val definition = parser.parseFromString(yamlContent)

// Create game engine
val engine = GameEngine.fromYaml(yamlContent, listOf("Player1", "Player2"))

// Use the game engine...
```