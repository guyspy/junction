import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { parseGameDocument, type GameDocument } from "@junction/spec";
import { describe, expect, it } from "vitest";
import { runGame, simulate } from "../src/index.js";

function loadWar(): GameDocument {
  const text = readFileSync(
    fileURLToPath(new URL("../../../games/war.yaml", import.meta.url)),
    "utf8",
  );
  const parsed = parseGameDocument(text, { file: "games/war.yaml" });
  if (!parsed.ok) throw new Error(parsed.diagnostics.map((d) => d.message).join("\n"));
  return parsed.data;
}

describe("war.yaml — the first reference game", () => {
  it("golden replay: same seed ⇒ byte-identical event log; different seed diverges", () => {
    const doc = loadWar();
    const a = runGame(doc, { seats: 2, seed: "golden-1" });
    const b = runGame(doc, { seats: 2, seed: "golden-1" });
    const c = runGame(doc, { seats: 2, seed: "golden-2" });

    const logA = JSON.stringify(a.events);
    expect(JSON.stringify(b.events)).toBe(logA);
    expect(JSON.stringify(c.events)).not.toBe(logA);

    // Structural pin: a War game is exactly 52 plays, and the log shape is stable.
    expect(a.steps).toBe(52);
    expect(a.capped).toBe(false);
    expect(a.state.status).toBe("ended");
    const hash = createHash("sha256").update(logA).digest("hex").slice(0, 16);
    expect({ steps: a.steps, events: a.events.length, hash }).toMatchSnapshot();
  });

  it("every card is played exactly once (no resurrection, no loss)", () => {
    const doc = loadWar();
    const { events, state } = runGame(doc, { seats: 2, seed: "audit" });
    const playsPerPiece = new Map<string, number>();
    for (const e of events)
      if (e.type === "pieceMoved" && e.to === "pot")
        playsPerPiece.set(e.pieceId, (playsPerPiece.get(e.pieceId) ?? 0) + 1);
    expect(playsPerPiece.size).toBe(52);
    expect([...playsPerPiece.values()].every((n) => n === 1)).toBe(true);

    // Conservation: 52 pieces distributed across won piles + a possibly tied final pot.
    const total =
      (state.zones["won#0"]?.length ?? 0) +
      (state.zones["won#1"]?.length ?? 0) +
      (state.zones["pot"]?.length ?? 0);
    expect(total).toBe(52);
  });

  it("simulate: 200 games all terminate, roughly balanced, ~52 turns", () => {
    const doc = loadWar();
    const report = simulate(doc, { games: 200, seed: 7 });

    expect(report.capped).toBe(0);
    expect(report.stalled).toBe(0);
    expect(report.completed).toBe(200);
    expect(report.turns.min).toBe(52);
    expect(report.turns.max).toBe(52);

    // Rank symmetry ⇒ no structural seat advantage; allow sampling noise.
    for (const rate of report.winRateBySeat) {
      expect(rate).toBeGreaterThan(0.3);
      expect(rate).toBeLessThan(0.7);
    }
    expect(report.actionUsage["play-card"]).toBe(200 * 52);
    expect(report.notes.some((n) => n.includes("✓ all 200 games terminated"))).toBe(true);
  });
});
