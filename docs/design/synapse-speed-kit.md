# The Synapse Speed Kit — what the studio pre-builds so agents assemble, not build

**Date**: 2026-06-11 · **Status**: Design note (extends blueprint §10/Addendum 2/customization ladder) · answers "in the desktop studio, the agent works on the GameSpec and the UI/juice — what pre-made elements do we provide so it builds fast (e.g. it just gens a sprite for a skin)?"

## The key move: push the data/code boundary until ~95% of per-game visual identity is DATA

The trust model (customization ladder) says games carry data, never code. The speed answer is to make that data *expressive enough* that the studio agent rarely wants code. Most "custom UI" is expressible as validated, remixable data:

| The agent "writes the UI" by producing… | Format | Example |
|---|---|---|
| Theme | token sheet (data) | jungle palette, chunky cards, bouncy motion, celebration: high |
| Card look | **SVG template with `{property}` slots** (data!) | agent designs the card frame once as declarative SVG; the engine instantiates it per piece |
| Motion feel | animation timeline presets + intensity params (data) | `capture: arc-trail, deal: stagger-fan` |
| Sound | **WebAudio synth patch presets** (data) | tap/whoosh/chime/fanfare as oscillator/envelope parameters — zero binary assets |
| Particles | preset + params (data) | `confetti{count, colors}`, `trail{length}` |
| Art | sprites/images into **purpose-typed slots** | card-art, background, portrait, tile, icon — generated via img-MCP |
| Genuinely novel widgets | platform PR (code, reviewed) | a new zone component enters the catalog for *all* games |

So yes — in the common case the agent literally "just gens sprites for a skin," and even that is optional: **procedural fallbacks mean the game is playable and decent-looking with zero assets; art streams in later as progressive enhancement.** Rules-first, beauty-second, never blocked.

## The pre-made catalog (what Cadherin/Synapse ship)

**A. Zone & board components** (semantic auto-layout already picks defaults; skins override):
stack · fan · arc · grid · row · pile · score-track · dice-tray · tilemap (E7) · scene-canvas with hotspot layer (E6)

**B. Card chrome:** face-frame layouts (corner-index / art-window / stat-badges for attack-health corners) · generative back patterns (today) · rarity/foil accents

**C. Overlay widgets:** turn banner · resource bar (mana/energy) · health rings · dialogue box (E6) · inventory drawer · **choice prompt** (pairs with Wave-2 choice requests) · targeting arrow (the Hearthstone arrow) · event ticker (today) · win ceremony

**D. Juice primitives:** spring/arc/stagger timelines · press feedback · idle pulse · number pops · confetti/starburst/trail particles · streak meter · WebAudio patch bank

**E. The asset pipeline** (mantle media model): purpose-typed slots; components auto-consume whatever the slot holds; **a per-game style token** (art-direction prompt prefix the studio remembers) keeps every generated sprite consistent with the game's look; Occludin moderation at publish.

> **Live rung-4 provider (2026-04-30): Higgsfield MCP** — hosted endpoint (`https://mcp.higgsfield.ai/mcp`), account OAuth (no API keys), 30+ image/video models, credit-billed, async with polling, generation history reusable as inputs. The decisive feature for game assets is **Soul character training**: train a character/style from references once, then every generation stays visually consistent — the industrial-strength version of our style token (16 memory tiles, 52 card faces, one protagonist across scenes, one art direction). Composition stays provider-agnostic: the studio agent simply calls Integrin and an asset MCP side by side; Junction never builds or proxies image gen. Their official server is also a design mirror worth studying: workflow tools over raw atoms (Anthropic's consolidation guidance in the wild), and a **virality-prediction scoring tool** — *their* version of our QA badges. The "platform that judges, not just generates" thesis, converging in an adjacent industry.

**F. Agent workflow tools:** the Integrin loop (scaffold/validate/simulate — built) + render/screenshot/judge feel-loop (proven with preview MCP) + future UI lints (contrast, readability, touch-target size) in the same verdict format as simulate.

## The studio session, end to end (teacher's view)

1. *"A fractions game, jungle theme, 2nd grade, 15 minutes."*
2. Agent: `scaffold_game` → customize → `validate` → `simulate` until badges are green. **Playable in minutes, procedural look.**
3. Agent: picks skins, writes theme tokens, designs the SVG card template, selects a sound patch. **Now it feels like a game.**
4. Agent: gens background + card art via img-MCP into purpose slots, guided by the game's style token. **Now it's jungle.**
5. Teacher watches the live preview throughout; "bigger cards, more celebration" → agent edits *data*, hot-reload, instantly visible.
6. Publish: GameSpec + presentation data + assets through Occludin's gates; QA badges ride along.

The teacher never sees YAML; the agent never blocks on art; the registry receives only data + moderated assets. Remixers inherit everything — including the look.
