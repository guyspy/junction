# Junction

**The agent-native game engine + studio where educators and AI agents make educational 2D games together** — card, board, adventure, RPG — and where every published game makes the next one easier to make.

> The scripting language is conversation. The GameSpec is the bytecode.

Junction is the third member of a family of agent-native platforms ([mantle](https://github.com/aotter/mantle) does content, clam does data, Junction does play) sharing one thesis: **agents write config, the runtime carries the complexity.**

## The constitution

Read [`docs/2026-06-11-junction-reboot-blueprint.md`](./docs/2026-06-11-junction-reboot-blueprint.md) — thesis, anatomy, decision register, roadmap E0–E7, and the three addenda that shaped it.

The moat is not the DSL. The moat is the **verification loop** (validate → simulate → critic → playtest) and the **corpus** (a registry of schema-validated, simulation-certified, remixable games).

## Anatomy

| Organ | Role |
|---|---|
| **Catenin** | the kernel — GameSpec grammar, deterministic reducer, validator, simulator (`@junction/spec` + `@junction/runtime`) |
| **Cadherin** | the renderer — accessible DOM component registry + game templates |
| **Connexon** | the online runtime — per-room actors, ordered event streams, classroom join codes |
| **Synapse** | the studio — agentic desktop app where teachers and an embedded agent co-create |
| **Plexus** | the community — registry of addressable, forkable, certified game artifacts |
| **Integrin** | the agent interface — the MCP server, the platform's front door |
| **Occludin** | trust & safety — publish gates, moderation, compliance |

## Quick start (E0 spike)

```bash
pnpm install
pnpm check                                   # boundaries → build → test
node packages/cli/dist/index.js validate games/war.yaml
node packages/cli/dist/index.js simulate games/war.yaml --games 200
node packages/cli/dist/index.js play games/memory-match.yaml --seat 0   # play a human turn
```

Reference games: [`games/war.yaml`](./games/war.yaml) (chance, hidden decks) and
[`games/memory-match.yaml`](./games/memory-match.yaml) (flip mechanics, face-up/down
state, go-again). `play` shows only your seat's view — the per-seat projection that the
online runtime will reuse unchanged.

## History

The Kotlin Multiplatform prototype (Sessions 1–4, 2025-07 → 2026-03) is archived with honor at the [`kotlin-prototype`](../../tree/kotlin-prototype) tag — the prototype that taught us the shape. The development journal lives in [`journal/claude/`](./journal/claude/).

## License

Apache-2.0 (engine). Published GameSpecs default to CC BY.
