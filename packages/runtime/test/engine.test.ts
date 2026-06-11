import { parseGameDocument, type GameDocument } from "@junction/spec";
import { describe, expect, it } from "vitest";
import { applyAction, buildInitialState, createRng, legalMoves } from "../src/index.js";

/**
 * A two-card deterministic mini-game (no shuffle): seat0 holds rank 3, seat1 holds rank 2.
 * Exercises setup, legality, the move pipeline, the trigger cascade, resolveHighest,
 * end conditions, and tie semantics — all without randomness.
 */
function miniGame(ranks: readonly number[]): GameDocument {
  const yaml = `
apiVersion: games.junction.aotter.net/v1alpha1
kind: Game
metadata: { name: mini }
spec:
  meta:
    title: Mini
    seats: { min: 2, max: 2 }
  zones:
    - { name: deck, owner: seat, visibility: none }
    - { name: pot, owner: shared, visibility: all }
    - { name: won, owner: seat, visibility: all }
  pieces:
    - name: card
      properties:
        rank: { type: int }
      generate: { cartesian: { rank: [${ranks.join(", ")}] } }
  setup:
    - { op: create, pieces: card, into: pot }
    - { op: deal, from: pot, to: deck, count: all }
  turn:
    order: roundRobin
    phases: [{ name: battle, actions: [play] }]
  actions:
    - name: play
      move: { from: { zone: deck, owner: actor }, to: { zone: pot }, reveal: true }
  triggers:
    - name: resolve
      on: { event: pieceMoved, intoZone: pot }
      when: "zones.pot.count >= seats.count && turn.seatIndex == seats.count - 1"
      effects:
        - resolveHighest: { zone: pot, property: rank, toWinnerZone: won, onTie: stay }
  end:
    when: "zones.deck.allEmpty"
    winner: { mostPiecesIn: won }
`;
  const parsed = parseGameDocument(yaml);
  if (!parsed.ok) throw new Error(parsed.diagnostics.map((d) => d.message).join("\n"));
  return parsed.data;
}

describe("kernel engine semantics (deterministic mini-game)", () => {
  // create order: card-0 (rank 3 first value? no — cartesian order) …
  // generate cartesian {rank: [3, 2]} → card-0 rank 3, card-1 rank 2.
  // deal pops from the top (end): card-1 → seat0, card-0 → seat1.

  it("sets up dealt decks round-robin from the top", () => {
    const doc = miniGame([3, 2]);
    const { state } = buildInitialState(doc, 2, createRng("x"));
    expect(state.zones["deck#0"]).toHaveLength(1);
    expect(state.zones["deck#1"]).toHaveLength(1);
    expect(state.pieces[state.zones["deck#0"]![0]!.pieceId]!.properties["rank"]).toBe(2);
    expect(state.pieces[state.zones["deck#1"]![0]!.pieceId]!.properties["rank"]).toBe(3);
  });

  it("plays a full battle: higher rank takes the pot, game ends, winner computed", () => {
    const doc = miniGame([3, 2]);
    const { state: s0 } = buildInitialState(doc, 2, createRng("x"));

    expect(legalMoves(s0, doc.spec)).toEqual([{ action: "play" }]);
    const r1 = applyAction(s0, doc.spec, { seat: 0, action: "play" });
    expect(r1.ok).toBe(true);
    if (!r1.ok) return;
    // seat0 played rank 2 into the pot; no resolution yet (seat 0 is not the last seat).
    expect(r1.data.state.zones["pot"]).toHaveLength(1);
    const moved = r1.data.events.find((e) => e.type === "pieceMoved");
    expect(moved?.type === "pieceMoved" && moved.revealed?.properties["rank"]).toBe(2);

    const r2 = applyAction(r1.data.state, doc.spec, { seat: 1, action: "play" });
    expect(r2.ok).toBe(true);
    if (!r2.ok) return;
    const types = r2.data.events.map((e) => e.type);
    expect(types).toContain("triggerFired");
    expect(types).toContain("zoneResolved");
    expect(types).toContain("gameEnded");

    const final = r2.data.state;
    expect(final.status).toBe("ended");
    expect(final.winnerSeat).toBe(1); // seat1's rank 3 beats seat0's rank 2
    expect(final.zones["pot"]).toHaveLength(0);
    expect(final.zones["won#1"]).toHaveLength(2);
  });

  it("a tie leaves the pot in place and the game can end in a draw", () => {
    const doc = miniGame([5, 5]);
    const { state: s0 } = buildInitialState(doc, 2, createRng("x"));
    const r1 = applyAction(s0, doc.spec, { seat: 0, action: "play" });
    if (!r1.ok) throw new Error("r1");
    const r2 = applyAction(r1.data.state, doc.spec, { seat: 1, action: "play" });
    if (!r2.ok) throw new Error("r2");

    const resolved = r2.data.events.find((e) => e.type === "zoneResolved");
    expect(resolved?.type === "zoneResolved" && resolved.winnerSeat).toBeNull();
    expect(r2.data.state.zones["pot"]).toHaveLength(2); // onTie: stay
    expect(r2.data.state.status).toBe("ended"); // decks empty
    expect(r2.data.state.winnerSeat).toBeNull(); // 0–0 in won ⇒ draw
  });

  it("rejects out-of-turn and illegal actions with runtime diagnostics", () => {
    const doc = miniGame([3, 2]);
    const { state: s0 } = buildInitialState(doc, 2, createRng("x"));

    const wrongSeat = applyAction(s0, doc.spec, { seat: 1, action: "play" });
    expect(!wrongSeat.ok && wrongSeat.diagnostics[0]!.code).toBe("SEAT_NOT_ACTIVE");

    const unknown = applyAction(s0, doc.spec, { seat: 0, action: "cheat" });
    expect(!unknown.ok && unknown.diagnostics[0]!.code).toBe("ACTION_NOT_LEGAL");
  });

  it("the reducer never mutates its input state", () => {
    const doc = miniGame([3, 2]);
    const { state: s0 } = buildInitialState(doc, 2, createRng("x"));
    const snapshot = JSON.stringify(s0);
    applyAction(s0, doc.spec, { seat: 0, action: "play" });
    expect(JSON.stringify(s0)).toBe(snapshot);
  });
});
