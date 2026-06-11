# Architectural Boundaries - Catenin Implementation Guide

## Overview

This document defines **strict implementation boundaries** to ensure code follows the documented architecture. It serves as a quality gate to prevent architectural drift and magic string violations.

## Core Principle

> **Universal components MUST remain truly universal and domain-agnostic. Game-specific logic belongs in Schema Compilers only.**

## Layer Definitions

### 🟢 Universal Layer (Domain-Agnostic)
**Components that should know NOTHING about specific games**

```
core/
├── GameWorld          ✅ Generic object container
├── GameObject         ✅ Generic property/state holder  
├── PropertyValue      ✅ Generic value types
factory/
├── ObjectFactory      ✅ Generic object creation
initialization/
├── GameInitializer    ⚠️  Should be generic setup utility
validation/
└── SchemaValidator    ✅ Generic schema validation
```

**✅ ALLOWED in Universal Layer:**
- Generic object operations (`getObject`, `updateObject`)
- Type-agnostic property handling 
- Configuration-driven behavior
- Pure data structures
- Abstract interfaces

**❌ FORBIDDEN in Universal Layer:**
- Magic strings for game types (`"participant"`, `"hand"`, `"deck"`)
- Magic strings for properties (`"participant_id"`, `"name"`, `"health"`)
- Game-specific setup assumptions
- Hardcoded object relationships
- Domain knowledge of any kind
- Turn management logic (turns are schema-driven, not engine features)
- Player order enforcement (handled by triggers, not engine code)

### 🟡 Schema Compiler Layer (Domain-Specific)
**Components that transpile high-level schemas to universal**

```
compilers/
├── TabletopGameCompiler    ✅ Knows boards, pieces, hands
├── AdventureGameCompiler   ✅ Knows rooms, items, doors
├── CardGameCompiler        ✅ Knows decks, zones, turns
└── RPGGameCompiler         ✅ Knows stats, levels, combat
```

**✅ ALLOWED in Schema Compiler Layer:**
- Game-specific type definitions
- Domain-specific setup logic  
- Magic strings (contained within compiler)
- Complex game rule transpilation
- Schema-specific validation

### 🔴 Application Layer (Integration)
**Components that use Catenin for specific games**

```
applications/
├── ChessGame           ✅ Uses TabletopGameCompiler
├── MonkeyIslandGame    ✅ Uses AdventureGameCompiler  
└── HearthstoneGame     ✅ Uses CardGameCompiler
```

## Implementation Rules

### Rule 1: No Magic Strings in Universal Layer

**❌ VIOLATION:**
```kotlin
// In ObjectFactory (Universal Layer)
fun createInitialSetup(names: List<String>) {
    if (!definition.hasObjectType("participant")) {  // MAGIC STRING!
        return emptyList()
    }
    // Create participants...
}
```

**✅ CORRECT:**
```kotlin
// In TabletopGameCompiler (Schema Layer)
class TabletopGameCompiler {
    companion object {
        const val PARTICIPANT_TYPE = "participant"
        const val PARTICIPANT_ID_PROPERTY = "participant_id"
    }
    
    fun compileSetup(schema: TabletopGameSchema): SetupConfiguration {
        return SetupConfiguration(
            participantType = PARTICIPANT_TYPE,
            participantIdProperty = PARTICIPANT_ID_PROPERTY
        )
    }
}

// In ObjectFactory (Universal Layer) 
fun createInitialSetup(names: List<String>, config: SetupConfiguration) {
    if (!definition.hasObjectType(config.participantType)) {  // CONFIGURATION-DRIVEN!
        return emptyList()
    }
}
```

### Rule 2: Universal Components Must Be Configuration-Driven

**❌ VIOLATION:**
```kotlin
// Hardcoded game logic in universal component
fun createWorldObjects(): List<GameObject> {
    val objects = mutableListOf<GameObject>()
    
    if (definition.hasObjectType("game_controller")) {    // HARDCODED!
        objects.add(createObject("game_controller"))
    }
    if (definition.hasObjectType("main_board")) {         // HARDCODED!
        objects.add(createObject("main_board"))
    }
    
    return objects
}
```

