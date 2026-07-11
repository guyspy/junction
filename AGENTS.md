# AGENTS.md

This repo's agent entry point is **[CLAUDE.md](./CLAUDE.md)** — read that first (family pattern: this file only points you at the right place).

## Quick links

- Constitution / blueprint → [`docs/2026-06-11-junction-reboot-blueprint.md`](./docs/2026-06-11-junction-reboot-blueprint.md)
- Architecture + house rules → [CLAUDE.md](./CLAUDE.md)
- Authoring a game (the agent workflow) → [`skills/game-designer/SKILL.md`](./skills/game-designer/SKILL.md)
- Junction MCP server → `node packages/mcp/dist/stdio.js` (tools: describe_grammar, scaffold_game, validate_game, simulate_game, render_game, list/get_reference_game)
- Dev journal (read at session start, write at session end) → [`journal/claude/`](./journal/claude/)
- The archived Kotlin prototype → git tag `kotlin-prototype`
- CI gate → `pnpm check` (boundaries → build → test); it must be green before any PR
