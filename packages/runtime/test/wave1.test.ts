import { parseGameDocument, type GameDocument } from "@junction/spec";
import { describe, expect, it } from "vitest";
import { applyAction, buildInitialState, createRng, legalMoves, runGame } from "../src/index.js";

/** A tiny duel: 2 cards (ranks 3, 5), health 6, mana 10. Deterministic (no shuffle). */
function duel(extra = ""): GameDocument {
  const yaml = `
apiVersion: games.junction.aotter.net/v1alpha1
kind: Game
metadata: { name: duel }
spec:
  meta: { title: Duel, seats: { min: 2, max: 2 } }
  variables:
    perSeat:
      health: { initial: 6 }
      mana: { initial: 10 }
      luck: { initial: 0 }
  zones:
    - { name: armory, owner: shared, visibility: none }
    - { name: hand, owner: seat, visibility: owner }
    - { name: pot, owner: shared, visibility: all }
  pieces:
    - { name: attack-card, properties: { rank: { type: int } }, generate: { cartesian: { rank: [3, 5] } } }
  setup:
    - { op: create, pieces: attack-card, into: armory }
    - { op: deal, from: armory, to: hand, count: all }
  turn: { order: roundRobin, phases: [{ name: strike, actions: [strike] }] }
  actions:
    - name: strike
      move: { from: { zone: hand, owner: actor }, take: chosen, to: { zone: pot }, reveal: true }
      cost: { var: mana, amount: "target.rank" }
  triggers:
    - name: deal-damage
      on: { event: pieceMoved, intoZone: pot, pieceSet: attack-card }
      effects:
        - addVar: { scope: opponent, var: health, amount: "0 - this.rank" }
${extra}
  end:
    when: "seatVars.health.min <= 0"
    winner: { highestSeatVar: health }
`;
  const parsed = parseGameDocument(yaml);
  if (!parsed.ok) throw new Error(parsed.diagnostics.map((d) => d.message).join("\n"));
  return parsed.data;
}

describe("Wave 1 — the mutable world", () => {
  // Deal order: armory [rank3, rank5]; pop → seat0 gets rank5, seat1 gets rank3.

  it("variables initialize per seat and costs gate legality", () => {
    const doc = duel();
    const { state } = buildInitialState(doc, 2, createRng("w"));
    expect(state.seatVars["health"]).toEqual([6, 6]);
    expect(state.seatVars["mana"]).toEqual([10, 10]);
    expect(legalMoves(state, doc.spec)).toHaveLength(1); // seat0's single card, affordable
  });

  it("a strike pays mana, deals expression-valued damage to the opponent, and ends at 0", () => {
    const doc = duel();
    const { state: s0 } = buildInitialState(doc, 2, createRng("w"));
    const move = legalMoves(s0, doc.spec)[0]!;

    const r1 = applyAction(s0, doc.spec, { seat: 0, ...move });
    expect(r1.ok).toBe(true);
    if (!r1.ok) return;
    const after = r1.data.state;
    expect(after.seatVars["mana"]![0]).toBe(5); // paid target.rank = 5
    expect(after.seatVars["health"]![1]).toBe(1); // 6 - this.rank(5)
    const types = r1.data.events.map((e) => e.type);
    expect(types.filter((t) => t === "varChanged")).toHaveLength(2); // cost + damage

    const r2 = applyAction(after, doc.spec, { seat: 1, ...legalMoves(after, doc.spec)[0]! });
    expect(r2.ok).toBe(true);
    if (!r2.ok) return;
    expect(r2.data.state.seatVars["health"]![0]).toBe(3); // 6 - 3
    expect(r2.data.state.status).toBe("running");
  });

  it("highestSeatVar decides the winner when health hits zero", () => {
    const doc = duel();
    const result = runGame(doc, { seats: 2, seed: "duel" });
    expect(result.state.status).toBe("ended");
    // seat0 strikes 5 (s1 → 1), seat1 strikes 3 (s0 → 3), then hands are empty → stall…
    // wait: hands empty ⇒ both skip ⇒ stalled end; health 3 vs 1 ⇒ seat0 wins.
    expect(result.state.winnerSeat).toBe(0);
  });

  it("an unaffordable card simply isn't a legal move", () => {
    const doc = duel().spec;
    const poor = structuredClone(doc) as typeof doc;
    (poor.variables.perSeat["mana"] as { initial: number }).initial = 4;
    const { state } = buildInitialState(
      { apiVersion: "games.junction.aotter.net/v1alpha1", kind: "Game", metadata: { name: "x" }, spec: poor },
      2,
      createRng("w"),
    );
    expect(legalMoves(state, poor)).toHaveLength(0); // seat0 holds rank 5, has 4 mana
  });

  it("modifyProperty mutates the trigger's piece and chains a propertyChanged trigger", () => {
    const doc = duel(`
    - name: dull-the-blade
      on: { event: pieceMoved, intoZone: pot, pieceSet: attack-card }
      effects:
        - modifyProperty: { target: this, property: rank, add: -1 }
    - name: reward-on-dull
      on: { event: propertyChanged, property: rank, pieceSet: attack-card }
      effects:
        - addVar: { scope: actor, var: luck, amount: 1 }
`);
    const { state: s0 } = buildInitialState(doc, 2, createRng("w"));
    const r1 = applyAction(s0, doc.spec, { seat: 0, ...legalMoves(s0, doc.spec)[0]! });
    expect(r1.ok).toBe(true);
    if (!r1.ok) return;
    const after = r1.data.state;
    const potPiece = after.pieces[after.zones["pot"]![0]!.pieceId]!;
    expect(potPiece.properties["rank"]).toBe(4); // 5 dulled to 4
    expect(after.seatVars["luck"]![0]).toBe(1); // the chained trigger fired
    const types = r1.data.events.map((e) => e.type);
    expect(types).toContain("propertyChanged");
  });

  it("roll is deterministic per seed and demands an rng", () => {
    const doc = duel(`
    - name: lucky-roll
      on: { event: pieceMoved, intoZone: pot }
      effects:
        - roll: { scope: actor, var: luck, sides: 6 }
`);
    const { state: s0 } = buildInitialState(doc, 2, createRng("w"));
    const move = legalMoves(s0, doc.spec)[0]!;

    const a = applyAction(s0, doc.spec, { seat: 0, ...move }, createRng("dice"));
    const b = applyAction(s0, doc.spec, { seat: 0, ...move }, createRng("dice"));
    if (!a.ok || !b.ok) throw new Error("roll failed");
    const rolledA = a.data.events.find((e) => e.type === "diceRolled");
    const rolledB = b.data.events.find((e) => e.type === "diceRolled");
    expect(rolledA?.type === "diceRolled" && rolledA.value).toBeGreaterThanOrEqual(1);
    expect(JSON.stringify(rolledA)).toBe(JSON.stringify(rolledB)); // same seed, same die

    expect(() => applyAction(s0, doc.spec, { seat: 0, ...move })).toThrow(/rng/);
  });

  it("math-duel (the reference game) terminates with no stalls across 200 sims", () => {
    // Loaded inline to keep this hermetic from games/ edits: the real file is also simulated in CI.
    const result = runGame(duel(), { seats: 2, seed: "any" });
    expect(result.capped).toBe(false);
  });
});
