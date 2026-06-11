# Junction Reboot Blueprint — Day One

**Date**: 2026-06-11
**Status**: PROPOSAL — once accepted, this supersedes all prior architecture documents. Nothing in the repo has been deleted; the Kotlin implementation stands as the reference prototype that taught us what the product is.
**Revision**: v1.3 (same day) — v1.1 reconciled the family addendum; v1.2 the studio pivot (Addendum 2); v1.3 the studio architecture (Addendum 3: one shell, three genre workspaces, one narrative). Addenda remain as the record of what changed and why.

## The Goal (verbatim)

> build agent native educational card/board game online engine, runtime, playground and a community

Everything below is derived from this sentence and from first principles, as if no code existed. Research basis: 7 parallel web-research agents (2026-06-11) covering agent-native platform practice, MCP ecosystem, structured-generation reliability, hosting economics, COPPA/accessibility compliance, school network reality, frontend stack state, and the card-game/education competitive landscape.

---

## 1. Thesis

**Junction is the agent-native game engine + studio where educators and AI agents make educational 2D games together — card, board, adventure, RPG — and where every published game makes the next one easier to make.**

The 2025 thesis asked: *can we design a DSL simple enough for AI to write?* That question is obsolete. In 2026, agents can write almost anything. The scarce resources are now:

1. **Verification** — is the game valid, playable, terminating, balanced, age-appropriate, and actually educational?
2. **Distribution** — can a teacher have a class playing it in 30 seconds, on school Chromebooks, legally?
3. **Accumulation** — does each created game make the platform smarter, or just bigger?

So the moat is not the DSL. **The moat is the verification loop and the corpus.** A deterministic engine that can headlessly play a proposed game ten thousand times and report "player 1 wins 73% of the time, these three cards are never playable, and the game cannot end before turn 40" is what turns agent output from plausible into *good*. No LLM provider can replicate a growing registry of schema-validated, simulation-certified, remix-lineaged educational games with play telemetry.

**The division of labor:** humans direct, judge, curate, and teach. Agents fabricate, test, repair, and remix. Kids play.

### What changed from the 2025 thesis

| | 2025 (Catenin v1) | 2026 (this blueprint) |
|---|---|---|
| Primary creator | Human dev, AI-assisted | **Agent, human-directed** |
| Core artifact | Universal YAML schema (any game) | **Card/board-specialized GameSpec** (rich semantics) |
| Hero feature | The engine runs games | **The engine judges games** (validate → simulate → critic → playtest) |
| Front door | Library API + future GUI | **MCP server** (web UI is a client of the same capability layer) |
| Implementation | Kotlin Multiplatform | **TypeScript end-to-end** |
| UI strategy | Canvas-first (React + PixiJS) | **DOM-first, accessible; canvas as later juice layer** |
| Community | Future service | **Registry, not gallery** (shadcn model: addressable, agent-installable artifacts) |
| Economics | Unspecified | **BYO-agent; play is near-free; never subsidize generation** |

---

## 2. What "agent-native" commits us to

