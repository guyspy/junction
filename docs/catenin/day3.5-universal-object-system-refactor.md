# Day 3.5: Universal Object System Refactor

## Context
During Day 3 development, we realized the need for a fundamental architectural shift from game-specific models to a universal object system inspired by Roblox's architecture. This document outlines the complete refactor plan.

## Why This Refactor?
- Current system is too game-specific (hardcoded concepts like Player, Card)
- Future requirements include zones, card flipping, complex turn phases, custom objects
- Need infinite extensibility without engine changes
- Roblox-style universal object/property/trigger system provides this flexibility

## Core Paradigm Shift

### From:
- Game-specific models (Player, Card, GameState)
- Hardcoded actions (deal_damage, heal, add_score)
- Limited event system (only modify_attribute)

### To:
- Universal objects with properties
- Everything is property changes
- Universal trigger system for ANY property change
- No hardcoded game concepts

## Refactor Plan

### What to Keep:
- ✅ **YAML Parser** (the parsing library itself)
- ✅ **Monorepo Infrastructure** (build.gradle, settings, etc.)
- ✅ **Multiplatform setup** (commonMain/commonTest structure)
- ✅ **JavaScript exports setup** (for future)

### What to Remove:
- ❌ **GameDefinition** classes (too game-specific)
- ❌ **Player/Card** models (replaced by universal Object)
- ❌ **GameState** (replaced by ObjectGraph)
- ❌ **GameEngine** (complete rewrite)
- ❌ **EventProcessor** (new trigger system)
- ❌ **PlayerAction/GameEffect** (new event model)
- ❌ **All current game logic**
- ❌ **All current test cases** (new paradigm = new tests)

### Step 1: Data Models Only
1. Define new YAML structure for universal objects
2. Create data classes:
   - ObjectDefinition (schema for object types)
   - PropertyDefinition (property schemas)  
   - TriggerDefinition (when/then rules)
   - GameObject (runtime object instances)
   - ObjectGraph (the world state)
3. NO methods except data class basics
4. **STOP for review**

### Step 2: Test Cases
1. Test object creation from definitions
2. Test property changes
3. Test trigger matching
4. Test effect execution
5. **STOP for review**

### Step 3: Implementation
1. Object creation/instantiation
2. Property change system
3. Trigger evaluation engine
4. Effect execution engine

## Player vs Participant Separation

Following Roblox's brilliant design, we separate:
- **Participants**: Abstract users/seats (participant 0, participant 1, etc.) - defined in `meta.participant_count`
- **Player Objects**: Optional in-game representations linked to participants via `participant_id` property

This allows:
- One participant controlling multiple objects (character + inventory + pets)
- Pure universal engine with no special cases
- Flexible game designs (some games might not need player objects at all)
- Clean session management vs game world separation

**Key Changes:**
- `meta.player_count` → `meta.participant_count` 
- Player objects get `participant_id: {type: INT}` property to link to abstract participants
- ObjectFactory creates player objects with `participant_id` instead of `player_index`

## New YAML Design

```yaml
meta:
  name: "Universal Card Game"
  target_age: [8, 12]
  participant_count: [2, 4]  # Abstract participants/seats

# Object types define the schema - everything is just objects
object_types:
  # Player object type (optional - represents in-game player character/avatar)
  player:
    properties:
      health: {type: INT, initial: 20, min: 0}
      mana: {type: INT, initial: 0, min: 0, max: 10}
      name: {type: STRING}
      participant_id: {type: INT}  # Links to abstract participant/seat
    states:
      active: {type: BOOL, initial: false}
  
  container:
    properties:
      name: {type: STRING}
      max_size: {type: INT, initial: -1}  # -1 = unlimited
      visibility: {type: STRING, initial: "public"}

  card:
    properties:
      name: {type: STRING}
      cost: {type: INT, min: 0}
      attack: {type: INT, min: 0}
      defense: {type: INT, min: 0}
      text: {type: STRING}
    states:
      tapped: {type: BOOL, initial: false}
      face_up: {type: BOOL, initial: true}

# Predefined object instances
instances:
  lightning_bolt:
    template: card
    properties:
      name: "Lightning Bolt"
      cost: 1
      attack: 3
      defense: 0
      text: "Deal 3 damage to target"

# Universal trigger system
triggers:
  # When a card is played (moved to battlefield container)
  - name: "card_enters_battlefield"
    when:
      object_type: card
      property_changed: parent
      new_value_matches: {name: "battlefield"}
    effects:
      - log: "{this.name} enters the battlefield"
      
  # Lightning Bolt effect
  - name: "lightning_bolt_effect"
    when:
      object_type: card
      property_changed: parent
      new_value_matches: {name: "battlefield"}
      condition: "this.properties.name == 'Lightning Bolt'"
    effects:
      - modify_property:
          target: {type: player, relation: opponent}
          property: "health"
          delta: "-3"
      - change_parent:
          target: {id: "this"}
          new_parent: {type: container, property_match: {name: "graveyard"}}

```

## Key Design Principles

### 1. Everything is an Object
- Players, cards, zones - all just objects
- Objects have properties and states
- Objects can contain other objects (parent-child)

### 2. Everything is a Property Change
- Playing a card = changing its parent property
- Dealing damage = changing health property  
- Tapping = changing tapped state
- Drawing = moving object between zones

### 3. Universal Trigger System
- Listen to ANY property change on ANY object
- Rich matching conditions
- Effects are just more property changes

### 4. No Special Cases
- No "hardcoded" concepts like draw, play, damage
- Everything expressed as property modifications
- Maximum flexibility and extensibility

## Expected Benefits

This design supports all future features:
- **Zones**: Just containers with properties
- **Card flipping**: Just changing face_up state
- **Turn phases**: Just time-based trigger contexts
- **Complex abilities**: Just sophisticated trigger conditions
- **Custom objects**: Any object type with any properties
- **Infinite extensibility**: No engine changes needed for new mechanics

## Files to Remove

### Core Game Logic:
- `/core/GameEngine.kt`
- `/events/EventProcessor.kt`
- `/actions/PlayerAction.kt`
- `/actions/GameError.kt`

### Game-Specific Models:
- `/model/Player.kt`
- `/model/Card.kt`
- `/model/GameState.kt`
- `/model/GameDefinition.kt`

### Test Files:
- All files in `/commonTest/` except infrastructure tests

### Keep:
- `/parser/YamlParser.kt` (YAML parsing utility)
- `/utils/Random.kt` (utility)
- `/events/GameEventHandler.kt` (might be useful for new system)

## Next Steps

1. Execute Step 1: Remove old code and create new data models
2. Review and validate the new object schema
3. Create comprehensive test cases for the new system
4. Implement the universal trigger/effect engine
5. Validate with complex game scenarios