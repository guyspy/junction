import { describe, expect, it } from "vitest";
import { parseGameDocument, type Diagnostic } from "../src/index.js";

/** Lints for the flip/chosen-target/resolveEqualPair extensions. */
const base = `
apiVersion: games.junction.aotter.net/v1alpha1
kind: Game
metadata: { name: mem }
spec:
  meta:
    title: Mem
    seats: { min: 2, max: 2 }
  zones:
    - { name: grid, owner: shared, visibility: none }
    - { name: won, owner: seat, visibility: all }
  pieces:
    - name: tile
      properties: { value: { type: int } }
      generate: { cartesian: { value: [1, 2] } }
      copies: 2
  setup:
    - { op: create, pieces: tile, into: grid }
    - { op: shuffle, zone: grid }
  turn:
    order: roundRobin
    phases:
      - { name: a, actions: [flip] }
      - { name: b, actions: [flip] }
  actions:
    - name: flip
      flip: { zone: { zone: grid }, target: chosen, direction: faceUp }
  triggers:
    - name: resolve
      on: { event: pieceFlipped, inZone: grid }
      when: "zones.grid.faceUpCount == 2"
      effects:
        - resolveEqualPair: { zone: grid, property: value, toZone: won, onMatch: goAgain, onMismatch: flipDown }
  end:
    when: "zones.grid.allEmpty"
    winner: { mostPiecesIn: won }
`;

function codes(d: readonly Diagnostic[]): string[] {
  return d.map((x) => x.code);
}

describe("v1alpha grammar extensions", () => {
  it("accepts the memory-match base", () => {
    expect(parseGameDocument(base).ok).toBe(true);
  });

  it("rejects an action declaring both move and flip", () => {
    const bad = base.replace(
      "flip: { zone: { zone: grid }, target: chosen, direction: faceUp }",
      "flip: { zone: { zone: grid }, target: chosen, direction: faceUp }\n      move: { from: { zone: grid }, to: { zone: won, owner: actor } }",
    );
    const r = parseGameDocument(bad);
    expect(r.ok).toBe(false);
  });

  it("rejects faceUpCount on an owner zone", () => {
    const bad = base.replace('when: "zones.grid.faceUpCount == 2"', 'when: "zones.won.faceUpCount == 2"');
    const r = parseGameDocument(bad);
    expect(r.ok).toBe(false);
    if (r.ok) return;
    const d = r.diagnostics.find((x) => x.code === "EXPRESSION_REF_INVALID")!;
    expect(d.expected).toContain("shared zones");
  });

  it("rejects pieceFlipped trigger using intoZone (should be inZone)", () => {
    const bad = base.replace(
      "on: { event: pieceFlipped, inZone: grid }",
      "on: { event: pieceFlipped, intoZone: grid }",
    );
    const r = parseGameDocument(bad);
    expect(r.ok).toBe(false);
    if (r.ok) return;
    expect(codes(r.diagnostics)).toContain("SCHEMA_VALIDATION_FAILED");
  });

  it("rejects resolveEqualPair targeting an unknown property with a suggestion", () => {
    const bad = base.replace(
      "resolveEqualPair: { zone: grid, property: value, toZone: won, onMatch: goAgain, onMismatch: flipDown }",
      "resolveEqualPair: { zone: grid, property: valeu, toZone: won, onMatch: goAgain, onMismatch: flipDown }",
    );
    const r = parseGameDocument(bad);
    expect(r.ok).toBe(false);
    if (r.ok) return;
    const d = r.diagnostics.find((x) => x.code === "SCHEMA_VALIDATION_FAILED" && x.suggestion === "value");
    expect(d).toBeDefined();
  });

  it("rejects resolveEqualPair on a non-shared zone", () => {
    const bad = base.replace(
      "resolveEqualPair: { zone: grid, property: value, toZone: won, onMatch: goAgain, onMismatch: flipDown }",
      "resolveEqualPair: { zone: won, property: value, toZone: won, onMatch: goAgain, onMismatch: flipDown }",
    );
    const r = parseGameDocument(bad);
    expect(r.ok).toBe(false);
    if (r.ok) return;
    const d = r.diagnostics.find((x) => x.expected?.includes("common to all seats"));
    expect(d).toBeDefined();
  });
});
