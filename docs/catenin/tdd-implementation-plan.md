# TDD Implementation Plan - Universal Game Schema

## Overview

This document outlines a Test-Driven Development (TDD) approach to implementing the universal game schema from the ground up. We start with a clean slate: only Kotlin Multiplatform infrastructure and YAML parsing.

**IMPORTANT: Turn-Based Games First** - This implementation focuses exclusively on turn-based games. Real-time games with tick systems are explicitly out of scope for Phases 1-4. See `turn-based-first-architecture.md` for the complete rationale.

## Implementation Philosophy

### TDD Approach
1. **Red**: Write failing tests that define the behavior we want
2. **Green**: Write minimal code to make tests pass
3. **Refactor**: Clean up code while keeping tests passing
4. **Repeat**: Add next feature incrementally

### Incremental Strategy
- Start with core primitives (objects, properties, values)
- Add trigger system layer by layer (action-driven, not time-based)
- Build effect system incrementally (discrete state changes)
- Add high-level schema compilers last
- **No real-time features**: No tick systems, timers, or continuous updates

## Phase 1: Data Models (Week 1)

### Day 1: Property Values and Basic Objects
**Goal**: Define the fundamental data types for the universal system

#### TDD Tasks:
1. **PropertyValue Tests** - Test all value types (INT, STRING, BOOL, OBJECT_REF)
2. **PropertyDefinition Tests** - Test property schemas with types, ranges, defaults
3. **GameObject Tests** - Test basic object creation, property access, immutability

#### Implementation Order:
```kotlin
// 1. PropertyValue sealed class
sealed class PropertyValue
data class IntValue(val value: Int) : PropertyValue()
data class StringValue(val value: String) : PropertyValue()
// ... etc

// 2. PropertyDefinition data class
data class PropertyDefinition(
    val type: PropertyType,
    val initial: PropertyValue? = null,
    val min: PropertyValue? = null,
    val max: PropertyValue? = null
)

// 3. GameObject data class
data class GameObject(
    val id: String,
    val type: String,
    val properties: Map<String, PropertyValue>,
    val states: Map<String, PropertyValue>
)
```

**Success Criteria**: All property value operations work, objects are immutable, property access is type-safe.

### Day 2: Game Definition and Object Types
**Goal**: YAML-defined object schemas

#### TDD Tasks:
1. **ObjectTypeDefinition Tests** - Test object type definitions from YAML
2. **UniversalGameDefinition Tests** - Test complete game definition parsing
3. **Validation Tests** - Test schema validation rules

#### Implementation Order:
```kotlin
// 1. ObjectTypeDefinition
data class ObjectTypeDefinition(
    val properties: Map<String, PropertyDefinition>,
    val states: Map<String, PropertyDefinition>
)

// 2. UniversalGameDefinition
data class UniversalGameDefinition(
    val meta: GameMeta,
    val objectTypes: Map<String, ObjectTypeDefinition>,
    val instances: Map<String, ObjectInstance>,
    val triggers: List<TriggerDefinition>
)

// 3. Validation logic
class SchemaValidator {
    fun validate(definition: UniversalGameDefinition): List<ValidationError>
}
```

**Success Criteria**: Can parse complete game definitions from YAML, validation catches schema errors.

### Day 3: Object Factory and World State
**Goal**: Create objects from definitions and manage world state

#### TDD Tasks:
1. **ObjectFactory Tests** - Test object creation from schemas
2. **GameWorld Tests** - Test world state management
3. **Initial Setup Tests** - Test participant and instance creation

#### Implementation Order:
```kotlin
// 1. ObjectFactory
class ObjectFactory(private val definition: UniversalGameDefinition) {
    fun createObject(type: String, overrides: Map<String, PropertyValue>): GameObject
    fun createFromInstance(instanceId: String): GameObject
    fun createInitialSetup(participantNames: List<String>): List<GameObject>
}

// 2. GameWorld
data class GameWorld(
    val objects: Map<String, GameObject>,
    val nextObjectId: Int
) {
    fun withObject(obj: GameObject): GameWorld
    fun withoutObject(objectId: String): GameWorld
    fun updateObject(objectId: String, updates: GameObject): GameWorld
}
```

