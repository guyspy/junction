# Universal YAML Schema Documentation

This document defines the complete YAML schema for the Universal Game Definition system. This is the **single source of truth** for all game definitions and must be followed by all implementations.

## Core Philosophy

The schema is designed to be **truly universal** - it makes no assumptions about game type, mechanics, or concepts. All game-specific logic is expressed through generic object/property/trigger patterns.

## Schema Design Principles

**Map-Based Dynamic Keys**: Many fields use `Map[String, ...]` to allow arbitrary user-defined names while keeping the schema compact and flexible.

**Field Type Legend**:
- 🔧 **SYSTEM**: Required schema structure fields
- 🎮 **USER**: Arbitrary names defined by game designers
- 📝 **MIXED**: System structure containing user-defined content

This design prioritizes flexibility and universality over rigid type safety.

## Root Structure

```yaml
meta:                    # Required - Game metadata
object_types:           # Required - Define object templates  
instances:              # Optional - Predefined object instances
triggers:               # Optional - Global trigger definitions
setup:                  # Optional - Game initialization rules
runtime_spawning:       # Optional - Dynamic object creation rules
```

## Schema Reference

### `meta` (Required)

Game metadata and configuration.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Display name of the game |
| `target_age` | Array[Int] | Yes | Age range `[min, max]` |
| `participant_count` | Array[Int] | | Participant range `[min, max]` (abstract seats/players) |

**Example:**
```yaml
meta:
  name: "My Game"
  target_age: [8, 12]
  participant_count: [2, 4]
```

### `object_types` (Required)

Defines templates for object types using Map[String, ObjectTypeDefinition].

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `<object_type_name>` | ObjectTypeDefinition | Yes | User-defined object type name (arbitrary) |
| `properties` | Map[String, PropertyDefinition] | | Static properties with constraints |
| `states` | Map[String, StateDefinition] | | Dynamic state properties |
| `triggers` | Array[TriggerDefinition] | | Type-specific triggers |

**Example:**
```yaml
object_types:
  wizard:                    # 🎮 USER: Arbitrary object type name
    properties:              # 🔧 SYSTEM: Properties container
      mana:                  # 🎮 USER: Arbitrary property name
        type: INT            # 🔧 SYSTEM: Required type field
        initial: 100         # 🔧 SYSTEM: Optional initial value
        min: 0               # 🔧 SYSTEM: Optional constraint
    states:                  # 🔧 SYSTEM: States container
      casting:               # 🎮 USER: Arbitrary state name
        type: BOOL           # 🔧 SYSTEM: Required type field
        initial: false       # 🔧 SYSTEM: Required initial value
```

### `ObjectTypeDefinition`

Defines the schema for an object type, including its properties and states.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `properties` | Map[String, PropertyDefinition] | | Static properties with constraints |
| `states` | Map[String, PropertyDefinition] | | Dynamic state properties |

### `PropertyDefinition`

Defines a property with type constraints and defaults (all fields are system-defined).

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | PropertyType | Yes | Data type: `INT`, `STRING`, `BOOL`, `OBJECT_REF` |
| `initial` | String | | Default value (stored as string) |
| `min` | String | | Minimum value (stored as string) |
| `max` | String | | Maximum value (stored as string) |
| `values` | Array[String] | | Enum values (for future use) |

**Example:**
```yaml
properties:
  health:                    # 🎮 USER: Arbitrary property name
    type: INT                # 🔧 SYSTEM: Required type field
    initial: 100             # 🔧 SYSTEM: Optional default value
    min: 0                   # 🔧 SYSTEM: Optional constraint
    max: 200                 # 🔧 SYSTEM: Optional constraint
  name:                      # 🎮 USER: Another arbitrary property name
    type: STRING             # 🔧 SYSTEM: Required type field
    initial: "Default"       # 🔧 SYSTEM: Optional default value
```

### `StateDefinition`

Defines a dynamic state property.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | PropertyType | Yes | Data type: `INT`, `STRING`, `BOOL`, `OBJECT_REF` |
| `initial` | String | Yes | Initial value (stored as string) |

**Example:**
```yaml
states:
  current_health: {type: INT, initial: 100}
  status: {type: STRING, initial: "normal"}
  activated: {type: BOOL, initial: false}
```

### `instances` (Optional)

Predefined object instances with specific property values using Map[String, ObjectInstance].

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `<instance_name>` | ObjectInstance | Yes | User-defined instance name (arbitrary) |
| `object_type` | String | Yes | Object type to instantiate |
| `properties` | Map[String, String] | | Property overrides (as strings) |
| `states` | Map[String, String] | | State overrides (as strings) |
| `triggers` | Array[TriggerDefinition] | | Instance-specific triggers |

**Example:**
```yaml
instances:
  lightning_bolt:               # 🎮 USER: Arbitrary instance name
    object_type: spell          # 🔧 SYSTEM: Required object type reference
    properties:                 # 🔧 SYSTEM: Properties container
      name: "Lightning Bolt"    # 🎮 USER: Property name (from object type)
      damage: "3"               # 🎮 USER: Property value (as string)
      cost: "1"                 # 🎮 USER: Property value (as string)
```

### `setup` (Optional)

Game initialization configuration.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `world_initialization` | Array[CreateObjectsInstruction] | | Objects created at game start |
| `participant_initialization` | Array[CreateObjectsInstruction] | | Objects created per participant |

**Example:**
```yaml
setup:
  world_initialization:
    - create_objects:
        count: 1
        object_type: "game_board"
        properties:
          width: "10"
          height: "10"
  
  participant_initialization:
    - create_objects:
        count: 1
        object_type: "player_state"
        properties:
          participant_id: "{participant_id}"
          name: "{participant_name}"
```

