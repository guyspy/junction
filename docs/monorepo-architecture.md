# Junction Monorepo Architecture

## Overview

Junction is designed as a multi-technology monorepo supporting educational gaming platform services. Each service is independent with its own technology stack and build system.

## Design Principles

### Service Independence
- **Technology Agnostic**: Services can use Kotlin, Java, JavaScript, TypeScript, etc.
- **Build System Choice**: Gradle, npm, Maven, or any appropriate tooling
- **No Forced Sharing**: Services integrate via APIs, not shared code modules
- **Clean Boundaries**: Each service is self-contained and deployable

### Monorepo Benefits
- **Unified Versioning**: Coordinated releases across services
- **Shared Documentation**: Centralized architecture and API docs
- **Development Coordination**: Cross-service changes in single PRs
- **Consistent Tooling**: Shared CI/CD, linting, and quality gates

## Current Structure

```
junction/
├── cadherin/                    # Game Engine Service (Kotlin Multiplatform)
│   ├── src/                     # Core engine code
│   ├── examples/                # Platform-specific demos
│   └── game-samples/            # YAML game definitions
├── docs/                        # Shared documentation
├── gradle/                      # Shared Gradle configuration
├── build.gradle.kts             # Root build (multi-tech)
└── settings.gradle.kts          # Project structure
```

## Future Services

### Planned Services
- **occludin/**: Quarkus server (Java + MongoDB)
- **phaser-renderer/**: Game renderer (Pure JS + Phaser)
- **ai-agent/**: Game generation (Python + ML models)
- **web-ui/**: Management interface (React + TypeScript)

### Integration Patterns
- **API Communication**: REST, GraphQL, WebSocket
- **Event Streaming**: Apache Kafka for real-time events
- **Shared Data**: MongoDB collections, Redis cache
- **Package Publishing**: npm, Maven Central for reusable components

## Gradle Configuration

### Root Build Strategy
- **Plugin Declaration**: All technology plugins declared at root
- **Service-Specific Groups**: Each service maintains its own Maven coordinates
- **Version Catalog**: Shared dependency versions across compatible services
- **Conditional Configuration**: Service-specific build logic

### Example Service Integration
```kotlin
// Root build.gradle.kts
configure(subprojects.filter { it.name == "cadherin" }) {
    group = "org.junction.cadherin"
}

configure(subprojects.filter { it.name == "occludin" }) {
    group = "org.junction.occludin"
}
```

## Non-Gradle Services

### Pure JavaScript/TypeScript Services
- Use standard npm/yarn with package.json
- Independent of Gradle build system
- Integrate via published packages or APIs
- Example: Phaser game renderers, React UIs

### Python Services
- Use poetry/pip with pyproject.toml
- Completely independent toolchain
- Docker-based deployment
- Example: AI model services

## Development Workflow

### Adding New Services
1. Create service directory at root level
2. Initialize with appropriate build system
3. Update root documentation
4. Add service-specific CI/CD pipeline
5. Configure cross-service integration points

### Cross-Service Changes
1. Make changes across multiple services in single PR
2. Coordinate API changes with interface versioning
3. Test integration points with service contracts
4. Deploy services in dependency order

## Service Communication

### Runtime Integration
- **Cadherin** publishes game events → **Occludin** processes
- **Occludin** serves game data → **Web UI** displays
- **AI Agent** generates games → **Cadherin** validates

### Build-Time Integration
- **Cadherin** publishes to npm → **Phaser Renderer** imports
- **Shared Types** via TypeScript definitions
- **API Contracts** via OpenAPI specifications

## Benefits for AI Agents

### Multi-Language Support
- AI can work with preferred languages per domain
- Game logic (Kotlin), Server (Java), UI (TypeScript), ML (Python)

### Service Isolation
- Changes to one service don't break others
- AI can focus on specific service without monolith complexity

### API-First Integration
- Clear contracts between services
- AI can understand and modify service boundaries

## Deployment Strategy

### Service Independence
- Each service deployable independently
- Docker containers for consistent environments
- Kubernetes orchestration for scalability

### Coordinated Releases
- Version tagging across services
- Integration testing in staging
- Blue-green deployment for zero downtime

---

This architecture provides maximum flexibility for growth while maintaining monorepo benefits for coordination and shared tooling.