**Success Criteria**: Can create objects from schemas, world state is immutable, initial setup works.

## Phase 2: Trigger System (Week 2)

### Day 4: Trigger Conditions
**Goal**: Implement trigger condition matching

#### TDD Tasks:
1. **TriggerCondition Tests** - Test all condition types (object_type, property_changed, etc.)
2. **ConditionEvaluator Tests** - Test complex condition expressions
3. **TriggerMatcher Tests** - Test trigger firing conditions

#### Implementation Order:
```kotlin
// 1. TriggerCondition
data class TriggerCondition(
    val objectType: String? = null,
    val propertyChanged: String? = null,
    val newValue: PropertyValue? = null,
    val condition: String? = null
)

// 2. ConditionEvaluator using JEXL
class ConditionEvaluator {
    fun evaluate(expression: String, context: Map<String, Any>): Boolean
}

// 3. TriggerMatcher
class TriggerMatcher {
    fun findMatchingTriggers(
        triggers: List<TriggerDefinition>,
        event: PropertyChangeEvent
    ): List<TriggerDefinition>
}
```

**Success Criteria**: All trigger conditions work, complex expressions evaluate correctly.

### Day 5: Effect System Foundation
**Goal**: Basic effect execution

#### TDD Tasks:
1. **EffectDefinition Tests** - Test all effect types
2. **TargetResolver Tests** - Test target resolution (self, opponent, by ID, by property)
3. **EffectExecutor Tests** - Test basic effect execution

#### Implementation Order:
```kotlin
// 1. Effect types
sealed class EffectDefinition
data class ModifyPropertyEffect(...) : EffectDefinition()
data class CreateObjectEffect(...) : EffectDefinition()
data class DestroyObjectEffect(...) : EffectDefinition()

// 2. TargetResolver
class TargetResolver {
    fun resolveTargets(
        target: TargetDefinition,
        world: GameWorld,
        context: EffectContext
    ): List<GameObject>
}

// 3. EffectExecutor
class EffectExecutor {
    fun executeEffect(
        effect: EffectDefinition,
        world: GameWorld,
        context: EffectContext
    ): GameWorld
}
```

**Success Criteria**: Basic effects work (modify properties, create/destroy objects).

### Day 6: Complete Trigger-Effect Pipeline
**Goal**: Full reactive system

#### TDD Tasks:
1. **TriggerEngine Tests** - Test complete trigger firing pipeline
2. **PropertyChange Tests** - Test property change detection and propagation
3. **TriggerChain Tests** - Test triggers firing other triggers

#### Implementation Order:
```kotlin
// 1. PropertyChangeEvent
data class PropertyChangeEvent(
    val objectId: String,
    val propertyPath: String,
    val oldValue: PropertyValue?,
    val newValue: PropertyValue
)

// 2. TriggerEngine
class TriggerEngine {
    fun processPropertyChange(
        event: PropertyChangeEvent,
        world: GameWorld,
        triggers: List<TriggerDefinition>
    ): GameWorld
}

// 3. GameEngine coordinator
class GameEngine {
    fun updateProperty(objectId: String, property: String, value: PropertyValue): GameWorld
}
```

**Success Criteria**: Property changes fire triggers, effects modify world, cascade effects work.

### Day 7: Game Actions and Turn Management
**Goal**: Player actions and game flow

#### TDD Tasks:
1. **PlayerAction Tests** - Test action validation and execution
2. **GameController Tests** - Test turn/phase management
3. **ActionValidator Tests** - Test action legality checking
4. **ThreadSafeGameEngine Tests** - Test coordinated state updates with immutable world

#### Implementation Order:
```kotlin
// 1. PlayerAction system
sealed class PlayerAction
data class ModifyObjectAction(...) : PlayerAction()
data class MoveObjectAction(...) : PlayerAction()

// 2. ActionValidator
class ActionValidator {
    fun validateAction(action: PlayerAction, world: GameWorld): ValidationResult
}

// 3. GameController
class GameController {
    fun processAction(participantId: String, action: PlayerAction): ActionResult
    fun advanceTurn(): GameWorld
}

// 4. ThreadSafeGameEngine (coordination layer)
class ThreadSafeGameEngine {
    @Volatile private var currentWorld: GameWorld
    private val coordinator = GameCoordinator()
    
    fun processAction(action: PlayerAction): ActionResult
    fun getCurrentWorld(): GameWorld  // Thread-safe read
}
```

