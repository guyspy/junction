# Junction

Junction turns a data-defined educational game into something an agent can
validate, simulate, render, and host for a class.

> Conversation is the authoring language. GameSpec is the bytecode.

The accepted direction is intentionally narrow. Read the
[blueprint](./docs/2026-06-11-junction-reboot-blueprint.md) before changing
architecture or grammar.

## What exists

- **Catenin**: GameSpec, deterministic execution, validation, and simulation
  (`@junction/spec` + `@junction/runtime`).
- **Cadherin**: accessible DOM rendering and self-contained playable HTML
  (`@junction/renderer`).
- **CLI and MCP**: agent and developer interfaces over the same capabilities.
- **Rooms**: authoritative multiplayer state, per-seat projections, reconnect,
  and Node/Cloudflare transports.

Other biological names are not part of the architecture. Integrin and Occludin
are reserved pending explicit approval; future products use functional names.

## Quick start

```bash
pnpm install
pnpm check
node packages/cli/dist/index.js validate games/war.yaml
node packages/cli/dist/index.js simulate games/war.yaml --games 200
node packages/cli/dist/index.js play games/memory-match.yaml --seat 0
node packages/cli/dist/index.js render games/war.yaml
node packages/cli/dist/index.js serve games/war.yaml
```

`render` writes one self-contained HTML file with the engine, renderer,
GameSpec, and simulation badges. `serve` opens an authoritative local room.

The Cloudflare adapter lives in `packages/cloudflare`:

```bash
pnpm --filter @junction/cloudflare dev
pnpm --filter @junction/cloudflare test
pnpm --filter @junction/cloudflare deploy
```

It serves one SQLite-backed Durable Object per room and uses hibernating
WebSockets. Deployment credentials stay in the environment; they are never
stored in the repository.

Reference games:

- [`war.yaml`](./games/war.yaml)
- [`memory-match.yaml`](./games/memory-match.yaml)
- [`make-ten-match.yaml`](./games/make-ten-match.yaml)
- [`math-duel.yaml`](./games/math-duel.yaml)

## History

The retired Kotlin Multiplatform prototype is preserved at the
[`kotlin-prototype`](../../tree/kotlin-prototype) tag. The development journal
lives in [`journal/claude/`](./journal/claude/).

Apache-2.0. Published GameSpecs default to CC BY.
