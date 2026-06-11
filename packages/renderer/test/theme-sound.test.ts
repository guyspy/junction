// @vitest-environment happy-dom
import { parseGameDocument } from "@junction/spec";
import { describe, expect, it } from "vitest";
import {
  applyThemeTokens,
  celebrationColors,
  createSoundBank,
  motionParams,
  resolveTheme,
  soundForEvent,
} from "../src/index.js";

const MINI = `
apiVersion: games.junction.aotter.net/v1alpha1
kind: Game
metadata: { name: mini }
spec:
  meta: { title: Mini, seats: { min: 2, max: 2 } }
  zones:
    - { name: deck, owner: seat, visibility: none }
    - { name: pot, owner: shared, visibility: all }
    - { name: won, owner: seat, visibility: all }
  pieces:
    - { name: card, properties: { rank: { type: int } }, generate: { cartesian: { rank: [2, 3] } } }
  setup:
    - { op: create, pieces: card, into: pot }
    - { op: deal, from: pot, to: deck, count: all }
  turn: { order: roundRobin, phases: [{ name: battle, actions: [play] }] }
  actions:
    - { name: play, move: { from: { zone: deck, owner: actor }, to: { zone: pot }, reveal: true } }
  end: { when: "zones.deck.allEmpty", winner: { mostPiecesIn: won } }
`;

const THEMED = MINI + `
  presentation:
    theme: { table: ocean, accent: sky, motion: calm, sound: arcade }
`;

function parse(yaml: string) {
  const parsed = parseGameDocument(yaml);
  if (!parsed.ok) throw new Error(parsed.diagnostics.map((d) => d.message).join("\n"));
  return parsed.data;
}

describe("theme tokens (rung 2)", () => {
  it("defaults apply when presentation is absent", () => {
    const theme = resolveTheme(parse(MINI).spec);
    expect(theme).toEqual({
      table: "forest",
      accent: "gold",
      cardSize: "regular",
      motion: "lively",
      celebration: "festive",
      sound: "soft",
    });
  });

  it("declared theme overrides defaults and maps to CSS custom properties", () => {
    const theme = resolveTheme(parse(THEMED).spec);
    expect(theme.table).toBe("ocean");
    expect(theme.sound).toBe("arcade");

    const root = document.createElement("div");
    const shell = document.createElement("div");
    applyThemeTokens(root, shell, theme);
    expect(shell.style.getPropertyValue("--felt")).toBe("#1d5379"); // ocean
    expect(shell.style.getPropertyValue("--accent")).toBe("#7be3ff"); // sky
    expect(shell.style.getPropertyValue("--card-w")).toBe("76px");
  });

  it("motion levels shape the physics: calm has no arc, bouncy overshoots most", () => {
    const calm = motionParams({ ...resolveTheme(parse(MINI).spec), motion: "calm" });
    const bouncy = motionParams({ ...resolveTheme(parse(MINI).spec), motion: "bouncy" });
    expect(calm.arc).toBe(0);
    expect(calm.tilt).toBe(0);
    expect(bouncy.arc).toBeGreaterThan(0.2);
    expect(bouncy.moveDuration).toBeGreaterThan(calm.moveDuration);
  });

  it("celebration colors derive from the theme", () => {
    const colors = celebrationColors(resolveTheme(parse(THEMED).spec));
    expect(colors).toContain("#7be3ff"); // sky accent leads the confetti
    expect(colors.length).toBeGreaterThanOrEqual(4);
  });
});

describe("synth sound bank", () => {
  it("an 'off' bank never throws, even without AudioContext", () => {
    const bank = createSoundBank("off");
    expect(() => {
      bank.unlock();
      bank.play("fanfare");
    }).not.toThrow();
  });

  it("mute round-trips", () => {
    const bank = createSoundBank("soft", true);
    expect(bank.isMuted()).toBe(true);
    bank.setMuted(false);
    expect(bank.isMuted()).toBe(false);
  });

  it("maps events to the right sounds from the viewer's perspective", () => {
    expect(soundForEvent({ type: "pairResolved", matched: true }, 0)).toBe("match");
    expect(soundForEvent({ type: "pairResolved", matched: false }, 0)).toBe("mismatch");
    expect(soundForEvent({ type: "zoneResolved", winnerSeat: 0 }, 0)).toBe("winTrick");
    expect(soundForEvent({ type: "zoneResolved", winnerSeat: 1 }, 0)).toBe("loseTrick");
    expect(soundForEvent({ type: "gameEnded", winnerSeat: 0 }, 0)).toBe("fanfare");
    expect(soundForEvent({ type: "gameEnded", winnerSeat: 1 }, 0)).toBe("defeat");
    expect(soundForEvent({ type: "triggerFired" }, 0)).toBeNull();
  });
});