Grounded in the 2026 state of practice (Netlify's AX pillars: Access/Context/Tools/Orchestration; Every's agent-native architecture: parity, granularity, composability, emergence, self-improvement; Anthropic's tool-design guidance; the Figma/shadcn/Roblox precedents):

1. **Parity.** Anything a human can do through the UI, an agent can do through MCP tools. The MCP server (Integrin) is the product's front door; the Synapse studio (and any web surface) consumes the same capability layer — the studio is the first-party MCP client. We follow the Figma March 2026 template: MCP write-access shipped together with official skills.
2. **The artifact is data, never code.** Games are GameSpec documents validated against a published schema. No user-uploaded code, ever — that single decision buys safe remixing, static analysis, deterministic simulation, kid-safe distribution, and a tractable moderation problem.
3. **Verification over generation.** Every error is machine-actionable: JSON path, what was expected, a concrete candidate fix, and where possible a counterexample play-trace. Research is unambiguous: repair loops converge only when validator feedback is structured; naive retries cycle forever.
4. **Incremental authoring, not one-shot.** Agents build games through tool calls with continuous validation (scaffold → add pieces → add triggers → validate → simulate), the same way they edit code. One-shot generation of 300–800-line documents fails structurally <50% of the time, and forcing deep recursive schemas through provider structured outputs both breaks (≈5-level nesting limits) and taxes reasoning quality 10–30%. The schema serves the *validator*, not the decoder.
5. **The onboarding trinity.** MCP server + official `game-designer` SKILL.md (open standard, 30+ agent platforms) + AGENTS.md (60k+ projects, Linux Foundation stewardship). The platform teaches agents how to use it well.
6. **Provenance everywhere.** Every GameSpec carries: fork lineage, creator attribution (human and model), license, generation provenance (C2PA-style), and an educator-facing changelog. Aligned with where Creative Commons is heading in its 2026 education pilots (attribution as requirement, not preference).
7. **Agent evals are the QA suite.** A standing benchmark of realistic tasks ("build a fractions card game for 8-year-olds, 2–4 players, 15 minutes") run weekly by real agents against the MCP server, measuring end-to-end success rate and tokens-to-valid-game. Tool descriptions get tuned against this. (Anthropic's methodology; "dramatic improvements" are documented from description-tuning alone.)

---

## 3. The flywheel

```
educator intent ──▶ agent authors GameSpec ──▶ verification loop certifies it
                                                        │
        ┌───────────────────────────────────────────────┘
        ▼
   published to registry (lineage, license, QA badges)
        │
        ▼
   corpus grows ──▶ richer few-shot/skills/templates for agents
        │                                   │
        ▼                                   ▼
   teachers discover & remix ──▶ play telemetry improves balance lint
        │
        └──▶ more educator intent
```

Each published game is raw material: agents search the registry, fork proven mechanics, and remix — one MCP call. Play telemetry (anonymous event logs) feeds back into the simulator's heuristics. This is the shadcn registry model applied to games, and **nobody in the educational space has it**: Kahoot/Blooket/Wayground stop at AI quiz-fill; Websim/Rosebud generate unvalidated code. "Validated, rule-rich educational games" is an open niche (confirmed again June 2026).

---

## 4. Anatomy

The biological naming deepens — every organ is a junction protein or structure, and each metaphor is load-bearing:

| Component | Biology | Role |
|-----------|---------|------|
| **Catenin** | carries signals into the nucleus | The kernel: GameSpec schema, deterministic reducer, expression evaluator, validator, simulator |
| **Cadherin** | makes the cell visible & touchable | The renderer: accessible DOM component registry, game templates, MCP Apps packaging |
| **Connexon** | gap-junction channel between cells | The online runtime: per-room actors, ordered event streams, classroom join codes |
| **Synapse** | junction where minds meet | The studio: agentic desktop app (Electrobun, nacre pattern) — one shell, genre workspaces, one creation loop (Addendum 3); the web carries play |
| **Plexus** | interwoven network | The community: registry of addressable, forkable, certified game artifacts |
| **Integrin** | transduces signals across the membrane, both ways | The agent interface: the MCP server — the platform's front door |
| **Occludin** | tight junction; controls what may pass | Trust & safety: publish-time review, moderation, compliance guardrails |

```
            agents (Claude, ChatGPT, Codex…)        humans (educators)
                      │                                   │
                      ▼                                   ▼
               ┌─ Integrin ─┐                      ┌─ Synapse ──┐
               │ MCP server │◀────same capability──│   studio   │
               └─────┬──────┘         layer        └─────┬──────┘
                     ▼                                   ▼
        ┌────────────────────── Catenin ──────────────────────────┐
        │  GameSpec schema · reducer · expressions · seeded RNG   │
        │  validate · simulate · per-seat projection              │
        └──────┬──────────────────┬───────────────────┬───────────┘
               ▼                  ▼                   ▼
         Cadherin            Connexon             Plexus ◀── Occludin
       (DOM renderer,     (DO rooms, WSS,      (registry, lineage,    (publish
        MCP Apps iframe)   join codes)          QA badges)             gates)
```

One kernel, four execution habitats: **browser** (solo play, instant preview), **workerd** (authoritative rooms), **Node/Bun** (CLI, CI, fallback server), **agent sandboxes** (`npm i @junction/catenin` → simulate). This constraint is what forces the language decision (§7).

---

## 5. GameSpec — the artifact

### Specialized semantics on a universal core (refined in Addendum 2)

The 2025 universal object system was elegant — and it pushed all *meaning* into metadata. When the engine doesn't know what a "hand" is, every game re-invents hands, the renderer can't auto-render one, the validator can't lint "draw from empty deck," and analytics can't say "cards never drawn." Junction is genre-locked to **2D card/board/adventure/RPG**. Embrace it. Semantic concepts are what make auto-rendering, deep validation, and learning analytics *fall out for free* — and they shrink what agents must specify (less boilerplate = fewer failure modes).

The universality we keep lives **below** the genres: a small universal core (entities, typed properties, total expressions, ECA triggers, event log, seeded RNG — Addendum 2 §3) carries cross-genre expressiveness, while genre modules (tabletop first; adventure, RPG later) own the semantics and contribute their own kinds. The sketch below shows the tabletop module's `Game` shape.

### Core concepts (v1alpha sketch)

GameSpec wears the family's Kubernetes-style manifest envelope (mantle ADR-0001 pattern); the registry stamps lineage and attribution into `metadata`, while `spec` holds only the game itself:

```yaml
apiVersion: games.junction.aotter.net/v1alpha1
kind: Game
metadata:
  name: fraction-duel
  # provenance lives here, registry-stamped: lineage (forked_from chain),
  # authors (human + model), license, changelog
spec:
  meta:         # description, ages, seats (min/max), est. minutes
  learning:     # objectives, knowledge tags, (later) standards alignment
  seats:        # roles, teams, simultaneous vs sequential
  zones:        # deck, hand, discard, play_area, grid/hex board cells…
                # each: owner, visibility (all/owner/none), ordering, capacity
  pieces:       # cards, tokens, dice, tiles — typed properties + knowledge tags
  setup:        # initial distribution (shuffle seeds come from the room)
  turn:         # rounds → phases → steps; per-phase allowed actions
  actions:      # move/flip/draw/play/choose/roll… + conditions (expressions) + costs
  triggers:     # event-condition-action rules (the 2025 paradigm, kept)
  end:          # win/lose/draw conditions + scoring
  presentation: # template id, theme tokens, layout slots (hints only)
```

### Design rules

- **Canonical JSON, YAML surface.** Zod schema is the source of truth → published JSON Schema. Humans and git diffs see YAML; machines exchange JSON.
- **The expression language is tiny and total.** Side-effect-free comparisons/arithmetic/boolean logic over state paths — no loops, no recursion, guaranteed termination. (The Kotlin `ConditionEvaluator` design ports directly; Session 3's discipline pays off here.)
- **Composability via sibling kinds.** Candidate kinds `PieceSet`, `MechanicPack`, `Theme` are standalone registry artifacts (`@creator/fraction-cards`) imported by reference and remixable across games. The exact kind set is locked by an E0 ADR — count minimal, names permanent. Related docs co-locate in one multi-doc YAML file (one feature = one file).
- **Versioned via the API group.** `v1alpha1 → v1beta1 → v1` maturity ladder on the `apiVersion` group, not a bare integer; locked-grammar discipline with closed enums and documented-but-unimplemented DRAFT keys (mantle's grammar-revise process); the registry stores which engine versions certified a game.
- **No escape to code.** Genuinely new effect types are added centrally to the kernel as vetted, versioned primitives — never by game authors.

---

## 6. The verification loop (the hero feature)

Four gates, each exposed as library API + CLI + MCP tool:

1. **`validate`** — schema conformance + semantic lints (unreachable zones, actions that can never fire, type errors in expressions). Errors carry: path, expected, candidate fix.
2. **`simulate`** — N seeded headless playouts with random + heuristic bots. Reports: termination guarantee, game-length distribution, win-rate by seat (first-player advantage!), action availability per turn, dead pieces, branching factor. Deterministic engine ⇒ thousands of playouts per second, replayable by seed.
3. **`critic`** — educational alignment: do winning lines actually exercise the tagged learning objectives, or can players win while ignoring the math? Age-appropriateness lint (reading level, content rules).
4. **`playtest`** — ephemeral room where LLM agents play via the *same action API humans use*, returning qualitative notes ("turn 3 felt like there was nothing to do").

All four gates emit one diagnostic shape — mantle ADR-0008, adopted verbatim: `{code, phase: validate|test|boot|runtime, severity, path, value?, expected?, candidates?, suggestion?, message}` with UPPER_SNAKE codes, a single message formatter, CLI `--format=json|text`, and `candidates` stripped from runtime wire responses.

This is the loop Roblox validated at platform scale in April 2026 (plan → build → simulate → read results → self-fix; 44% of top creators use agentic tools). Our structural advantage: a deterministic, headless, data-driven kernel makes the loop *cheap* — no pixels, no physics, no flaky E2E.

Registry consequence: simulation results become **public QA badges** on every published game (`✓ terminates · ✓ 48–52% seat balance · ✓ keyboard-playable · ✓ objectives exercised`). Trust, rendered visible. That's the brand.

---

## 7. Catenin: the kernel (TypeScript)

**The hard call: TypeScript end-to-end, retiring Kotlin Multiplatform.**

The forcing function is §4's habitat list: browser, workerd, Node, agent sandboxes. That's four JavaScript runtimes on the critical path and zero JVM. Durable Objects run workerd — neither Node nor JVM; KMP-JS *can* compile to JS but pays a permanent tax (experimental d.ts generation, sealed-class export limits, bundle overhead, Gradle+JVM toolchain for every contributor — human or agent). The agent-native requirement settles it: **the codebase itself must be maximally agent-legible**, because agents will write most of it. TS strict + Zod + Vitest is the most agent-fluent stack that exists in 2026, and the OSS community this project wants to attract lives there.

What this does *not* mean: the founder's Kotlin years are not discarded. The kernel's architecture — immutable world, `PropertyValue` typing, ECA triggers, the expression evaluator, the event protocol — **is** the Kotlin design, ported. Strict TypeScript reads like Kotlin with different punctuation; the founder's review instincts transfer intact. And the prototype proved the design before we bet the platform on it. That was its job.

Kernel contract:

- **Pure reducer**: `(state, action, rng) → { state', events[] }`. No IO, no clocks, no globals. Dependency-free ESM package.
- **Determinism**: seeded PRNG; canonical serialization; golden replay tests (same seed + actions ⇒ byte-identical event log).
- **Event stream**: the 2026-03 Hearthstone-protocol work survives as the wire format — ordered semantic events, nested causality blocks, non-mutating `AnimationHint`s, sequence numbers, resume-from-sequence.
- **Hidden information**: zones have visibility; a per-seat projection function filters state and events (SHOW/HIDE semantics). The same projection code runs in solo mode, so local and online behavior never diverge.
- **The event log is the source of truth**: replays, spectating, debugging, and learning analytics are all reads of the same log.

Toolchain (family-aligned): pnpm@9 workspaces + Node ≥22 (24 LTS canonical; Node 26 LTS lands Oct 2026); tsc -b project references; Vitest; Zod v4; Bun optional as dev tooling. CI gate is mantle's: `pnpm run check` = check-boundaries → build → frozen-lockfile install → typecheck → test.

```
junction/
├── packages/
│   ├── spec/           # @junction/spec — GameSpec grammar, Zod schema, validator,
│   │                   #   diagnostics kernel (zero deps, sideEffects: false)
│   ├── runtime/        # @junction/runtime — reducer, expressions, seeded rng,
│   │                   #   simulate, per-seat projection; ports only, no adapter types
│   ├── renderer/       # @junction/renderer — Cadherin: component registry + templates
│   ├── cloudflare/     # @junction/cloudflare — Connexon adapter: DO rooms,
│   │                   #   Hono mounts, Better Auth
│   ├── node/           # @junction/node — portability-hedge adapter (plain ws +
│   │                   #   SQLite), compiled in CI
│   ├── mcp/            # @junction/mcp — Integrin: tool catalog over runtime use cases
│   ├── cli/            # junction CLI: validate / simulate / play / render
│   └── junction/       # umbrella re-exports (@junction/* under subpaths)
├── apps/
│   ├── synapse/        # the studio — Electrobun desktop app (E4)
│   └── plexus/         # community registry web app (E5)
├── games/              # reference GameSpecs + golden replays
├── skills/game-designer/SKILL.md
├── scripts/check-boundaries.mjs   # the layering law, enforced in CI
├── AGENTS.md           # points to CLAUDE.md (family pattern)
└── CLAUDE.md
```

Package names carry the *layer* (family convention — cf. `mantle-spec`/`mantle-runtime`); the anatomy names the *concepts and deployables*: Catenin ≈ spec + runtime, Cadherin = renderer, Connexon = cloudflare/node adapters + protocol, Integrin = mcp, Synapse/Plexus = apps. Biology is the brand; layering is the law.

---

## 8. Cadherin: the renderer (DOM-first, accessible)

Second reversal from the March plan: **DOM + CSS + Motion first; canvas later.** Three reasons:

1. **Accessibility is a procurement requirement, not a feature.** EN 301 549 / WCAG 2.1 AA is what school buyers ask for (EAA in force since June 2025; we're microenterprise-exempt from the law but not from procurement checklists). Cards and boards are DOM-shaped: keyboard-playable (arrow-key card selection, visible focus), ARIA live regions narrating the event stream (our ordered events map onto screen-reader announcements *perfectly*), 4.5:1 contrast, color-independent state, reduced-motion support, adjustable turn timers. Canvas makes all of this hard; DOM makes it nearly free.
2. **Agents are dramatically better at React DOM** than at imperative canvas scene graphs — and agents write the components.
3. **Card/board games don't need 60fps particles to be good.** Motion's FLIP layout animations cover card movement beautifully. PixiJS enters later as a *juice layer* behind the same component registry (the June 2026 stack research — Pixi 8.19, renderer fallback arrays, @pixi/react caveats — remains the reference for that day).

Architecture: a **component registry per template** (`card_game` v1: Hand, Deck, PlayArea, DiscardPile, ScoreBoard, TurnBanner…), driven by the event stream through an animation queue (API shape per `delucis/bgio-effects`: declared effects, ordered queue, per-effect durations — don't make the client diff states). View Transitions for screen-level changes only.

Bonuses that fall out of DOM: **print-and-play export** (print stylesheet — educators love paper), trivial theming via CSS tokens, SSR-able game pages for the registry.

**MCP Apps packaging**: the same renderer wraps in a sandboxed iframe (`ui://` resource, postMessage JSON-RPC) → **games are playable inside Claude and ChatGPT conversations** (extension ratified 2026-01-26; ChatGPT app directory open; games explicitly a category). Distribution channel and demo loop in one.

---

## 9. Connexon: the online runtime

**Cloudflare Durable Objects + partyserver, WebSocket Hibernation, SQLite-backed.** The economics are decisive for a solo founder: at realistic classroom scale (≈200 sessions/day, 40 concurrent rooms peak, ~1 msg/sec) the entire runtime fits the **$5/month Workers Paid plan** — hibernation stops duration billing between turns, nights/weekends/summers cost ~nothing, and the static solo-play client hosts free on the same deploy. partyserver is now load-bearing for Cloudflare's own Agents SDK (`DurableObject > Server > Agent`), the strongest maintenance signal available. (Rivet pivoted to AI infra; Fly costs more for less; a VPS has no scale-to-zero.)

- **Join flow (the Kahoot-proven pattern)**: teacher starts a room → short join code (`idFromName(code)`) → kids enter code at the join URL on Chromebooks → auto-assigned nicknames from curated word lists → play. **No student accounts, no student emails, no free-text chat at launch.**
- **Protocol**: ordered JSON events with sequence numbers, resume-from-sequence on reconnect (cheap-Chromebook Wi-Fi is the design case), per-seat projections server-side, WSS on port 443 only.
- **School network playbook** (incumbent-validated): one first-party domain for app + WSS (never random CDN hosts); published one-page IT allowlist doc; `/check` self-test page (firewall, WS, SSL); SSE-down + POST-up fallback for networks whose SSL inspection breaks WebSocket upgrades (MCP's own Streamable HTTP migration validated the pattern).
- **Portability hedge**: the platform-specific surface is a ~300-line room adapter. A second adapter (plain Node `ws` + SQLite) compiles in CI; exit path is a €4/month Hetzner box. The protocol, not the platform, is the contract.

---

## 10. Integrin: the agent interface

The MCP server is the front door. Tool design blends Anthropic's consolidation guidance with agent-native parity:

**Workflow tools** (the common paths, context-efficient):
- `scaffold_game(genre_template, ages, seats, learning_goal)` → valid skeleton GameSpec
- `validate_game(spec)` → structured errors with paths + candidate fixes
- `simulate_game(spec, n, seed?)` → balance/termination report (MCP **Tasks** for long runs)
- `playtest_game(spec, persona?)` → qualitative agent playtest notes
- `render_preview(spec)` → MCP Apps `ui://` playable preview, right in the conversation
- `publish_game(spec, license)` / `fork_game(ref)` / `search_registry(query, filters)`

**Atomic tools** (parity + emergence): `add_piece`, `add_zone`, `add_trigger`, `set_property`, `get_spec(path)`, … with a `response_format` verbosity parameter; human-readable names, never opaque UUIDs.

**Topology + auth (inherited from mantle ADR-0014):** public `/mcp` carries play/search/read tools; `/mcp/staff` carries authoring/publish tools behind Better Auth (OAuth 2.1 + PKCE, DCR), with roles checked fresh per-request, never cached in tokens. Tool responses are bounded, agent-safe receipts (nacre's contract): no absolute paths, no raw dumps; inspect-then-paginated-read for anything large.

**The authoring contract** (encoded in the skill): reason free-form about design first → scaffold → build incrementally with continuous validation → `simulate` gate → `playtest` gate → publish. Never emit an 800-line spec one-shot.

**Onboarding trinity shipped together** (Figma March 2026 template): Integrin + `skills/game-designer/SKILL.md` (schema conventions, age heuristics, balance workflow, worked examples; submitted to skills directories) + AGENTS.md in every template repo.

**Economics — the Websim lesson** (it cut free credits Jan 2026; *subsidized-free* generation doesn't survive): the play path hosts **zero LLM inference**, ever. Creation is tiered the family way: **free** = educators bring their own agent (Claude, ChatGPT, Copilot) via MCP, or a BYO API key in the studio; **Pro** = the studio's built-in agent, metered and billed through clam-platform's existing LiteLLM proxy + licensing infrastructure; **District (later)** = the atoll playbook for education (governance, rosters, NDPA). Marginal cost of play stays ~zero (static + $5 DO); generation is either the user's tokens or priced honestly — never given away. The platform sells certainty, a home, and distribution.

**Agent evals as CI**: the standing task suite runs weekly against Integrin with real agents; success rate and tokens-to-valid-game are the platform's north-star quality metrics; tool descriptions are tuned against regressions.

---

## 11. Plexus + Occludin: community and trust

**Registry, not gallery** (the shadcn move, proven at scale):

- Every game, piece set, and mechanic pack is addressable — `@ms-chen/fraction-duel` — and installable/forkable by agents and humans in one call.
- **QA badges from the verification loop displayed on every listing** (termination, balance, a11y, objectives-exercised). Curation by *proof*, not vibes — this is the answer to AI-slop fatigue (52% of game devs now view gen-AI negatively; trust is the scarce good).
- **Lineage graph**: forked-from chains rendered visibly; remix culture with mandatory attribution. Default license CC BY-style; provenance block records human + model authorship (C2PA-style), aligned with CC's 2026 education pilots.
- **Teacher collections**: classroom-ready shelves filtered by age, subject, class-period length.

**Occludin** (tight junction — controls what passes) gates publishing, not playing:
- Publish-time automated review: schema validation, content lint (word lists, reading level), LLM age-appropriateness screen (one-time cost per publish, not per play).
- Report loop + human review; teacher-curated featured shelves.
- **Compliance checklist v1** (now mandatory under the amended COPPA rule, in force since 2026-04-22): students contribute only nickname + gameplay events under the support-for-internal-operations exception; zero third-party ad/analytics SDKs on student pages; COPPA-compliant privacy notice naming the specific internal operations; written security program + written retention policy (ephemeral room state auto-deleted, documented); teacher accounts are the only accounts; school-authorization terms; SDPC NDPA v2 signed the moment we persist teacher-linked student results (legally required in CA/NY/IL/CO/CT).

---

## 12. Decision register (the hard calls, and what would reverse them)

| # | Decision | Why | Reversal trigger |
|---|----------|-----|------------------|
| 1 | **TypeScript end-to-end**, retire KMP | 4 JS runtimes on the critical path, 0 JVM; agent fluency; OSS gravity; workerd constraint is absolute | None foreseen; a JVM-only school-integration need (e.g., deep LTI) would add an edge service, not change the kernel |
| 2 | **Genre-layered kernel** — universal core (entities, expressions, triggers, events) + genre modules (tabletop → adventure → RPG); tabletop ships first | Semantics buy auto-rendering, deep lint, analytics; the core carries cross-genre universality at the right altitude (Addendum 2 §3) | A genre that won't fit the core's turn/event model → standalone sibling engine |
| 3 | **DOM-first renderer**, canvas later | A11y is procurement-mandatory; agents write DOM best; cards are DOM-shaped | A template that demonstrably needs sprite-scale perf → Pixi juice layer behind same registry |
| 4 | **Bespoke GameSpec + bespoke event protocol**; A2UI-aligned shapes at layout layer only | No standard covers game event choreography; A2UI is pre-1.0, single-vendor | A2UI v1.0 + Kotlin/TS SDKs land (Q4 2026) → thin adapter, revisit |
| 5 | **Cloudflare DO + partyserver** | $0–5/mo, hibernation fits turn-based, scale-to-zero, partyserver load-bearing for CF's own Agents SDK | Pricing/policy shift → Node `ws` adapter in CI, Hetzner exit (~€4/mo) |
| 6 | **Tiered agent economics** — free = BYO key/agent via MCP; Pro studio = built-in metered agent via clam-platform's LiteLLM proxy; play path always LLM-free | Websim's lesson holds (never *subsidized-free* generation) — and the family already owns billing + proxy infrastructure | Proxy economics fail → Pro falls back to BYO-key-only |
| 7 | **pnpm + Node 24 LTS + Vitest (mantle-aligned); Bun optional dev tooling**; kernel runtime-agnostic ESM | Family house style; rooms run workerd regardless | Bun LTS + family-wide migration → could flip cheaply |
| 8 | **Engine Apache-2.0; GameSpecs CC BY; FSL-1.1-ALv2 held in reserve** | mantle's posture fits education/community goodwill; content stays remixable | A commercial materialize-equivalent layer emerges → clam's FSL playbook for that layer only |
| 9 | **Creation surface = Synapse desktop studio (Electrobun, nacre pattern); play surface = web** | Teachers get a home — local-first projects, embedded agent, live preview, projector mode; kids need zero-install browser play | Studio adoption stalls while web-lite editor demand grows → promote a web creation surface |
| 10 | **Closed-grammar contract over YAML fundamentalism** — structure in YAML kinds; dialogue may get an Ink-style text grammar (adventure module ADR); never open-ended code | Verification, safety, and remixing require closed, total, analyzable grammars — the syntax per content type can vary | — |
| 11 | **One studio app — shell + genre workspaces; branded per-genre distributions remain a packaging option** | One narrative (creation loop + Places·Pieces·Rules·Goals IA + schema-derived inspectors); hybrids need one roof; solo founder = one release train (JetBrains/Office/Affinity precedents) | GTM shows distinct genre personas with distinct channels → split distributions (packaging cost only, by construction) |

---

## 13. Roadmap — epochs with exit criteria

Sized for one founder directing an agent team. Epochs, not calendar promises.

**E0 — Kernel** (~2–3 wks)
GameSpec v1alpha (card subset, manifest envelope), Zod schema, reducer + expressions + seeded RNG, `validate` + `simulate` CLI, 3 reference games (War, a matching/memory game, a quiz-battle), golden replay tests. Diagnostics kernel, manifest-parser shape, and CLI `--format=json|text` lift directly from mantle-spec patterns; the kind-set ADR (which sibling kinds exist) is written here.
**Exit:** `junction simulate games/war.yaml` prints a balance report in under a second.

**E1 — Solo playground** (~2–3 wks)
`card_game` DOM template (keyboard-playable, ARIA live narration), animation queue, Synapse v0 (editor pane + hot-reload preview), static deploy, shareable play links.
**Exit:** a stranger plays a game from a link; a screen-reader user completes a turn.

**E2 — Agent loop** (~2–3 wks)
Integrin MCP server (workflow + atomic tools), `game-designer` skill, AGENTS.md, agent-eval suite v0, MCP Apps packaging spike. This is the parity contract the studio's embedded agent will consume in E4.
**Exit:** a fresh Claude conversation builds a new, valid, balanced, playable game end-to-end through MCP — unassisted — and you play it in the same conversation.

**E3 — Rooms** (~3–4 wks)
Connexon on DO (hibernation, SQLite), join codes + auto nicknames, per-seat projections, sequence/resume, `/check` page, IT allowlist doc, compliance notice + retention automation.
**Exit:** a real classroom of 25 plays a full game on school Chromebooks.

**E4 — Synapse studio alpha** (~4–6 wks)
Electrobun desktop app, the nacre pattern applied to games: the genre-agnostic **shell** (Places·Pieces·Rules·Goals IA, schema-derived inspectors) + the **tabletop workspace** (board/zone layout canvas), embedded agent (free = BYO key; Pro = metered via clam-platform's LiteLLM proxy) consuming Integrin's tools, bundled `junction` CLI runtime (runtime-tools.lock pattern), local-first project files, live renderer preview, simulate/playtest panels surfacing QA badges, purpose-typed asset library (mantle media model), projector/hot-seat classroom mode, print-and-play export.
**Exit:** a teacher with no Claude account builds a game and plays it with their class, entirely from the studio.

**E5 — Community alpha** (~4–6 wks)
Plexus registry (addressable artifacts, fork lineage, QA badges), publish-from-studio, teacher collections, Occludin publish gates, registry search via MCP. **The grammar locks at `v1` here** — the registry locks the grammar; the studio floats it.
**Exit:** 50 published games; 10 remixes by people/agents we don't know.

**E6 — Education depth + adventure module**
Event-log learning analytics + teacher dashboard, xAPI/LTI export, NDPA execution, `board_game` template (grid/hex), the adventure genre module + **adventure workspace in the same shell** (scene/hotspot canvas; dialogue-grammar ADR — likely Ink-style; reachability lints: orphan scenes, stuck states, dialogue dead ends), Pixi juice layer where templates earn it.
**Exit:** a teacher cites the dashboard in a parent conference. (That's the mission, measured.)

**E7 — RPG module**
Maps/actors/stats/encounters/quests kinds, the **rpg workspace in the same shell** (tile-map painter — the largest per-genre canvas), tilemap rendering (the canvas layer's moment), RPG simulation heuristics (progression balance, encounter difficulty curves, economy). Genre-composition ADR (subgame embedding — e.g., an rpg encounter resolved as a tabletop battle) lands here or E8.
**Exit:** a teacher ships a curriculum-aligned mini-RPG; the registry's first RPG remix chain appears.

---

## 14. Risks

- **The rewrite discards working Kotlin.** Mitigation: we port the *design*, which was the hard part; the prototype is small (~3k LOC) and stays archived as reference. Sessions 1–4 were the tuition, not the product.
- **Founder works outside Kotlin.** Mitigation: agents author, founder architects and reviews; strict TS + Zod reads familiarly; the JetBrains–Anthropic tooling wave helps both worlds.
- **Spec expressiveness ceiling.** Mitigation: versioned spec; new effect primitives added centrally and vetted; never user code. If a game can't be expressed, that's a kernel PR, not an escape hatch.
- **Agent slop floods the registry.** Mitigation: Occludin gates + QA badges + human curation; the brand *is* the quality bar. Publishing has friction by design; playing has none.
- **Platform dependence (Cloudflare).** Mitigation: protocol-first design, second adapter in CI, documented exit.
- **Compliance drift.** Mitigation: the checklist lives as CI lint rules on student-facing surfaces (no third-party scripts, notice present, retention jobs tested).
- **Standards churn (MCP/skills/A2UI).** Mitigation: we bet only on Linux-Foundation-governed layers (MCP, Apps, AGENTS.md, skills) and keep A2UI at arm's length until v1.0.

## 15. What survives from 2025–2026

The ECA trigger paradigm and the total expression language (Session 3's work — ports directly). The Hearthstone event protocol design — `GameEvent`/`EffectBlock`/`AnimationHint`, causality blocks, per-seat visibility (the 2026-03 session's work — becomes the wire format). The research corpus (2026-03 and 2026-06 docs — the renderer-stack guidance carries into Cadherin's future juice layer). TDD culture and golden tests. The journal, and the habit of writing it. The biological naming, now grown into a full anatomy. The mission, unchanged since Session 1.

The Kotlin implementation is archived with honor — tagged, referenced from the new AGENTS.md as "the prototype that taught us the shape."

---

## 精神筆記

> 2025 年我們問:「AI 寫得出遊戲定義嗎?」
> 2026 年 agent 什麼都寫得出來 —— 稀缺的不再是生成,而是**驗證**與**累積**。
>
> 所以新的核心不是 DSL,是那個能把一個遊戲玩一萬遍然後告訴你「這遊戲公不公平、會不會結束、學生有沒有真的練到分數加法」的引擎。
> Catenin 不再只是大腦 —— 它是良心。
>
> 教育者帶著教學目標來,agent 負責打造,引擎負責把關,社群讓每個遊戲成為下一個遊戲的養分。
> 人類指揮、判斷、策展、教學;agent 製造、測試、修復、混搭;孩子們玩。
>
> 這就是 Junction:細胞之間所有的連結蛋白,終於長成一個完整的組織。

---

# Addendum (same day): Junction joins the family

*Added 2026-06-11, after the founder revealed `mantle` and the `clam` family (`~/projects/aotter-clam-mantle-workroot`, `~/projects/aotter-clam-work-root`) and ratified the TypeScript + Cloudflare calls: "i think cloudflare is awesome, i can embrace full ts for cloudflare worker."*

*(v1.1: the main body above has been reconciled to these decisions — §5 envelope, §6 diagnostic shape, §7 layout/toolchain, §10 topology, register #7–#8, E0. This addendum stands as the record of what changed and why.)*

## What changed

The blueprint above was written as if its thesis were new. It is not — **the founder has already built it twice, in production**:

- **mantle** — MCP-native headless CMS for Cloudflare Workers. *"Agents write config; the runtime carries complexity."* Locked-grammar YAML manifests (4 atoms: Schema/View/Procedure/Trigger), structured JSON diagnostics, skills as product surface, starters + tarball scaffolder, Better Auth + scoped MCP endpoints, Apache-2.0.
- **clam family** — agent-native data-ops harness (Excel/CSV → YAML contracts → deterministic DuckLake warehouses). Content-addressed keys, fingerprint revalidation, structured diagnostics for agent retry loops, FSL-1.1-ALv2 kernel with commercial tiers (nacre desktop / atoll enterprise / clam-platform control plane).

Junction is therefore not a standalone bet. It is the **third domain of an existing family thesis**: mantle does content apps, clam does data modeling, **Junction does play**. Decision register #1 (TypeScript) and #5 (Cloudflare) are no longer "hard calls" — they are the founder's own lived stack, ratified.

## What Junction inherits (adopt, don't reinvent)

| Inherited | From | Note |
|-----------|------|------|
| **Diagnostic shape** — `{code, phase, severity, path, value, expected, candidates, suggestion, message}`; UPPER_SNAKE codes; phases validate/test/boot/runtime; one formatter; `candidates` stripped at runtime; `--format=json\|text` | mantle ADR-0008 | Blueprint §6's "machine-actionable errors," already designed and battle-tested. Adopt verbatim. |
| **Manifest envelope** — `apiVersion: games.junction.aotter.net/v1`, `kind`, `metadata.name`, `spec`; multi-doc YAML co-location (one feature = one file) | mantle ADR-0001 | GameSpec's outer shell. Composable kinds (e.g. `Game`, `PieceSet`, `MechanicPack`) defined by ADR in E0; count minimal, names locked. |
| **Locked grammar discipline** — closed enums, DRAFT keys documented-not-implemented, grammar-revise process | mantle | Blueprint §5's "no escape to code," operationalized. |
| **Package layering** — spec (zero deps, `sideEffects: false`) ← runtime (ports only, no adapter types) ← adapters (cloudflare) ← umbrella; `check-boundaries` script in CI | mantle | Maps onto the organ names: spec+kernel=Catenin, adapter=Connexon, etc. Layer rules are the law; biology is the brand. |
| **Clean-arch layout + naming** — `kernel ← domain (model/port/service) ← usecase ← infrastructure`; `*Repository/*Driver/*Cache/*UseCase`; composition root is the only wiring site | mantle + nacre | |
| **Result objects** — `{ok: true, data} \| {ok: false, diagnostic}`; tests assert `diagnostic.code` | mantle | |
| **Auth + MCP topology** — Better Auth (OAuth 2.1 + DCR), public `/mcp` (play, search) vs `/mcp/staff` (authoring), role checked per-request | mantle ADR-0014 | Solves Integrin's auth design outright. |
| **Hono on Workers** + D1/KV/R2 mount patterns | mantle-cloudflare | Blueprint didn't name a web framework; the family has. |
| **Skills format + interview discipline** — front-matter, ground-truth-first, explicit user authorization before destructive actions | mantle skills/ | `game-designer`, `install`, `extend`, `provision` skills follow the house format. |
| **Starters distribution** — separate starters repo, tarball scaffolder, archetype/feature/theme merge, `{{MACRO}}` substitution, templated consumer `AGENTS.md` | mantle-starters | For self-hosted instances and custom renderer/template-pack dev projects. Educator-created *games* are registry **data**, not scaffolded repos — renderer templates live in `@junction/renderer`. |
| **Agent-safe tool receipts** — bounded outputs, no absolute paths/raw stdout, two-step inspect→paginated-read | nacre | Integrin tool responses follow this contract. |
| **Static `.md` mirrors + llms.txt** per published artifact | mantle | Every Plexus game page gets an agent-crawlable mirror. |
| **Determinism practices** — content-addressed/fingerprint identity, replay-safe artifacts | clam | Kernel seeds, golden replays, and registry certification reuse the mindset (and possibly `clam-runtime/fingerprint`). |
| **Working practices** — ADR discipline, develop→main + merge-not-squash, changelog at release, conventional commits with co-author trailers, `pnpm run check` gate, zh-TW internal / EN external docs, agent-team + spec-driven (openspec) workflows | whole family | |

**Tooling alignment**: pnpm@9 + Node ≥22 + Vitest + tsc -b (mantle's combo), amending decision #7 (Bun optional dev tooling; clam side uses it, mantle side doesn't). **Licensing**: engine Apache-2.0 (mantle's posture — right for education/community goodwill); GameSpecs CC BY (per §11); FSL-1.1-ALv2 held in reserve for a future commercial layer (clam's playbook) if one emerges.

## What remains genuinely novel (where the risk budget goes)

1. **The deterministic game kernel** — pure reducer, seeded simulation, balance/termination reports. Clam's determinism obsession applied to *play*; no family member has it.
2. **Realtime rooms** — Durable Objects + WS hibernation + per-seat hidden-information projection. Junction is the family's first realtime member.
3. **The kid-facing renderer** — accessible DOM game templates, animation queue, COPPA-posture join flow. mantle's surfaces are admin/content; Junction's are children in classrooms.
4. **MCP Apps playable previews** — games running inside Claude/ChatGPT conversations.

## Effect on the roadmap

E0–E2 compress: diagnostics, manifest parsing, CLI shape, skills, starters, MCP mounting, and auth all start from proven family patterns instead of blank pages. The novel-risk budget concentrates exactly where it should — kernel, simulator, renderer, rooms. The blueprint's epochs stand, but their unknowns just got smaller.

> 母蚌分泌珍珠質,築成貝殼 —— mantle 與 nacre 教會我們怎麼蓋平台。
> 現在輪到 Junction 用同一套身法,蓋一座孩子們玩的城。

---

# Addendum 2 (same day): The studio pivot — engine identity, layered kernel, desktop studio

*Added 2026-06-11, after the founder asked: "does this yml grammar still matter? or we can think otherwise… i want to give the teachers an agentic desktop app to build games… see if we make it like a specialized unity or godot that only makes 2d card/board/adventure/rpg games."*

## 1. The grammar survives — demoted to exactly where it belongs

The question forced a re-derivation, and the grammar holds — but its **role changes**:

- **Before:** GameSpec was framed as the thing educators (via agents) author. The grammar was close to being the product.
- **Now:** GameSpec is the **project file format and the agent⇄runtime contract** — Junction's `.tscn`, RPG Maker's database, Unity's scene+prefab serialization. Teachers never see it. The studio's chat pane is the authoring surface; the agent compiles conversation into validated artifacts; the runtime executes and judges them.

> **The scripting language is conversation. The GameSpec is the bytecode.**

What was never negotiable is the **closed-grammar contract**: closed vocabulary, total (always-terminating) expressions, analyzable structure. That — not YAML syntax — is what makes the verification loop, safe community remixing, kid-safe moderation, and deterministic replays *possible*. An "agent writes code" substrate (the Websim/Rosebud route) forfeits all four. A conversation-driven editor without a structured artifact stores the game's truth in chat history — undiffable, unrecoverable, unremixable. The grammar matters **more** in the studio world, not less.

One honest refinement: **closed grammar ≠ YAML fundamentalism.** Structure lives in YAML kinds; content types with their own shape may get their own closed grammars — dialogue trees in particular read terribly in YAML and may deserve an Ink-style text grammar (compiled to the same trigger/event model, equally total and analyzable). Decided per module ADR.

**Grammar stability nuance:** while the studio is the only consumer, the grammar floats (`v1alpha`, breaking changes allowed). It locks at `v1` when the registry opens — **the registry locks the grammar; the studio floats it.** This de-risks "locked too early" across four genres.

## 2. The engine identity, embraced

Junction is **a specialized, agent-native game engine + studio for 2D educational card/board/adventure/RPG games**. The Unity/Godot analogy is now the product narrative, with one precedent that proves genre-locking works commercially and culturally: **RPG Maker** — genre-locked, database-driven, beloved for 30 years. Bitsy and Twine prove the same at the small end. Junction's differences from all of them:

1. The primary author is an agent in conversation with a teacher (not a scripter).
2. Verification is built in (simulate/balance/critic/playtest → QA badges).
3. Games are data, so publishing is COPPA-safe and remixing is one click.
4. The classroom is a first-class deploy target (join codes, projector mode, print-and-play).

## 3. The genre-layered kernel (decision #2 amended)

Expanding to adventure/RPG partially **vindicates Session 3's universality** — at the right altitude. The blueprint's "specialized beats universal" argument was correct about *semantics* but drew the line one layer too high. The synthesis:

```
┌─ Genre templates (renderer)  card_game · board_game · adventure · rpg ─┐
├─ Genre modules (kernel)      tabletop: zones/pieces/turns              │
│                              adventure: scenes/hotspots/dialogue/inventory
│                              rpg: maps/actors/stats/encounters/quests  │
├─ Universal core              entities · typed properties · expressions │
│                              · ECA triggers · event log · seeded RNG   │
│                              · per-seat visibility · reducer           │
└─────────────────────────────────────────────────────────────────────────┘
```

- The **core** is Session 3's discipline reborn: small, total, domain-agnostic.
- **Genre modules** contribute kinds, semantic lints, and simulation heuristics. RPG battles are largely the tabletop module wearing different clothes; adventure/RPG content (dialogue, maps, quests) is famously data-friendly — RPG Maker's event commands *are* a closed-vocabulary ECA system.
- **Ship order unchanged:** tabletop first (E0–E3). Adventure module ~E6, RPG ~E7. The core is architected for modules from day one; no genre is built speculatively.

## 4. Synapse becomes the studio (decision #9)

**Synapse = an Electrobun desktop app, the nacre pattern applied to games** — the family already proved every piece:

| Studio element | nacre precedent |
|---|---|
| Embedded agent with bounded, schema-validated tools | Mastra agents + workbench tools (agent-safe receipts) |
| Bundled engine runtime, version-pinned | `runtime-tools.lock.json` bundling `clam`/`clam-materialize` → bundles `junction` CLI |
| Local-first projects (git-friendly YAML files on disk) | nacre project folders + inspect contract |
| Propose → evidence → approve UX | nacre's core loop |
| shadcn + Tailwind v4 + frontend discipline doc | nacre `docs/frontend-development.md` |

Studio pillars: project browser · agent chat pane · **live preview that is the real renderer** · simulate/playtest panels surfacing QA badges before publish · asset manager (mantle's media model: R2 presigned uploads, variants, publish-time moderation; stock packs for teachers) · publish-to-registry · **projector/hot-seat mode** (one classroom screen, pass-the-mouse — many classrooms have one device) · print-and-play export.

**The web does not lose its job — it loses only the authoring job.** Kids play in the browser (zero-install, join codes, $5 rooms); the registry stays web; MCP Apps previews stay. A web-lite editor can return later if demand says so.

**MCP parity is untouched:** the studio's embedded agent consumes the *same Integrin tools* exposed at `/mcp/staff` — the studio is simply the first-party MCP client. Commitment #1 (parity) is how we guarantee the studio never grows private capabilities.

## 5. Economics, tiered the family way (decision #6 amended)

The Websim lesson was "never **subsidized-free** generation" — not "never hosted":

- **Free** — BYO: teacher's own Claude/ChatGPT via MCP, or BYO API key in the studio. Platform's generation cost: zero.
- **Pro** — the studio's built-in agent, metered and billed through **clam-platform's existing LiteLLM proxy + licensing/billing infrastructure** (already in production for clam Pro). Junction doesn't build a control plane; it joins one.
- **District (later)** — the atoll playbook for education: governance, rosters, NDPA/DPA execution, audit. SLG wedge when schools ask.
- **Play is always LLM-free.** The engine, not a model, runs the games.

## 6. What this addendum does NOT change

Deterministic reducer kernel and event protocol · the verification loop as hero feature · DOM-first accessible renderer (RPG tilemaps may justify the Pixi layer at E6–E7) · Cloudflare rooms + registry + $5 economics · COPPA join-code posture · registry-as-data community · the onboarding trinity · agent evals as CI.

**Body reconciliation (v1.2):** §1 thesis genre list, §2 commitment 1, §4 Synapse row + diagram, §7 apps tree, §10 economics, register #2/#6 amended + #9/#10 added, §13 roadmap resequenced (studio = E4, community = E5, education depth + adventure = E6, rpg = E7).

> RPG Maker 證明了「鎖定類型的引擎」可以活三十年。
> Unity 給開發者一個家;Synapse 給老師一個家 —— 而住在裡面的,是一位 agent。

---

# Addendum 3 (same day): One studio, three worlds — the shell + workspace architecture

*Added 2026-06-11, after the founder said: "tabletop, adventure and rpg, i'd ship three studio apps if necessary, given that props, assets management and core logic is different. but it would be cool if these three can fit in a single uiux narrative."*

## The decision

**One studio app: a genre-agnostic shell + three genre workspaces.** Shipping three branded distributions later (Junction Tabletop / Junction Quest / Junction RPG) remains open as a **packaging decision** — the JetBrains model (one IntelliJ platform, many IDEs), the Office model (three apps, one ribbon narrative), the Affinity model (three apps, one file format, persona switching). Because workspaces are plugins over the shell, that split costs packaging work only, never rearchitecting (decision #11).

Two arguments make one roof strictly better for v1:

1. **Hybrids are the soul of educational games.** Oregon Trail = adventure + resource systems. Carmen Sandiego = adventure + quiz. Math Blaster = arcade + drills. The genre-composition future (an RPG encounter resolved as a card battle — Slay the Spire for fractions) requires the modules to live in one studio over one core. Three sealed apps foreclose the most pedagogically valuable design space. A genre-composition ADR ("subgame embedding") lands ~E7–E8.
2. **Solo-founder economics.** One release train, one updater, one onboarding, one doc set — versus three of everything. nacre itself is one workbench over many source types; the family pattern agrees.

## Why one narrative is *possible*: the same reason one kernel is

The UI narrative mirrors the kernel layering. The layered kernel works because all three genres reduce to a universal core; the studio works because all three genres reduce to a universal **information architecture**:

> **Places · Pieces · Rules · Goals (· Assets)**

| | Places | Pieces | Rules | Goals |
|---|---|---|---|---|
| **Tabletop** | zones: deck, hand, board | cards, tokens, dice | turns, triggers, costs | win conditions, learning tags |
| **Adventure** | scenes | items, characters, hotspots | dialogue, flags, triggers | endings, learning tags |
| **RPG** | maps, rooms | actors, monsters, items | encounters, quests, triggers | quest goals, learning tags |

This maps 1:1 onto the kernel core (places = spatial containers with visibility + adjacency; pieces = entities; rules = triggers/turn structure; goals = end conditions + learning layer). The sidebar never changes; what a "place editor" opens as is the workspace's business. A teacher who learns the model once carries it across genres — the mental model *transfers*, which is itself pedagogy.

## The narrative spine (identical across genres)

1. **One creation loop:** Intent (chat) → Draft (agent scaffolds; preview is playable from minute one) → Shape (chat + workspace tweaks) → Prove (simulate/playtest; QA badges go green) → Play (join code / projector) → Share (publish, remix lineage). Six verbs, three genres, zero divergence.
2. **One inspector, derived from the grammar.** Click any card/hotspot/actor → the same schema-driven inspector: art, name, typed properties, behaviors (triggers), "ask the agent about this." The studio renders its editors from the kind grammars **the same way Cadherin renders games from the spec** — when an agent (or a future module) adds a kind, the studio grows its UI for free. The editor is itself registry-driven; the house thesis applies to the studio's own face.
3. **One asset library, typed by purpose.** "Props and assets management is different" — different in *taxonomy*, not in *system*. One library (mantle's media model: R2, presigned uploads, variants, publish-time moderation) where every asset carries a **purpose** (card-face, tileset, portrait, background, icon, sfx, music — mantle's `MediaPurposePolicy` pattern, verbatim). Workspaces filter by purpose; registry asset packs (`@creator/fantasy-pack`) serve any genre whose purposes match; AI art generation slots in per-purpose with one UX (describe → pick → place).

## What is honestly per-genre (the real cost, kept small)

In an **agentic** studio, conversation + live preview replace most of what Unity/RPG Maker need panels for. Direct manipulation survives only where **pointing beats prose** — nobody should type "move the door hotspot 20 pixels left." Per genre, that means roughly one spatial canvas plus module content:

| Genre | Spatial canvas | Module-specific lints & sim reports |
|---|---|---|
| Tabletop (E4) | board/zone layout canvas | win-rate balance, termination, dead cards |
| Adventure (E6) | scene + hotspot canvas | **reachability**: can the player get stuck? orphan scenes/items, dialogue dead ends |
| RPG (E7) | tile-map painter (the big one) | progression curves, encounter difficulty, economy balance |

Everything else — chat, preview host, project browser, QA panel, asset library, publish flow, classroom modes — is the shell, written once.

## 細胞學註腳

同一套基因組,三種分化的細胞 —— 差異不在 DNA,在表現。
Shell 是細胞膜;workspace 是胞器;genome 是 universal core。
這不是比喻硬湊:這正是為什麼「一個敘事」可行 —— 因為它和「一個核心」可行,是同一個原因。
