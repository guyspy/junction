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

  it("presentation.theme: defaults fill, declared values stick, bad enums fail", () => {
    const defaulted = parseGameDocument(base);
    expect(defaulted.ok && defaulted.data.spec.presentation.theme.table).toBe("forest");

    const themed = parseGameDocument(base.replace("spec:", "spec:\n  presentation: { theme: { table: plum, sound: arcade } }"));
    expect(themed.ok && themed.data.spec.presentation.theme.table).toBe("plum");
    expect(themed.ok && themed.data.spec.presentation.theme.accent).toBe("gold"); // default

    const bad = parseGameDocument(base.replace("spec:", "spec:\n  presentation: { theme: { table: lava } }"));
    expect(bad.ok).toBe(false);
    if (bad.ok) return;
    expect(codes(bad.diagnostics)).toContain("SCHEMA_VALIDATION_FAILED");
  });

  it("Wave 1: variables, costs, and scoped expressions lint correctly", () => {
    const wave1 = base
      .replace(
        "spec:",
        `spec:
  variables:
    perSeat: { mana: { initial: 5 } }
    global: { pool: { initial: 0 } }`,
      )
      .replace(
        "flip: { zone: { zone: grid }, target: chosen, direction: faceUp }",
        `flip: { zone: { zone: grid }, target: chosen, direction: faceUp }
      cost: { var: mana, amount: 1 }`,
      );
    expect(parseGameDocument(wave1).ok).toBe(true);

    // Unknown variable → VAR_REF_UNKNOWN with candidates.
    const badVar = wave1.replace("cost: { var: mana, amount: 1 }", "cost: { var: gold, amount: 1 }");
    const r1 = parseGameDocument(badVar);
    expect(!r1.ok && codes(r1.diagnostics)).toContain("VAR_REF_UNKNOWN");

    // seat.* is illegal in end conditions (no actor exists there).
    const badEnd = wave1.replace('when: "zones.grid.allEmpty"', 'when: "seat.mana <= 0"');
    const r2 = parseGameDocument(badEnd);
    expect(!r2.ok && r2.diagnostics.some((d) => d.code === "EXPRESSION_REF_INVALID" && d.expected?.includes("end conditions"))).toBe(true);

    // seatVars aggregates ARE legal in end conditions.
    const aggEnd = wave1.replace('when: "zones.grid.allEmpty"', 'when: "seatVars.mana.sum <= 0"');
    expect(parseGameDocument(aggEnd).ok).toBe(true);

    // target.* requires a chosen-target action.
    const topAction = wave1.replace(
      `flip: { zone: { zone: grid }, target: chosen, direction: faceUp }
      cost: { var: mana, amount: 1 }`,
      `move: { from: { zone: grid }, to: { zone: won, owner: actor } }
      requires: "target.value > 1"`,
      );
    const r3 = parseGameDocument(topAction);
    expect(!r3.ok && r3.diagnostics.some((d) => d.expected?.includes("chosen-target"))).toBe(true);

    // opponent scope demands an exactly-two-seat game.
    const wideSeats = wave1
      .replace("seats: { min: 2, max: 2 }", "seats: { min: 2, max: 4 }")
      .replace(
        "- resolveEqualPair: { zone: grid, property: value, toZone: won, onMatch: goAgain, onMismatch: flipDown }",
        "- addVar: { scope: opponent, var: mana, amount: 1 }",
      );
    const r4 = parseGameDocument(wideSeats);
    expect(!r4.ok && r4.diagnostics.some((d) => d.expected?.includes("exactly 2"))).toBe(true);
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
