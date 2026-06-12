# Competitive Watch

Dated entries on products that move in Junction's waters. The June 2026 research split the field into (a) quiz-template platforms with AI content-fill and (b) freeform AI code-gen with no guarantees, and named the risk: *"(b) getting good enough that educators tolerate unreliability."* This file tracks that risk.

## 2026-06: Higgsfield Games (the strongest form of route (b) yet)

**What shipped** (announced ~2026-06-10, riding the Claude Fable 5 launch): prompt + genre template → **Fable 5 writes full game code** (browser, includes 3D low-poly worlds) → Higgsfield generates all assets in-loop → **auto-hosted shareable URL, one click** → optional **multiplayer "wired for you" (lobbies + state sync)**. Available on their "Supercomputer" platform and via Higgsfield MCP inside any agent. Generated code is editable afterward. Page contains **zero** mention of: validation/QA, education, pricing, creator monetization.

**Honest assessment:**

- **The artifact is code; ours is data.** That single difference carries every classroom requirement: their games can't be simulated, can't carry fairness/termination proofs, can't be safely remixed, can't be moderated at registry scale, can't yield per-student learning analytics, and can't be audited by a teacher before 25 children touch them. None of that matters for entertainment creators — all of it matters for schools. Junction's lane is unchanged and still uncontested.
- **Their multiplayer is a feature checkbox; ours is a correctness property.** Generic state sync over generated code cannot guarantee hidden-information integrity (per-seat projection, opaque handles, server-sent options). For open-information party games theirs is fine; for anything with a hidden hand, it leaks by construction.
- **Where they beat us today: distribution.** "Prompt → playable URL in minutes, hosting handled" is now the publicly-set UX bar. Junction has every piece (render → single file; serve → live room; render_game → ui:// page) but **no hosted one-click publish**. This is the time-sensitive gap — not generation quality, not verification.
- **The credible threat path:** Higgsfield already ships a *virality-prediction* scoring tool for video — they understand judgment tools. If they point that muscle at games (fun-score, difficulty-score) and then at education, the "certified games" window narrows. Watch for: any Higgsfield QA/education move; generated-code platforms adding determinism or replay.
- **The tide lifts us too:** the same Fable-class capability powering their demo makes Synapse's embedded agent stronger — and a constrained grammar + structured diagnostics narrows the failure space far below raw codegen.

**Resequencing recommendation (proposed):** pull a **Plexus-lite hosted publish** forward — `junction publish` → hosted URL with QA badges visible (DO/Workers static + rooms), before or alongside E4 Synapse. The differentiator must be *equally frictionless to experience*: "one prompt → a **certified** game your class joins by code." The badge is the moat; the link is the market.

**Positioning sentence (use everywhere):** *Anyone can prompt a game now. Junction is where you prompt a game you can trust in a classroom — proven fair, guaranteed to end, safe for kids, and remixable by the next teacher.*