### `CreateObjectsInstruction`

Instruction for creating objects during initialization.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `create_objects` | CreateObjectsRule | Yes | The creation rule |

### `CreateObjectsRule`

Rule for creating objects.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `count` | Int | Yes | Number of objects to create |
| `object_type` | String | Yes | Object type to instantiate |
| `properties` | Map[String, String] | | Property overrides (as strings) |
| `instance_source` | String | | Use predefined instance instead of template |
| `parent` | String | | Parent object ID pattern |

**Participant Substitution Patterns:**
- `{participant_id}` - Replaced with participant index (0, 1, 2, ...)
- `{participant_name}` - Replaced with participant name

**Example:**
```yaml
- create_objects:
    count: 3
    object_type: "card"
    instance_source: "lightning_bolt"
    parent: "deck_{participant_id}"
```

### `runtime_spawning` (Optional)

Dynamic object creation rules triggered by property changes.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Rule name |
| `when` | TriggerCondition | Yes | When to trigger |
| `effects` | Array[EffectDefinition] | Yes | What to do |

**Example:**
```yaml
runtime_spawning:
  - name: "spawn_enemy"
    when:
      property_changed: "spawn_requested"
      new_value: "true"
    effects:
      - create_object:
          object_type: "enemy"
          properties:
            x: "0"
            y: "5"
```

### `triggers` (Optional)

Global trigger definitions.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | | Trigger name |
| `when` | TriggerCondition | Yes | When to trigger |
| `effects` | Array[EffectDefinition] | Yes | What to do |

### `TriggerCondition`

Condition for when a trigger should fire.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `object_type` | String | | Filter by object type |
| `property_changed` | String | | Property name that changed |
| `new_value` | String | | Exact value match (as string) |
| `new_value_matches` | ObjectMatcher | | Complex value matching |
| `condition` | String | | Expression condition |

**Example:**
```yaml
when:
  object_type: enemy
  property_changed: "health"
  new_value: "0"
```

### `ObjectMatcher`

Matches objects based on criteria.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | String | | Object type to match |
| `name` | String | | Object name to match |
| `owner` | String | | Owner to match |

**Example:**
```yaml
new_value_matches:
  type: "container"
  name: "battlefield"
```

### `EffectDefinition`

Effect to execute when trigger fires.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `log` | String | | Log message |
| `modify_property` | ModifyPropertyEffect | | Change property value |
| `change_parent` | ChangeParentEffect | | Change object relationships |
| `create_object` | CreateObjectEffect | | Create new object |
| `destroy_object` | DestroyObjectEffect | | Remove object |

**Example:**
```yaml
effects:
  - log: "Enemy defeated!"
  - modify_property:
      target: {type: player, relation: self}
      property: "score"
      delta: "+10"
```

### `ModifyPropertyEffect`

Modify a property of an object.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `target` | TargetDefinition | Yes | Which object to modify |
| `property` | String | Yes | Property name to modify |
| `delta` | String | | Change by amount (e.g., "+5", "-3") |
| `value` | String | | Set to exact value |

### `ChangeParentEffect`

Change object relationships.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `target` | TargetDefinition | Yes | Object to move |
| `new_parent` | TargetDefinition | | New parent (null = remove) |

### `CreateObjectEffect`

Create a new object.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `template` | String | Yes | Object type to create |
| `id` | String | | Specific ID (null = auto-generate) |
| `properties` | Map[String, String] | | Property values (as strings) |
| `parent` | TargetDefinition | | Parent object |

### `DestroyObjectEffect`

Remove an object.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `target` | TargetDefinition | Yes | Object to destroy |

### `TargetDefinition`

References to objects for effects.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | String | | Object type filter |
| `relation` | String | | Relationship: "self", "opponent", etc. |
| `id` | String | | Specific object ID |
| `property_match` | Map[String, String] | | Match by property values |

**Special Values:**
- `{id: "this"}` - The triggering object
- `{relation: "self"}` - Related to current participant
- `{relation: "opponent"}` - Related to other participants

**Examples:**
```yaml
# Target by type and relation
target: {type: player, relation: opponent}

# Target the triggering object
target: {id: "this"}

# Target by property match
target: {property_match: {name: "battlefield"}}
```

## Data Types

### `PropertyType`

Supported property types:
- `INT` - Integer numbers
- `STRING` - Text values
- `BOOL` - Boolean true/false
- `OBJECT_REF` - Reference to another object

## Key Design Principles

1. **No Hardcoded Concepts**: Object types, properties, and states are completely arbitrary
2. **String Storage**: All values stored as strings in YAML, rehydrated to typed values at runtime
3. **Pattern-Based**: Support for `{participant_id}` and `{participant_name}` substitution
4. **Property-Driven**: Everything is a property change - no special game mechanics
5. **Universal Effects**: Only basic operations - modify, create, destroy, move

## Validation Rules

1. `meta` section is required
2. `object_types` section is required
3. All property/state types must be valid `PropertyType` values
4. All object type references must exist in `object_types`
5. All instance template references must exist in `object_types`
6. Property values must be parseable as their declared types
7. Trigger conditions must reference valid properties

## Example Games

See complete examples in:
- `/game-samples/universal-card-game.yaml` - Card game mechanics
- `/game-samples/tower-defense-game.yaml` - Real-time strategy mechanics

These examples demonstrate how the same universal schema can express completely different game types using only generic object/property/trigger patterns.