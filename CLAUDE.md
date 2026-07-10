# CLAUDE.md

Agent orientation for Junction. Humans start with [README.md](./README.md).

## Product boundary

Junction helps an educator and an AI agent author a closed GameSpec, validate
and simulate it, render it as an accessible game, and optionally host an
authoritative classroom room.

Read the [blueprint](./docs/2026-06-11-junction-reboot-blueprint.md) before
changing architecture or grammar. Catenin and Cadherin are accepted names.
Integrin and Occludin are reserved pending explicit approval. Use functional
names for every other component.

The Kotlin prototype is archived at `kotlin-prototype`; do not resurrect it.

## Commands

```bash
pnpm install
pnpm check
pnpm build
pnpm test
node packages/cli/dist/index.js validate games/war.yaml
node packages/cli/dist/index.js simulate games/war.yaml --games 200 --seed 42
```

`pnpm check` is the merge gate: boundaries, build, then tests.

## Packages

```text
packages/spec        closed GameSpec grammar, parser, diagnostics
packages/runtime     deterministic reducer, projection, simulation
packages/renderer    Cadherin DOM renderer and standalone page builder
packages/rooms       authoritative room core and wire protocol
packages/node        Node WebSocket room adapter
packages/cloudflare  Worker + Durable Object room adapter
packages/mcp         Junction MCP server
packages/cli         validate/simulate/play/render/serve commands
```

Allowed dependency direction:

```text
spec <- runtime <- rooms <- transport adapters
  ^        ^          ^
  +----- renderer ----+
  +-------- MCP / CLI
```

`spec`, `runtime`, `renderer`, and `rooms` must remain platform-neutral. Node
and Cloudflare APIs belong only in their adapters.

## House rules

1. Diagnostics use `{code, phase, severity, path, value?, expected?,
   candidates?, suggestion?, message}`. Messages come from the shared formatter.
2. Package boundaries return result objects rather than throwing expected user
   errors. Tests assert diagnostic codes.
3. The grammar is closed. New keys, effects, expression roots, or events update
   schema, lints, examples, and docs together.
4. The reducer is pure. All randomness comes from a seeded RNG. No clocks or
   ambient randomness in `spec` or `runtime`.
5. Expressions are total: no loops, recursion, calls, or side effects.
6. The manifest envelope is `apiVersion`, `kind`, `metadata.name`, and `spec`.
7. Hidden information is projected on the authoritative side before transport.
8. Prefer the smallest working implementation. Do not scaffold future products.
9. Conventional commits; branches `feature/YYYYMMDD_name` or
   `spike/YYYYMMDD_name`; PRs target `main`; merge rather than squash.

## Journal

At the start of a working session, read [`journal/claude/README.md`](./journal/claude/README.md)
and the recent entries. At the end of a substantive implementation session,
add `journal/claude/YYYY-MM-DD-session-N.md` in the established reflective
format and update the README.
