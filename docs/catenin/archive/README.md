# Archived Documentation

This directory contains historical documentation from the early development phases of Catenin, before the universal object system was finalized.

## Contents

### Day-by-Day Implementation Docs (Historical)
- `day1-kotlin-multiplatform-setup.md` - Initial Kotlin Multiplatform setup
- `day1.5-sdk-monorepo-restructure.md` - Monorepo restructuring
- `day2-player-state-and-actions.md` - Player state and action system (pre-universal)
- `day3-event-system.md` - Original event system design (superseded)
- `day3.5-universal-object-system-refactor.md` - Transition to universal system
- `day4-turn-management-and-scoring.md` - Turn management (superseded by TurnBasedSchema)
- `day5-win-conditions-and-complete-game.md` - Win conditions (superseded by universal triggers)

### Reports
- `delivery-report.md` - Day 2 delivery analysis (pre-universal)

## Note

These documents represent the evolution of thinking that led to the current universal object system. They show the progression from game-specific models to the truly universal approach now implemented.

The current active documentation is:
- `../universal-yaml-schema.md` - Current universal schema design
- `../high-level-game-schemas.md` - Layered schema hierarchy
- `../board-game-schema.md` - BoardGameSchema design
- `../adventure-game-schema.md` - AdventureGameSchema design
- `../cadherin-integration.md` - UI integration design

## Architectural Evolution

1. **Days 1-2**: Traditional game engine with Player/Card/GameState classes
2. **Day 3**: Event system with hardcoded game concepts
3. **Day 3.5**: Breakthrough - universal object/property/trigger system
4. **Current**: Layered schemas transpiling to universal primitives

The journey from specific to universal represents a major architectural insight that makes Catenin truly powerful for any game type.