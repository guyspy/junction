# AdventureGameSchema Design

## Overview

AdventureGameSchema is a high-level schema for creating narrative-driven adventure games featuring:
- Explorable locations (rooms, areas, scenes)
- Interactive objects and items
- Inventory management
- Puzzles and challenges
- Character dialogues and interactions
- Story progression and state tracking

This schema extends NarrativeSchema and transpiles to UniversalGameSchema.

## Key Concepts

### 1. **Locations**
Game spaces that players can explore:
- **Rooms**: Discrete locations with descriptions
- **Connections**: How locations link together
- **State Changes**: Locations can change based on actions

### 2. **Items**
Interactive objects that can be:
- **Examined**: Provide descriptions or clues
- **Collected**: Added to inventory
- **Used**: On other items or in locations
- **Combined**: Create new items

### 3. **Characters**
NPCs that can:
- **Dialogue**: Branching conversations
- **Trade**: Exchange items
- **Give Quests**: Provide objectives
- **React**: To player actions or items

### 4. **Puzzles**
Challenges requiring:
- **Item Usage**: Right item in right place
- **Sequence**: Actions in correct order
- **Knowledge**: Information gathered from exploration
- **Mini-games**: Embedded puzzle mechanics

## Schema Structure

```yaml
# AdventureGameSchema YAML Structure
adventure_game:
  extends: narrative  # Inherits narrative features
  
  # World Definition
  world:
    # Locations/Rooms
    locations:
      forest_entrance:
        name: "Dark Forest Entrance"
        description: "Tall trees loom overhead, blocking most of the sunlight."
        
        # Connections to other locations
        exits:
          north: "forest_path"
          east: "village_square"
          
        # Items in this location
        items:
          - "old_lantern"
          - "mysterious_note"
          
        # State variations
        states:
          first_visit:
            description: "You've never seen a forest this dark and forboding."
          lantern_lit:
            description: "Your lantern casts dancing shadows on the trees."
            exits:
              down: "hidden_cave"  # New exit revealed
              
      hidden_cave:
        name: "Hidden Cave"
        description: "A damp cave revealed by your lantern light."
        requires: "lantern_lit"  # State requirement
        
    # Global location properties
    location_defaults:
      visit_tracking: true  # Track if player has visited
      auto_mapping: true    # Build map as explored
      
  # Inventory System
  inventory:
    max_items: 10  # -1 for unlimited
    weight_limit: 50
    
    # Special inventory types
    containers:
      - id: "backpack"
        extra_slots: 5
        
  # Item Definitions
  items:
    old_lantern:
      name: "Old Brass Lantern"
      description: "An antique lantern. It might still work."
      
      properties:
        carryable: true
        weight: 2
        
      states:
        unlit:
          description: "The lantern is dark."
          usable: true
        lit:
          description: "The lantern glows warmly."
          provides_light: true
          
      # Item interactions
      use:
        with_self:
          requires: ["matches"]
          effect: "light_lantern"
          
    golden_key:
      name: "Golden Key"
      carryable: true
      use:
        with: "locked_door"
        effect: "unlock_door"
        consumes: false  # Reusable
        
    # Combinable items
    rope:
      combine:
        with: "hook"
        creates: "grappling_hook"
        
  # Characters/NPCs
  characters:
    old_wizard:
      name: "Gandorf the Grey"
      location: "wizard_tower"
      
      # Dialogue trees
      dialogue:
        greeting:
          text: "Ah, a visitor! How rare these days."
          responses:
            - text: "I seek the Crystal of Power"
              next: "crystal_quest"
            - text: "Just passing through"
              next: "farewell"
              
        crystal_quest:
          text: "The Crystal? You'll need to prove yourself first."
          effect: "start_wizard_quest"
          
      # Trading
      trades:
        - wants: "magic_herbs"
          gives: "spell_scroll"
          
      # State-based behavior
      states:
        quest_complete:
          dialogue_override: "crystal_location"
          
  # Puzzles
  puzzles:
    door_puzzle:
      type: "combination"
      location: "ancient_temple"
      
      # What player needs to solve it
      solution:
        - use_item: "stone_tablet"  # Provides clue
        - sequence: ["red_gem", "blue_gem", "green_gem"]
        
      hints:
        - examine: "temple_mural"
          reveals: "The sunset, ocean, forest..."
          
      reward:
        unlock: "temple_inner_sanctum"
        items: ["ancient_artifact"]
        
    # Environmental puzzle
    bridge_puzzle:
      type: "environmental"
      
      solution:
        - move_object: "heavy_rock"
          to_position: "pressure_plate"
          
      effect: "extend_bridge"
      
  # Game Flow
  story:
    # Flags that track game state
    flags:
      - has_met_wizard: false
      - knows_about_crystal: false
      - temple_unlocked: false
      
    # Objectives/Quests
    quests:
      main_quest:
        name: "Find the Crystal of Power"
        stages:
          - id: "meet_wizard"
            description: "Find someone who knows about the Crystal"
            complete_when: "has_met_wizard == true"
            
          - id: "gather_gems"
            description: "Collect the three elemental gems"
            complete_when: "has_items(['red_gem', 'blue_gem', 'green_gem'])"
            
    # Endings
    endings:
      good_ending:
        requires:
          - has_item: "crystal_of_power"
          - flag: "saved_village"
        text: "You saved the realm with the Crystal's power!"
        
  # UI/Interaction
  interface:
    # Available commands/verbs
    verbs:
      - examine/look
      - take/get
      - use
      - talk
      - go/move
      - inventory
      - combine
      
    # Interaction feedback
    defaults:
      cannot_take: "You can't take that."
      no_exit: "You can't go that way."
      unknown_verb: "I don't understand that."
```