**✅ CORRECT:**
```kotlin
// Generic, configuration-driven
fun createWorldObjects(config: WorldSetupConfiguration): List<GameObject> {
    val objects = mutableListOf<GameObject>()
    
    config.singletonObjects.forEach { objectConfig ->
        if (definition.hasObjectType(objectConfig.type)) {
            objects.add(createObject(objectConfig.type, customId = objectConfig.id))
        }
    }
    
    return objects
}

// Configuration comes from Schema Compiler
data class WorldSetupConfiguration(
    val singletonObjects: List<SingletonObjectConfig>
)

data class SingletonObjectConfig(
    val type: String,
    val id: String
)
```

### Rule 3: Schema Compilers Handle Domain Logic

**✅ CORRECT - Domain logic in compiler:**
```kotlin
class TabletopGameCompiler {
    fun compileWorldSetup(schema: TabletopGameSchema): WorldSetupConfiguration {
        val singletons = mutableListOf<SingletonObjectConfig>()
        
        // Schema compiler makes domain-specific decisions
        if (schema.requiresGameController) {
            singletons.add(SingletonObjectConfig("game_controller", "game_controller"))
        }
        
        if (schema.board != null) {
            singletons.add(SingletonObjectConfig("board", "main_board"))
        }
        
        return WorldSetupConfiguration(singletonObjects = singletons)
    }
}
```

### Rule 4: Turn Management Must Be Schema-Driven

**CRITICAL**: Turn management is NOT a universal engine feature. Different game schemas require different action ordering:
- **TurnBasedSchema**: Rigid turn order, phases, action points
- **NarrativeSchema**: Flexible action order, story-driven progression
- **Real-time schemas** (future): No turns at all

**❌ VIOLATION - Hardcoded turn logic in engine:**
```kotlin
// In GameEngine (Universal Layer) - WRONG!
class GameEngine {
    private var currentPlayerIndex = 0
    private val players = mutableListOf<String>()
    
    fun nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }
    
    fun canAct(playerId: String): Boolean {
        return playerId == players[currentPlayerIndex]  // HARDCODED TURN LOGIC!
    }
}
```

**✅ CORRECT - Turn management via triggers:**
```kotlin
// TurnBasedSchema generates these triggers
triggers:
  - name: "enforce_turn_order"
    when:
      any_action_attempted: true
    condition: "actor.id == game_state.current_player"
    reject_with: "Not your turn!"
    
  - name: "advance_turn"
    when:
      action_type: "end_turn"
    effects:
      - modify_property:
          target: "game_state"
          property: "current_player_index"
          value: "(current_player_index + 1) % player_count"
```

```kotlin
// NarrativeSchema uses different triggers
triggers:
  - name: "story_progression_lock"
    when:
      action_type: "enter_room"
    condition: "world.hasStoryFlag('key_found')"
    reject_with: "The door is locked. You need a key."
```

**Key Principle**: The engine just processes triggers. Turn order, story locks, and action validation are ALL schema-defined trigger rules.

### Rule 5: Tests Must Follow Same Boundaries

**❌ VIOLATION:**
```kotlin
// Test using magic strings
@Test
fun testGameSetup() {
    val world = initializer.createInitialWorld(listOf("Alice", "Bob"))
    
    val participants = world.getObjectsByType("participant")  // MAGIC STRING!
    assertEquals(2, participants.size)
}
```

**✅ CORRECT:**
```kotlin
// Test with explicit configuration
@Test
fun testGameSetup() {
    val config = TestSetupConfiguration(participantType = "participant")
    val world = initializer.createInitialWorld(listOf("Alice", "Bob"), config)
    
    val participants = world.getObjectsByType(config.participantType)
    assertEquals(2, participants.size)
}
```

