# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Junction is an educational gaming platform inspired by cell junction biology, allowing educators to create online 2D card-based board games with AI assistance. The platform provides a complete gaming ecosystem with creation tools, gameplay platform, and community features.

## Commands

Since this is an early-stage project with planning documents, there are currently no build/test commands available. The project directories are primarily empty and contain documentation only.

## Architecture & Components

This project follows a microservices architecture with biological naming conventions based on cell junction proteins:

### Core Services
- **`cadherin/`** - DSL interpreter for game rules (server & client shared)
- **`game-engine/`** - Core game execution engine (Kotlin + Quarkus)  
- **`platform-services/`** - User auth, game rooms, community features (Kotlin + Quarkus)
- **`creator-tools/`** - Game creation and editing tools (TypeScript)
- **`web-client/`** - Frontend application (TypeScript)

### Future Components (planned naming)
- **`claudin`** - Security and permission management
- **`integrin`** - External system integration interfaces  
- **`connexin`** - Real-time communication system
- **`desmosome`** - Persistent data connections
- **`cytoplasm`** - Shared data layer

### Technology Stack
- **Backend**: Kotlin + Quarkus microservices
- **Frontend**: TypeScript
- **Database**: MongoDB
- **Development Approach**: Test Driven Development
- **Communication**: WebSocket for real-time gaming

### Data Architecture
The system uses MongoDB with collections for:
- Users (authentication, profiles, credits)
- Games (DSL definitions, assets, metadata)
- GameSessions (active game state, events, players)

## Development Phases

The project is planned in 4 phases:
1. **Game Engine Core** (Priority) - DSL design, game rules engine, card systems
2. **Platform Infrastructure** - User management, game rooms, real-time communication  
3. **Creator Tools** - Visual game editor, AI-assisted creation, asset management
4. **Community & Business** - Rating system, crowdfunding, donation credits

## Cadherin DSL

The core DSL (Domain Specific Language) for defining card games features:
- Natural language-like syntax for educators
- Type system with game-specific types (Card, Player, Deck, Hand)
- Event-driven architecture for game rules
- Security sandboxing to prevent malicious code
- Compilation to intermediate representation for execution

## Project Structure

```
junction/
├── docs/                     # Development planning documents
│   ├── overview.md           # Overall project planning  
│   ├── architecture.md       # Technical architecture design
│   ├── cadherin/            # DSL documentation
│   ├── game-engine/         # Game engine plans
│   ├── platform/            # Platform infrastructure plans
│   ├── creator-tools/       # Creation tools plans
│   └── community/           # Community features plans
├── cadherin/                # DSL interpreter (empty - planned)
├── game-engine/             # Game engine core (empty - planned)
├── platform-services/      # Platform microservices (empty - planned)
├── creator-tools/           # Creation tools (empty - planned)
└── web-client/             # Frontend application (empty - planned)
```

## Key Development Considerations

- **Education Focus**: All features prioritize learning and teaching use cases
- **Multilingual**: Documentation is primarily in Traditional Chinese
- **Social Impact**: Includes "pay-it-forward" credit system for disadvantaged children
- **Security**: Emphasis on safe execution of user-generated game content
- **Performance**: Target <100ms response time, support 1000+ concurrent games
- **AI Integration**: OpenAI/Claude API for assisted game creation

## Current Status

This is an early-stage project in the planning phase. The source directories are empty, containing only documentation. Implementation will begin with the Cadherin DSL and game engine core as Phase 1 priorities.