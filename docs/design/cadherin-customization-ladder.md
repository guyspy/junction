# Cadherin Customization Ladder

**Date**: 2026-06-11 · **Status**: Design note (extends blueprint §8) · answers "how customizable is the UI world, and how do we reach real mobile-game feel?"

## The principle

**Rules are sealed; presentation is a ladder.** Agent-generated UI exists at two trust levels, and the split is the design:

- **Platform level — agents write code.** New components, skins, animation primitives, and sound synths enter through the Cadherin codebase: PR, tests, review, shipped for every game. (Cadherin v0 was itself agent-written.)
- **Game level — agents write data, never code.** A GameSpec customizes presentation through validated, closed-vocabulary data. A game containing code could not be simulated, moderated, remixed, or trusted around children. This boundary is permanent.

Presentation can never alter outcomes: the kernel's event log is byte-identical for a seed regardless of theme, skin, or art. Juice subscribes to events; it does not produce them. (Hearthstone's META_DATA lineage, via the Kotlin prototype's AnimationHints.)

## The rungs

| Rung | What | Authored as | Status |
|---|---|---|---|
| 0 | Rules, RNG, legality, hidden info, the event stream | — (sealed) | ✅ built |
| 1 | **Semantic auto-layout** — owner+visibility → stack/hand/row/pile; every game playable with zero config | nothing (inferred) | ✅ built (v0) |
| 2 | **Theme tokens** — felt/ink/accent colors, card size & ratio, fonts, radius, motion intensity, celebration level, sound set | `presentation.theme` data in GameSpec; maps to the CSS custom properties the stylesheet is already structured around | 📐 designed; grammar slot exists |
| 3 | **Skins** — named renderers per semantic role: hand `fan\|arc\|grid`, capture `slide\|arc-trail\|burst`, board `felt\|wood\|space` | `presentation.skins` selections from a closed catalog; the catalog grows via platform PRs | 📐 designed |
| 4 | **Asset packs** — card art, backgrounds, audio by *purpose* (mantle MediaPurposePolicy); replace procedural SVG where present, fall back where absent | registry kinds (`Theme`/`AssetPack`), referenced from games | 🗓 E4 (img-gen MCP plugs in here) |
| 5 | **Pixi juice layer** — dense particles, shader glow, trails, tilemaps | platform code subscribing to the same event stream | 🗓 reserved (user approved Pixi 8.19); RPG tilemaps are its forcing function |

**Runtime/user settings** (never game data): reduced motion, sound on/off, card scale, colorblind palettes. Client preferences, WCAG posture.

## Deterministic vs customizable — the contract

| Sealed (deterministic) | Customizable as game data (on the go) | Customizable as platform code |
|---|---|---|
| outcomes, RNG, replays | theme tokens | new components/skins/templates |
| legality + hidden-info projection | skin selections + parameters | animation & particle primitives |
| the ordered event stream | animation intensity / celebration level | sound synthesis sets |
| QA badges (simulated, not declared) | card-face content mapping; asset refs (E4) | the Pixi layer |

## The road to mobile-game feel

v0 ships ~20% of the feel (FLIP movement, 3D flips, hover lift, focus). **DOM + WAAPI + WebAudio carries ~80% for card/board games**; the Pixi rung covers the rest. The juice catalog, in priority order:

1. **Sound (biggest single win)** — WebAudio *synthesized* taps/whooshes/chimes/fanfares: zero binary assets, fully agent-authorable, same philosophy as procedural card art.
2. **Motion quality** — springy overshoot easing, arced flight paths with rotation, staggered deal-in cascade at game start, elevation shadows during flight.
3. **Touch feedback** — press-down scale (<100 ms), idle pulse on playable cards.
4. **Celebration** — win confetti, "+N!" pops, escalating match streaks, end-screen ceremony.
5. **Atmosphere** — vignette, gradient depth, subtle parallax.

**Feel is inside the verification loop**: presentation is data and pages regenerate in milliseconds, so an agent can tune → render → screenshot → judge → iterate (demonstrated with the preview browser, 2026-06-11). Future agent-evals can score contrast/readability the way simulate scores balance.

## Proposed next increment — E1.5 "the juice pass"

Implement rung 2 + catalog items 1–4 in DOM/WebAudio: `presentation.theme` grammar (closed vocabulary + lints), token-driven stylesheet, spring/arc/stagger motion, synthesized sound set with mute control, celebration sequence. Pixi stays sheathed until a template earns it (likely E7 tilemaps).
