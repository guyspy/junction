import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { parseGameDocument, type GameDocument } from "@junction/spec";
import { describe, expect, it } from "vitest";
import {
  applyAction,
  buildInitialState,
  createRng,
  legalMoves,
  projectState,
  runGame,
  simulate,
} from "../src/index.js";

function loadMemory(): GameDocument {
  const text = readFileSync(
    fileURLToPath(new URL("../../../games/memory-match.yaml", import.meta.url)),
    "utf8",
  );
  const parsed = parseGameDocument(text, { file: "games/memory-match.yaml" });
  if (!parsed.ok) throw new Error(parsed.diagnostics.map((d) => d.message).join("\n"));
  return parsed.data;
}

describe("memory-match — flip, chosen targets, goAgain, projection", () => {
  it("offers one chosen-flip move per face-down tile", () => {
    const doc = loadMemory();
    const { state } = buildInitialState(doc, 2, createRng("m"));
    const moves = legalMoves(state, doc.spec);
    expect(moves).toHaveLength(16);
    expect(moves.every((m) => m.action === "flip" && typeof m.target === "string")).toBe(true);
  });

  it("a matching pair is taken and the same seat goes again", () => {
    const doc = loadMemory();
    const { state: s0 } = buildInitialState(doc, 2, createRng("m"));
    // Find two grid tiles with equal value and flip them in turn (seat 0).
    const grid0 = s0.zones["grid"]!;
    const byValue = new Map<number, string[]>();
    for (const e of grid0) {
      const v = Number(s0.pieces[e.pieceId]!.properties["value"]);
      byValue.set(v, [...(byValue.get(v) ?? []), e.pieceId]);
    }
    const pair = [...byValue.values()].find((ids) => ids.length === 2)!;

    const r1 = applyAction(s0, doc.spec, { seat: 0, action: "flip", target: pair[0]! });
    expect(r1.ok).toBe(true);
    if (!r1.ok) return;
    const r2 = applyAction(r1.data.state, doc.spec, { seat: 0, action: "flip", target: pair[1]! });
    expect(r2.ok).toBe(true);
    if (!r2.ok) return;

    const types = r2.data.events.map((e) => e.type);
    expect(types).toContain("pairResolved");
    const resolved = r2.data.events.find((e) => e.type === "pairResolved");
    expect(resolved?.type === "pairResolved" && resolved.matched).toBe(true);

    const after = r2.data.state;
    expect(after.zones["won#0"]).toHaveLength(2);
    expect(after.zones["grid"]).toHaveLength(14);
    expect(after.activeSeat).toBe(0); // goAgain kept the turn
    expect(after.phaseIndex).toBe(0); // restarted at the first flip phase
  });

  it("a mismatch flips both tiles back down and passes the turn", () => {
    const doc = loadMemory();
    const { state: s0 } = buildInitialState(doc, 2, createRng("m"));
    const grid0 = s0.zones["grid"]!;
    // Two tiles of differing value.
    const a = grid0[0]!.pieceId;
    const b = grid0.find((e) => s0.pieces[e.pieceId]!.properties["value"] !== s0.pieces[a]!.properties["value"])!.pieceId;

    const r1 = applyAction(s0, doc.spec, { seat: 0, action: "flip", target: a });
    if (!r1.ok) throw new Error("r1");
    const r2 = applyAction(r1.data.state, doc.spec, { seat: 0, action: "flip", target: b });
    if (!r2.ok) throw new Error("r2");

    const resolved = r2.data.events.find((e) => e.type === "pairResolved");
    expect(resolved?.type === "pairResolved" && resolved.matched).toBe(false);
    const after = r2.data.state;
    expect(after.zones["grid"]).toHaveLength(16); // nothing taken
    expect(after.pieces[a]!.faceUp).toBe(false); // flipped back down
    expect(after.pieces[b]!.faceUp).toBe(false);
    expect(after.activeSeat).toBe(1); // turn passed
  });

  it("projection hides face-down grid tiles but reveals flipped ones", () => {
    const doc = loadMemory();
    const { state: s0 } = buildInitialState(doc, 2, createRng("m"));
    const target = s0.zones["grid"]![0]!.pieceId;
    const r1 = applyAction(s0, doc.spec, { seat: 0, action: "flip", target });
    if (!r1.ok) throw new Error("r1");

    const view = projectState(r1.data.state, doc.spec, 1); // opponent's view
    const grid = view.zones.find((z) => z.zone === "grid")!;
    expect(grid.count).toBe(16);
    const revealed = grid.entries.filter((e) => !("hidden" in e));
    const hidden = grid.entries.filter((e) => "hidden" in e);
    expect(revealed).toHaveLength(1); // only the flipped tile is visible
    expect(hidden).toHaveLength(15); // the rest stay face-down to seat 1
    expect(revealed[0]).toMatchObject({ pieceId: target });
  });

  it("golden replay: deterministic and fully terminating", () => {
    const doc = loadMemory();
    const a = runGame(doc, { seats: 2, seed: "mm-golden" });
    const b = runGame(doc, { seats: 2, seed: "mm-golden" });
    expect(JSON.stringify(a.events)).toBe(JSON.stringify(b.events));
    expect(a.capped).toBe(false);
    expect(a.state.status).toBe("ended");
    // Every tile ends in someone's won pile (grid fully drains).
    const won = (a.state.zones["won#0"]?.length ?? 0) + (a.state.zones["won#1"]?.length ?? 0);
    expect(won).toBe(16);
  });

  it("simulate: 300 games terminate and are seat-symmetric", () => {
    const doc = loadMemory();
    const report = simulate(doc, { games: 300, seed: 9 });
    expect(report.capped).toBe(0);
    expect(report.completed).toBe(300);
    expect(Math.abs(report.winRateBySeat[0]! - report.winRateBySeat[1]!)).toBeLessThan(0.1);
  });
});
