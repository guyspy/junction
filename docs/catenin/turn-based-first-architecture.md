# Turn-Based First Architecture Decision

## Executive Summary

Junction/Catenin is designed as a **turn-based game engine first**, with real-time games as a future consideration. This architectural decision shapes our entire implementation approach and creates a cleaner, more focused foundation.

## Core Decision

**Catenin prioritizes turn-based games as first-class citizens.**

Real-time games will be addressed in future iterations, potentially as a separate engine (`CateninRT`) or as an optional mode with tick system extensions.

## Rationale

### 1. Educational Game Focus
Most educational games are naturally turn-based:
- **Card games**: Magic: The Gathering, educational math cards
- **Board games**: Chess, educational geography games  
- **Adventure games**: Text adventures, point-and-click learning
- **Strategy games**: Turn-based puzzles, logic games
- **Quiz games**: Question-answer cycles

### 2. AI-Friendly Design
Turn-based games are ideal for AI agent interaction:
- **Discrete states**: Clear before/after snapshots for AI analysis
- **Predictable flow**: AI can reason about action consequences
- **YAML simplicity**: No timing complexities in game definitions
- **Testing**: Deterministic behavior, easier to validate

### 3. Architectural Cleanliness
Our current immutable design is perfect for turn-based:
```kotlin
// Clean action-based flow
val newWorld = gameEngine.processAction(playerId, DrawCardAction())

// vs complex real-time requirements
val newWorld = gameEngine.tick(deltaTime = 16.67f)
gameEngine.scheduleTimer(5000f) { spawnEnemy() }
```

### 4. Implementation Simplicity
Turn-based games avoid complex timing issues:
- **No game loops**: Event-driven architecture
- **No interpolation**: Discrete state changes
- **No lag compensation**: Action-based networking
- **Deterministic testing**: No timing-dependent behavior

### 5. Network-Friendly
Turn-based networking is much simpler:
```yaml
# Simple action messages
action: "play_card"
player: "alice"
card_id: "fire_spell_1"

# vs complex real-time sync
position: [120.5, 45.2]
velocity: [0.5, -2.1] 
timestamp: 1641234567890
sequence: 12847
```

## What This Means for Implementation

### Phase 1-4 Scope (Current)
**Include:**
- ✅ Action-based gameplay (DrawCard, PlayCard, EndTurn)
- ✅ Event-driven triggers (on_play, on_destroy)
- ✅ Turn management and phases
- ✅ Immutable world state
- ✅ Discrete win/lose conditions

**Exclude:**
- ❌ Tick systems and game loops
- ❌ Time-based triggers (after: 5000ms)
- ❌ Continuous animations
- ❌ Real-time networking concerns
- ❌ Frame rate considerations

### YAML Schema Impact
Our game definitions stay clean:
```yaml
# Turn-based trigger
triggers:
  - when: {event: "card_played", card_type: "spell"}
    effects:
      - action: "deal_damage" 
        target: "opponent"
        amount: 3

# NO real-time complexity:
# - when: {timer: "5000ms"}
# - when: {fps_tick: true}
```

### Cadherin Integration Impact
UI layer can focus on clean state transitions:
```typescript
// Simple state-based rendering
cadherin.onGameStateChange((newState) => {
  renderer.update(newState)
  animator.transitionTo(newState) // Optional smooth transitions
})

// NO real-time complexity:
// cadherin.startGameLoop(60, renderFrame)
// cadherin.handleContinuousInput()
```

## Future Real-Time Strategy

When we eventually need real-time games, we have clean options:

### Option 1: Separate Engine
```kotlin
// CateninRT - Real-time focused engine
class CateninRTEngine {
  fun tick(deltaTime: Float): GameWorld
  fun scheduleTimer(delay: Float, callback: () -> Unit)
}
```

### Option 2: Mode Extension
```kotlin
// Extended Catenin with optional real-time mode
class CateninEngine(val mode: GameMode) {
  fun processAction(action: Action): GameWorld  // Turn-based
  fun tick(deltaTime: Float): GameWorld?        // Real-time only
}
```

### Option 3: Unified Cadherin
```typescript
// Cadherin handles both modes transparently
const cadherin = new Cadherin(gameDefinition)

if (gameDefinition.meta.realTime) {
  cadherin.startGameLoop(60)  // Real-time mode
} else {
  cadherin.listenForActions() // Turn-based mode
}
```

## Benefits of This Decision

### For Developers
- **Cleaner codebase**: No timing complexity
- **Easier testing**: Deterministic behavior
- **Simpler debugging**: Clear action → state flow
- **Better documentation**: Focused examples

### For AI Agents
- **Predictable modification**: Clear cause-and-effect in YAML
- **Easier validation**: Can test complete game scenarios
- **Semantic clarity**: Actions have obvious meanings
- **Incremental learning**: Start simple, add complexity later

### For Educational Use
- **Age-appropriate**: Turn-based games suit classroom environments
- **Pedagogical value**: Students can think through moves
- **Collaborative**: Easy to discuss strategy
- **Assessment-friendly**: Clear win/lose criteria

## Implementation Timeline

**Phase 1-4 (Current)**: Perfect turn-based foundation
**Phase 5+**: Evaluate real-time needs based on:
- User feedback and demand
- Specific real-time game requirements
- Technical complexity vs benefit analysis
- Educational value assessment

## Validation Games

Our turn-based focus is validated by these target games:
- ✅ **Chess**: Perfect turn-based strategy
- ✅ **Magic: The Gathering**: Complex card interactions
- ✅ **Monopoly**: Board game with property management
- ✅ **Monkey Island**: Adventure game with inventory/dialogue
- ✅ **Educational card games**: Math, vocabulary, science

**Future real-time games** (Phase 5+):
- 🔮 Tower Defense: Enemies spawn over time
- 🔮 Racing games: Continuous movement
- 🔮 Action games: Real-time combat

## Conclusion

By focusing on turn-based games first, we create a solid, clean foundation that serves educational gaming perfectly. Real-time capabilities can be added later without compromising the core architecture.

This decision makes Phases 1-4 implementation cleaner, testing easier, and the final product more suitable for its primary educational gaming mission.