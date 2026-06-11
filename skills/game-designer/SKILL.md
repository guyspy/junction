---
name: game-designer
description: Design a validated, balanced, playable educational card/board game for Junction by authoring a GameSpec through the Integrin MCP tools. Use whenever someone wants to make, modify, or balance a Junction game.
when_to_invoke: "make a game", "build a card game", "a game that teaches X", "balance my game"
applies_to: junction games.junction.aotter.net/v1alpha1
---

# Designing a Junction game

You are authoring a **GameSpec** — a declarative description of a card/board game that
Junction's deterministic engine runs and *judges*. You never write code; you write a
validated data document, and the engine proves it is playable before anyone trusts it.

The thesis: **you write config, the runtime carries the complexity.** Your job is the
design and the data. The runtime handles turns, hidden information, shuffling, scoring,
and animation.

## The tools (Integrin MCP)

- `describe_grammar` — the complete closed vocabulary. **Call this first.** Never guess a
  key or enum value; if it isn't in the grammar, it doesn't exist.
- `list_reference_games` / `get_reference_game` — fully-worked examples (`war`,
  `memory-match`). Read one close to what you're building before writing your own.
- `scaffold_game` — a valid, terminating skeleton to start from. **Always start here** for
  a new game rather than writing from a blank page.
- `validate_game` — schema + semantic check. Returns diagnostics with a `path`, what was
  `expected`, `candidates`, and a `suggestion`. **Call after every edit.**
- `simulate_game` — plays the game headlessly hundreds of times and reports termination,
  seat balance, turn length, and draw rate. **This is the gate before you declare done.**

## The workflow (do not skip steps)

1. **Understand the goal.** What is the learning objective? Ages? How many players? How
   long should a game take? If the user hasn't said, ask — these shape the design.
2. **Learn the vocabulary.** Call `describe_grammar`. Skim a reference game with
   `get_reference_game` if one is close.
3. **Scaffold.** Call `scaffold_game` with the genre, title, seat range, and a one-line
   description. You now hold a valid, terminating starting point.
4. **Customize incrementally.** Edit the YAML toward the design — rename pieces, set
   property ranges that encode the lesson (fractions, sums, vocabulary…), adjust scoring.
   After **each** change, call `validate_game` and fix every error using its `suggestion`.
5. **Playtest.** Call `simulate_game`. Read the verdicts:
   - Termination must be **100%** — any capped games mean the rules can deadlock.
   - Seat win rates should sit near uniform — a large skew means first-player advantage.
   - A very high draw rate means the game rarely resolves; add a tiebreaker.
   - Actions never used are dead rules — cut or fix them.
6. **Iterate** 4–5 until validate is clean and simulate is healthy. Then present the game.

## How the pieces fit (mental model)

A game is **Places, Pieces, Rules, Goals**:
- **Places** are `zones` (a `deck`, a `hand`, a `board` cell). `owner: seat` zones exist
  once per player; `visibility` decides who sees what (a face-up piece is always visible).
- **Pieces** (`cards`, `tokens`) carry typed `properties`. Encode the lesson in the
  property *values* — that is where the math/vocabulary/science lives.
- **Rules** are `actions` (what a player may do on their turn) and `triggers`
  (event → condition → effects) that the engine fires automatically.
- **Goals** are the `end` condition and `winner`.

## Encoding a learning objective (the point of all this)

The lesson lives in piece properties and the win condition. "Highest fraction wins the
trick" teaches fraction comparison. "Match the sum to 10" teaches addition. Make the
*winning strategy* require the skill you want practised — then `simulate_game` tells you
whether players can win while ignoring it (if seat balance is near-random with random
bots, the skill genuinely matters).

## Don't

- Don't invent grammar. If `describe_grammar` doesn't list it, you can't use it.
- Don't write a game from scratch — scaffold, then customize.
- Don't declare a game done before `simulate_game` shows 100% termination and reasonable
  balance. Plausible-looking YAML that deadlocks is the most common failure.
- Don't hand the user raw diagnostics — read the `suggestion`, fix it, move on.