## Transpilation to UniversalGameSchema

### Locations → Room Objects
```yaml
# High-level
locations:
  forest_entrance:
    name: "Dark Forest Entrance"
    exits:
      north: "forest_path"

# Transpiles to:
object_types:
  room:
    properties:
      name: {type: STRING}
      description: {type: STRING}
      visited: {type: BOOL, initial: false}
    states:
      current_state: {type: STRING, initial: "default"}

  room_exit:
    properties:
      direction: {type: STRING}
      destination: {type: STRING}
      visible: {type: BOOL, initial: true}
      locked: {type: BOOL, initial: false}

setup:
  spawn_objects:
    - type: room
      id: "forest_entrance"
      properties:
        name: "Dark Forest Entrance"
        description: "Tall trees loom overhead..."
    
    - type: room_exit
      id: "forest_entrance_north"
      parent: "forest_entrance"
      properties:
        direction: "north"
        destination: "forest_path"
```

### Items → Interactive Objects
```yaml
# High-level
items:
  old_lantern:
    carryable: true
    states:
      lit: 
        provides_light: true

# Transpiles to:
object_types:
  item:
    properties:
      name: {type: STRING}
      description: {type: STRING}
      carryable: {type: BOOL}
      weight: {type: INT}
      in_inventory: {type: BOOL, initial: false}
    states:
      item_state: {type: STRING, initial: "default"}

instances:
  old_lantern:
    template: item
    properties:
      name: "Old Brass Lantern"
      carryable: "true"
      weight: "2"
    triggers:
      - name: "light_lantern"
        when:
          object_type: item
          property_changed: "used_with"
          condition: "this.id == 'old_lantern' && has_item('matches')"
        effects:
          - modify_property:
              target: {id: "this"}
              property: "item_state"
              value: "lit"
          - modify_property:
              target: {id: "current_room"}
              property: "current_state"
              value: "lantern_lit"
```