## Violation Detection

### Automated Checks (Code Review Checklist)

**🚨 Red Flags - Immediate Rejection:**

1. **Magic String Check:**
   ```kotlin
   // Search for these patterns in Universal Layer:
   ❌ "participant"
   ❌ "game_controller" 
   ❌ "player_state"
   ❌ "hand", "deck", "board"
   ❌ "participant_id", "name"
   ```

2. **Hardcoded Logic Check:**
   ```kotlin
   // These patterns indicate hardcoded domain logic:
   ❌ if (definition.hasObjectType("specific_type"))
   ❌ obj.getProperty("specific_property")
   ❌ createObject("specific_type")
   ❌ currentPlayer, nextTurn(), canAct()
   ❌ turnOrder, playerIndex, activePlayer
   ```

3. **Import Check:**
   ```kotlin
   // Universal components should not import schema-specific types
   ❌ import com.example.TabletopGameSchema
   ❌ import com.example.ParticipantSetup
   ```

### Manual Review Questions

**For Universal Layer Components:**

1. Could this component work with a completely different game type (chess vs. adventure game)?
2. Does it make any assumptions about what types of objects exist?
3. Could the logic be driven by configuration instead of hardcoded?
4. Does it contain any domain-specific terminology?

**For Schema Compiler Components:**

1. Is all domain logic contained within this layer?
2. Does it properly transpile to generic universal objects?
3. Are magic strings properly encapsulated as constants?

## Migration Strategy

### Phase 1: Extract Magic Strings (Quick Win)
```kotlin
// Create constants files for each domain
object TabletopGameTypes {
    const val PARTICIPANT = "participant"
    const val GAME_CONTROLLER = "game_controller" 
    const val BOARD = "board"
}

object TabletopGameProperties {
    const val PARTICIPANT_ID = "participant_id"
    const val NAME = "name"
}
```

### Phase 2: Configuration-Driven Refactoring
```kotlin
// Replace hardcoded logic with configuration
data class SetupConfiguration(
    val participantType: String?,
    val singletonTypes: List<String>,
    val participantSpecificTypes: List<ParticipantObjectConfig>
)
```

### Phase 3: Schema Compiler Implementation
```kotlin
// Move domain logic to proper layer
class TabletopGameCompiler {
    fun compileSetup(schema: TabletopGameSchema): SetupConfiguration {
        // All game-specific decisions happen here
    }
}
```

## Success Criteria

### ✅ **Architectural Compliance Achieved When:**

1. **Universal Layer:**
   - Zero magic strings
   - Zero hardcoded game assumptions
   - All behavior driven by configuration
   - Could support any game type

2. **Schema Compiler Layer:**
   - Contains all domain-specific logic
   - Generates proper configuration for universal layer
   - Magic strings properly encapsulated

3. **Tests:**
   - Follow same boundaries as production code
   - Use explicit configuration
   - No magic strings in universal layer tests

4. **Documentation:**
   - Each component clearly states its layer
   - Examples show proper boundary respect
   - Architecture diagrams match implementation

## Enforcement

### Pre-commit Hooks
```bash
# Add to .git/hooks/pre-commit
echo "Checking for magic strings in Universal Layer..."
git diff --cached --name-only | grep "src/commonMain/kotlin/org/junction/catenin/core" | xargs grep -l "participant\|game_controller\|player_state" && echo "❌ Magic strings found in Universal Layer!" && exit 1
```

### CI/CD Checks
```yaml
# Add to GitHub Actions
- name: Architectural Boundary Check
  run: |
    # Check for violations in Universal Layer
    find src/commonMain/kotlin/org/junction/catenin/core -name "*.kt" -exec grep -l "participant\|game_controller" {} \; && exit 1 || echo "✅ Boundaries respected"
```

This document should prevent the architectural drift we experienced and ensure all future code respects the universal/domain boundaries properly.