**Success Criteria**: Players can perform actions, turns advance correctly, invalid actions are rejected.

**🔒 Threading & Coordination Note for Day 7:**
While GameWorld immutability provides memory safety and eliminates data corruption, we still need coordination logic for:
- **Business logic consistency**: Ensuring actions don't violate game rules
- **Resource conflicts**: Managing shared resources (deck, shared objects)
- **Turn order enforcement**: Preventing out-of-turn actions
- **Atomic operations**: Multi-step game actions that must complete together

Implementation approach:
1. **Immutable GameWorld**: Handles memory safety (already implemented ✅)
2. **Coordination Layer**: Handles logical consistency and sequencing
3. **Action Validation**: Ensures actions are legal before applying
4. **Atomic Updates**: Use synchronized blocks for multi-step operations

Example coordination patterns:
```kotlin
// Turn-based coordination
synchronized(turnManager) {
    if (isPlayerTurn(playerId)) {
        currentWorld = currentWorld.applyAction(action)
        advanceTurn()
    }
}

// Resource conflict resolution
synchronized(resourceLock) {
    val available = world.getSharedResource("deck").quantity
    if (available >= action.requestedAmount) {
        world = world.consumeResource("deck", action.requestedAmount)
    }
}
```

## Phase 3: High-Level Schemas (Week 3)

### Day 8: Schema Compiler Foundation
**Goal**: Transpilation infrastructure

#### TDD Tasks:
1. **SchemaCompiler Tests** - Test basic transpilation framework
2. **CompilerContext Tests** - Test compilation context management
3. **GeneratedCode Tests** - Test generated universal schema validation

#### Implementation Order:
```kotlin
// 1. SchemaCompiler interface
interface SchemaCompiler<T> {
    fun compile(highLevelSchema: T): UniversalGameDefinition
}

// 2. CompilerContext
class CompilerContext {
    fun addObjectType(name: String, definition: ObjectTypeDefinition)
    fun addTrigger(trigger: TriggerDefinition)
    fun addInstance(id: String, instance: ObjectInstance)
}
```

**Success Criteria**: Compilation framework works, can generate valid universal schemas.

### Day 9: BoardGameSchema Compiler
**Goal**: First high-level schema implementation

#### TDD Tasks:
1. **BoardGameDefinition Tests** - Test board game YAML parsing
2. **BoardCompiler Tests** - Test board → universal transpilation
3. **ChessExample Tests** - Test complete Chess game compilation

#### Implementation Order:
```kotlin
// 1. BoardGameDefinition
data class BoardGameDefinition(
    val meta: GameMeta,
    val board: BoardDefinition,
    val pieces: Map<String, PieceDefinition>,
    val setup: SetupDefinition
)

// 2. BoardGameCompiler
class BoardGameCompiler : SchemaCompiler<BoardGameDefinition> {
    override fun compile(schema: BoardGameDefinition): UniversalGameDefinition
}
```

**Success Criteria**: Can compile Chess from BoardGameSchema to universal, all pieces/movement work.

### Day 10: AdventureGameSchema Compiler
**Goal**: Second high-level schema

#### TDD Tasks:
1. **AdventureGameDefinition Tests** - Test adventure game YAML parsing
2. **AdventureCompiler Tests** - Test adventure → universal transpilation  
3. **MonkeyIslandExample Tests** - Test complete adventure game compilation

**Success Criteria**: Can compile point-and-click adventure from AdventureGameSchema to universal.

## Phase 4: Integration and Polish (Week 4)

### Day 11: Expression Engine Integration
**Goal**: Advanced condition expressions

#### TDD Tasks:
1. **JEXL Integration Tests** - Test Apache Commons JEXL integration
2. **ExpressionEngine Tests** - Test game-specific expression context
3. **ComplexCondition Tests** - Test complex trigger conditions

