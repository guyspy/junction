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
- **Cadherin**: PixiJS game visuals with accessible DOM controls and narration,
  driven by one projected view-model and emitted as self-contained HTML
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
WebSockets. D1 stores educator-owned GameSpec drafts, immutable revisions, and
the published revision used to create rooms. Deployment credentials stay out
of the repository.

## Hosted agent workflow

The deployed Worker exposes an authenticated Streamable HTTP MCP endpoint at
`/mcp`. It reuses the local MCP tools and adds the smallest hosted authoring
loop:

```text
describe/scaffold -> validate/simulate -> create/update -> publish -> play
```

The hosted-only tools are `create_game`, `get_game`, `list_my_games`,
`update_game`, and `publish_game`. Updates replace the complete YAML and require
the expected revision. Publishing re-validates and simulates that exact
revision before returning a stable play URL.

For the current single-educator alpha, authentication is one 64-character
hexadecimal bearer token. The Worker stores only an HMAC verifier; keep the
token in a password manager or environment variable, never in Git. A coding
agent can connect with an MCP client that sends
`Authorization: Bearer $JUNCTION_MCP_TOKEN`. For Codex:

```bash
export JUNCTION_MCP_TOKEN='...'
codex mcp add junction \
  --url https://junction-rooms.phsu-31c.workers.dev/mcp \
  --bearer-token-env-var JUNCTION_MCP_TOKEN
```

Apply D1 migrations before the first deployment:

```bash
pnpm --filter @junction/cloudflare exec wrangler d1 migrations apply junction-games --remote
pnpm --filter @junction/cloudflare deploy
```

PixiJS is Cadherin's default visual engine. It consumes the projected
view-model and sends ordinary moves; the accessible DOM controls and narration
remain the semantic layer and fallback. Game art is generated from GameSpec
properties as SVG-backed textures. There is deliberately no asset upload,
asset service, renderer mode switch, or marketplace.

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
