# GameSpec Capability Roadmap — can the grammar carry real games?

**Date**: 2026-06-11 · **Status**: Design note (extends blueprint §5, Addendum 2 §3) · answers "is the grammar capable of MTG/Hearthstone-class card games, and future-proof for board / graphical-adventure / RPG?"

## Verdicts up front

| Target | Verdict | Why |
|---|---|---|
| **Hearthstone-class** (mana, minions, buffs, battlecries, deathrattles) | ✅ Reachable — vocabulary, not architecture | Hearthstone's own cards are data over a fixed keyword engine (~20 keywords); the fireplace OSS sim proves the genre fits closed grammars. Our trigger-cascade reducer is already the right substrate. |
| **Full Magic: The Gathering** | ❌ Explicit non-goal | MTG is Turing-complete: the stack + priority, the layer system, replacement effects, 290 pages of comprehensive rules. Even Arena/Forge implement cards as per-card code. The last 10% costs 90% and serves no educational goal. **MTG-like** (mana/creatures/combat, no stack) ≈ Hearthstone-class: yes. |
| **Board games** (race, set-collection, tile-laying, checkers-class) | ✅ | Needs spatial topology + dice + variables — all closed vocabulary. Chess: expressible but verbose (castling/en-passant as vocab entries). Go: needs one scoring primitive (territory fill). |
| **Graphical adventure** (point-and-click, the founder's favorite) | ✅ — and the architecture is almost suspiciously well-shaped for it | Scenes = zones, inventory = owner zone (exists today), hotspots = pieces with coordinates, flags = variables, verbs = actions, "use X on Y" = two-target actions, dialogue = the planned Ink-style grammar compiled to the same trigger model. |
| **JRPG** (maps, stats, encounters, quests) | ✅ | RPG Maker is the 30-year existence proof that the whole genre is data + closed event commands. RPG = adventure + Hearthstone-combat + board-movement primitives — all shared. |

## The convergence (why this is one roadmap, not four)

Gap analysis across all five targets lands on the **same missing primitives**. Hearthstone's mana bar and Monkey Island's "has the monkey wrench" flag are the *same feature* (seat/global variables). "Deal damage equal to the card's cost" and "door opens if rustyKey in inventory" are the same feature (piece-scoped expressions). The universal core grows once; every genre module inherits.

## The vocabulary waves (all pre-`v1` lock — the registry locks the grammar at E5; until then it floats)

### Wave 1 — Mutable world (unlocks Hearthstone-foundations AND adventure-foundations)
- **`modifyProperty` effect** — minion takes damage; door becomes unlocked. (Direct port: the Kotlin prototype shipped this.)
- **Seat & global variables** — mana, score, flags, quest stage; `vars.mana`, `seatVars.score` expression roots; effects `setVar`/`addVar`.
- **Piece-scoped expressions** — `this.cost`, `target.health`, `source.attack` contexts in requires/when. (Port: Kotlin ConditionEvaluator did `source.health < 5`.)
- **Costs** — `action.cost: { var: mana, amount: "this.cost" }`, validated + auto-legality.
- **Runtime RNG** — dice, random draw/target; the reducer gains a seeded RNG stream (replays stay byte-identical).
- **Piece-attached triggers** — `on_play`/`on_death` per piece set (port of the original 2025 card-events design); state-check loop (defeat when `health <= 0`) as a standing trigger.

### Wave 2 — Resolution depth (the ONE structural change lives here)
- **Choice requests** — the reducer pauses mid-resolution and asks the seat to choose ("deal 3 damage to any target", "discover a card"): `applyAction → { state, events, pendingChoice? }` + `applyChoice`. Everything else in this roadmap is vocabulary; this changes the reducer contract and the wire protocol — **design it before v1 locks**, even if implementation lands later. (Known pattern: boardgame.io stages, fireplace choices.)
- **Multi-target actions** — "use key on door", "give item to character": ordered target lists with per-slot filters.
- **Reaction windows (stack-lite)** — optional interrupt phases ("respond?"); Hearthstone deliberately omitted MTG's stack and thrived — we adopt the same posture, as opt-in vocabulary.
- **Computed/derived properties** — the cold agent's documented gap ("sum of two addends"); declared formulas over total expressions, plus `winner` tiebreaker keys.

### Wave 3 — Space & story (board / adventure / rpg modules)
- **Zone topology** — `grid`/`hex` zones with coordinates; `adjacent()`, `distance()`, `path()` expression helpers; movement vocabulary (range, direction, jump, blocked-by).
- **Freeform + simultaneous turn orders** — single-seat adventures (`seats.min: 1` already validates) and party games.
- **Scene/hotspot kinds** — pieces with normalized coordinates over a scene image; visibility gated by expressions (hotspots appear when flags allow).
- **Ink-style dialogue grammar** — branching prose compiled to the same trigger/event model (per Addendum 2 decision #10: closed grammars, not necessarily YAML).
- **Scoring primitives** — territory fill, longest-chain, set-counting.

## Simulation keeps pace (the superpower compounds)

Random bots suffice for War; they will not probe a mana curve. The waves bring **heuristic bots** (greedy value functions over variables/stats; later MCTS-lite) so `simulate_game` can answer Hearthstone-class questions: curve health, dead cards, first-player advantage with mulligan. The verification loop is the moat precisely *because* it scales with grammar depth — every wave ships with its lints and its bot upgrades.

## Honest ceilings (written down so nobody relitigates)

1. **No arbitrary code in games — ever.** A mechanic the vocabulary can't express is a kernel PR (new closed vocab), not an escape hatch. If a game genuinely needs per-card Turing-complete scripts, it is out of scope by design.
2. **Full MTG, Dwarf-Fortress-class simulation, realtime/twitch**: non-goals. The mission is educational card/board/adventure/rpg.
3. **The cascade guard stays** (depth-limited trigger chains) — totality is a feature; infinite combos are a bug here, not a flex.
