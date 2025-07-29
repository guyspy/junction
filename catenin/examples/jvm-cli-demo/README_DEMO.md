# JVM CLI Demo - Universal Game Engine

This demo showcases the complete Universal Game Engine implementation including:
- ObjectFactory for creating game objects
- GameInitializer for setting up participants
- TriggerEngine for processing events
- EffectEngine for executing effects
- TargetResolver for finding targets
- GameEngine orchestrating everything

## Features Demonstrated

1. **Object Creation**: Create units with different stats (Warriors, Archers, Mages)
2. **Spell Casting**: Cast spells like Lightning Bolt, Heal, and Armor Boost
3. **Combat System**: Attack units with damage calculation including armor
4. **Trigger System**: Automatic armor boost when unit health drops low
5. **State Management**: Track participant wins, turn counts, and unit states

## Running the Demo

```bash
# From the project root
./gradlew :catenin:examples:jvm-cli-demo:run

# Or run with more memory if needed
./gradlew :catenin:examples:jvm-cli-demo:run -Dorg.gradle.jvmargs="-Xmx1024m"
```

## Game Flow

1. Enter number of players (2-4)
2. Enter player names
3. Use the interactive menu to:
   - Create units for players
   - Cast spells on units
   - Attack with units
   - View all game objects
   - View active triggers
   - End turns

## Example Gameplay

```
=== Catenin Universal Game Engine - Interactive Demo ===

Welcome to Battle Arena!
A game for 10-99 year olds

Enter number of players (2-4): 2
Enter name for player 1: Alice
Enter name for player 2: Bob

✅ Game initialized!
Players: Alice, Bob

============================================================
GAME STATE
============================================================

Participants:
  Alice:
    Wins: 0
    Turn: 0
    Active: true
  Bob:
    Wins: 0
    Turn: 0
    Active: true

Units on battlefield:
  (No units)

Actions:
1) Create a unit
2) Cast a spell
3) Attack with unit
4) View all objects
5) View triggers
6) End turn
0) Exit game
Choose action: 1
```

## Trigger Example

When a unit's health drops below a certain threshold, the low health trigger automatically grants +2 armor:

```
⚔️ Warrior attacks Archer for 2 damage!
Unit gained +2 armor due to low health!
```

This demonstrates the event-driven architecture where property changes can trigger cascading effects.