### Inventory → Container Object
```yaml
# High-level
inventory:
  max_items: 10

# Transpiles to:
object_types:
  inventory:
    properties:
      owner: {type: INT}
      max_items: {type: INT}
      current_weight: {type: INT, initial: 0}

setup:
  spawn_objects:
    - for_each: participant
      type: inventory
      id: "inventory_p{participant_id}"
      properties:
        owner: "{participant_id}"
        max_items: "10"

triggers:
  - name: "pick_up_item"
    when:
      object_type: item
      property_changed: "in_inventory"
      new_value: "true"
    effects:
      - change_parent:
          target: {id: "this"}
          new_parent: {type: inventory, property_match: {owner: "{current_player}"}}
      - remove_from_room:
          target: {id: "this"}
```

### Dialogue → Conversation Objects
```yaml
# High-level
dialogue:
  greeting:
    text: "Hello, traveler!"
    responses: [...]

# Transpiles to:
object_types:
  dialogue_node:
    properties:
      speaker: {type: STRING}
      text: {type: STRING}
      available_responses: {type: STRING}  # JSON array
    states:
      visited: {type: BOOL, initial: false}

  dialogue_response:
    properties:
      text: {type: STRING}
      next_node: {type: STRING}
      requires_flag: {type: STRING}
      effect: {type: STRING}

instances:
  wizard_greeting:
    template: dialogue_node
    properties:
      speaker: "old_wizard"
      text: "Ah, a visitor! How rare these days."
      available_responses: '["seek_crystal", "just_passing"]'
```

### Puzzles → Trigger Sequences
```yaml
# High-level
puzzles:
  door_puzzle:
    solution:
      sequence: ["red_gem", "blue_gem", "green_gem"]

# Transpiles to:
object_types:
  puzzle:
    properties:
      puzzle_id: {type: STRING}
      current_sequence: {type: STRING}  # JSON array
      solution: {type: STRING}  # JSON array
      solved: {type: BOOL, initial: false}

triggers:
  - name: "check_door_puzzle"
    when:
      object_type: puzzle
      property_changed: "current_sequence"
      condition: "this.puzzle_id == 'door_puzzle'"
    effects:
      - conditional:
          condition: "this.current_sequence == this.solution"
          effects:
            - modify_property:
                target: {id: "this"}
                property: "solved"
                value: "true"
            - unlock_exit:
                room: "ancient_temple"
                direction: "north"
```

## Example Games

### Classic Point-and-Click
```yaml
adventure_game:
  interface:
    type: "point_and_click"
    verbs: ["look", "take", "use", "talk"]
    
  locations:
    pirate_bar:
      hotspots:
        - id: "three_headed_monkey"
          examine: "Wow! A three-headed monkey!"
          talk: "It doesn't seem very talkative."
```

### Text Adventure (Interactive Fiction)
```yaml
adventure_game:
  interface:
    type: "text_parser"
    verbs: ["look", "take", "drop", "use", "go", "talk", "examine"]
    
  parser:
    synonyms:
      look: ["examine", "x", "check"]
      take: ["get", "grab", "pick up"]
```

### Modern Narrative Adventure
```yaml
adventure_game:
  interface:
    type: "choice_based"
    
  story:
    branching: true
    track_choices: true
    multiple_endings: true
```

## Key Benefits

1. **Genre-Appropriate**: Uses adventure game terminology
2. **Narrative Focus**: Built for story-driven games
3. **Flexible Interaction**: Supports multiple UI paradigms
4. **Rich World Building**: Locations, items, and characters
5. **Puzzle Support**: Various puzzle types built-in

## Validation Checklist

- ✅ Can represent Monkey Island (point-and-click)
- ✅ Can represent Zork (text adventure)
- ✅ Can represent Myst (first-person puzzles)
- ✅ Can represent Phoenix Wright (dialogue/investigation)
- ✅ Can represent The Walking Dead (choice-based)
- ✅ Can represent Professor Layton (puzzle-focused)

## Implementation Notes

1. **State Management**: Location and item states create dynamic worlds
2. **Inventory Logic**: Container system with weight/size limits
3. **Dialogue Trees**: Flexible branching with conditions
4. **Puzzle Framework**: Modular puzzle types that plug in
5. **Save System**: All state in properties makes saving trivial