**Success Criteria**: Rich expressions work in triggers (arithmetic, string operations, object queries).

### Day 12: Schema Validation and Error Handling
**Goal**: Production-ready validation

#### TDD Tasks:
1. **JSON Schema Tests** - Test schema validation using JSON Schema libs
2. **ValidationEngine Tests** - Test comprehensive game definition validation
3. **ErrorReporting Tests** - Test clear error messages with line numbers
4. **Annotation-Based Documentation** - Add Swagger/OpenAPI annotations to model classes for comprehensive field documentation and automatic JSON Schema generation

**Success Criteria**: Clear validation errors, schema violations caught early.

**📝 Note for Day 12 Implementation:**
Consider adding Swagger/OpenAPI annotations (`@Schema`) to all model classes for:
- Self-documenting code with rich field descriptions
- Automatic JSON Schema generation from annotations  
- Integration with validation pipeline
- API documentation generation

Example approach:
```kotlin
@Schema(description = "Game metadata and configuration")
data class GameMeta(
    @Schema(description = "Display name shown to players", example = "Wizard's Quest", minLength = 1, maxLength = 100)
    val name: String,
    @Schema(description = "Target age range [min, max]", example = "[8, 12]")
    val targetAge: IntArray
)
```

Dependency: `io.swagger.core.v3:swagger-annotations:2.2.15`

### Day 13: Performance and Optimization
**Goal**: Production performance

#### TDD Tasks:
1. **Performance Tests** - Test large game performance (1000+ objects)
2. **Memory Tests** - Test memory usage for complex games
3. **Trigger Performance Tests** - Test trigger evaluation speed
4. **Concurrent Performance Tests** - Test thread safety and coordination overhead

**Success Criteria**: Can handle complex games with good performance.

**🏁 Threading Performance Note for Day 13:**
Test the performance characteristics of our immutable + coordination approach:
- **Immutable copy overhead**: Measure cost of creating new GameWorld instances
- **Lock contention**: Test coordination bottlenecks under concurrent load
- **Memory efficiency**: Verify structural sharing reduces memory usage
- **Throughput**: Compare single-threaded vs multi-threaded action processing

Benchmark scenarios:
```kotlin
// High-frequency updates (many small actions)
@Test fun testHighFrequencyActions() { ... }

// Concurrent players (multiplayer simulation)
@Test fun testConcurrentPlayers() { ... }

// Memory growth (long-running games)
@Test fun testMemoryGrowth() { ... }
```

### Day 14: Cross-Platform and Examples
**Goal**: Complete delivery

#### TDD Tasks:
1. **JavaScript Export Tests** - Test JS compilation and API
2. **Example Games Tests** - Test complete example implementations
3. **Demo Integration Tests** - Test examples in demo applications

**Success Criteria**: Examples work on all platforms, demos are functional.

## Testing Strategy

### Test Categories
1. **Unit Tests**: Individual class behavior
2. **Integration Tests**: Component interaction
3. **Schema Tests**: High-level → universal compilation
4. **Game Tests**: Complete game scenarios
5. **Performance Tests**: Speed and memory usage

### Test Data
- **Chess**: Complex board game with intricate rules
- **Magic: The Gathering**: Card game with zones and effects
- **Monkey Island**: Adventure game with inventory and dialogue
- **Tower Defense**: Real-time elements
- **Tic-Tac-Toe**: Simple game for basic validation

### Success Metrics
- 100% test coverage on core classes
- All example games compile and run
- Performance benchmarks met
- Cross-platform compatibility verified

## Risk Mitigation

### Technical Risks
1. **YAML Complexity**: Mitigated by incremental complexity
2. **Performance**: Mitigated by performance tests from Day 1
3. **Cross-Platform**: Mitigated by continuous JS compilation testing

### Scope Risks
1. **Feature Creep**: Mitigated by strict TDD discipline
2. **Over-Engineering**: Mitigated by minimal implementation approach
3. **Timeline**: Mitigated by daily deliverables and clear success criteria

This plan ensures we build a robust, well-tested universal game schema system that can handle any game type while maintaining code quality through TDD practices.