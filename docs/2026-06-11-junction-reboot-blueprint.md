# Junction blueprint

**Status:** accepted direction, simplified 2026-07-10

Junction helps an educator and an AI agent turn an idea for a turn-based
educational game into a validated, simulated, playable artifact.

> Conversation is the authoring language. GameSpec is the bytecode.

The product loop is deliberately small:

1. An educator describes a game.
2. An agent writes a closed GameSpec document.
3. Junction validates and simulates it.
4. The educator plays the rendered game.
5. A class may join an authoritative online room.

The project earns a larger studio, registry, or genre surface only after this
loop works for real educators.

## Accepted names

- **Junction** is the project and product.
- **Catenin** is the engine: GameSpec, validation, simulation, and deterministic
  execution. Its implementation currently spans `@junction/spec` and
  `@junction/runtime`.
- **Cadherin** is the accessible DOM renderer in `@junction/renderer`.
- **Integrin** and **Occludin** are reserved names. They are not architectural
  commitments until explicitly approved.

Everything else uses a functional name: MCP, CLI, rooms, server, studio,
registry, and trust and safety. Biology is identity, not a requirement that
every future component receive a protein name.

## Current architecture

```text
GameSpec YAML
    |
    v
@junction/spec       parse, validate, diagnose
    |
    v
@junction/runtime    reduce, project, simulate
    |          \
    v           v
renderer       rooms
    |           |
    v           v
standalone     Node or Cloudflare transport
HTML

CLI and MCP expose the same engine and renderer capabilities.
```

### Catenin

- Closed Zod grammar with a Kubernetes-style manifest envelope.
- Pure reducer: `(state, action, rng) -> { state, events }`.
- Seeded randomness only; no clocks or ambient randomness in the engine.
- Total expression language: no loops, recursion, calls, or arbitrary code.
- Structured diagnostics at package boundaries.
- Per-seat projections prevent hidden information from crossing the wire.
- Headless simulation checks termination, seat balance, game length, and action
  use.

### Cadherin

- Framework-free DOM renderer.
- Keyboard operation, visible focus, ARIA live narration, and reduced motion.
- The same view model renders local and server-projected state.
- The CLI can emit a self-contained playable HTML file.
- Presentation data never changes game rules.

### Interfaces

- The CLI supports validate, simulate, play, render, and serve.
- The MCP server supports grammar discovery, references, scaffolding,
  validation, simulation, and rendering.
- Rooms provide authoritative state, server-sent legal moves, opaque piece
  handles, ordered events, and reconnect tokens.
- Node and Cloudflare are transport/deployment adapters, not alternate engines.

## Rules that stay

1. Agents write data; the runtime carries complexity.
2. The grammar is closed. A new key or effect is a grammar revision.
3. Determinism is non-negotiable.
4. Hidden information is projected server-side.
5. No arbitrary user code or `eval`.
6. Validation, security boundaries, and accessibility are not simplified away.
7. `pnpm check` must pass before merge.
8. One working implementation precedes an abstraction for multiple future
   implementations.

## Near-term roadmap

### 1. Author -- substantially proven

An agent can produce a valid GameSpec, simulate it, and render a playable page.

Remaining proof: repeat this with an educator who did not design the engine.

### 2. Play -- current milestone

Host the existing room runtime through a small Cloudflare Worker and one
SQLite-backed Durable Object per room. Verify:

- two browsers can complete a game;
- reconnect resumes the same seat;
- hidden identities never cross the wrong projection;
- Durable Object eviction does not lose authoritative game state;
- the public `/check` endpoint reports the play surface and WebSocket route.

The first hosted version needs no accounts, chat, registry, payment system, or
general-purpose room directory.

### 3. Create -- decide from observation

After real classroom play, build the smallest creator surface that removes the
largest observed obstacle. It may be a thin web screen, a desktop studio, or
better MCP workflows. Do not decide the full shell before observing the loop.

## Deferred, not promised

- A desktop studio
- A public game registry and remix lineage
- Adventure and RPG genre modules
- Learning analytics and district integrations
- Ratings, peer review, donations, or crowdfunding
- Paid hosted generation

These remain possible directions, not active architecture. Promote one only
when user evidence supplies a concrete requirement and exit criterion.

## History

The Kotlin Multiplatform prototype is preserved at the `kotlin-prototype` Git
tag. The development journal records why the project changed. Historical Kotlin
plans are intentionally absent from the active documentation so agents do not
mistake retired designs for current instructions.
