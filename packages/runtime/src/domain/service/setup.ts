import type { GameDocument, PieceDecl } from "@junction/spec";
import { shuffled, type Rng } from "../../kernel/rng.js";
import type { GameEvent } from "../model/events.js";
import { zoneKey, type GameState, type PieceInstance, type ZoneEntry } from "../model/state.js";

/** Thrown for spec/world mismatches that validate-time lints cannot see (boot phase). */
export class SetupError extends Error {}

export interface SetupResult {
  readonly state: GameState;
  readonly events: readonly GameEvent[];
}

function generatePieces(decl: PieceDecl, idStart: number): PieceInstance[] {
  const out: PieceInstance[] = [];
  let n = idStart;
  if (decl.generate !== undefined) {
    const entries = Object.entries(decl.generate.cartesian);
    let combos: Record<string, string | number>[] = [{}];
    for (const [prop, values] of entries) {
      combos = combos.flatMap((c) => values.map((v) => ({ ...c, [prop]: v })));
    }
    for (const combo of combos)
      for (let copy = 0; copy < decl.copies; copy++)
        out.push({ id: `${decl.name}-${n++}`, decl: decl.name, properties: combo });
  } else {
    for (let copy = 0; copy < decl.copies; copy++)
      out.push({ id: `${decl.name}-${n++}`, decl: decl.name, properties: decl.values ?? {} });
  }
  return out;
}

export function buildInitialState(doc: GameDocument, seats: number, rng: Rng): SetupResult {
  const spec = doc.spec;
  if (seats < spec.meta.seats.min || seats > spec.meta.seats.max)
    throw new SetupError(
      `seats=${seats} outside [${spec.meta.seats.min}, ${spec.meta.seats.max}]`,
    );

  const zones: Record<string, ZoneEntry[]> = {};
  for (const zone of spec.zones) {
    if (zone.owner === "shared") zones[zoneKey(zone.name, null)] = [];
    else for (let seat = 0; seat < seats; seat++) zones[zoneKey(zone.name, seat)] = [];
  }
  const zoneOwner = new Map(spec.zones.map((z) => [z.name, z.owner]));
  const pieces: Record<string, PieceInstance> = {};
  let pieceCounter = 0;

  for (const op of spec.setup) {
    if (op.op === "create") {
      const decl = spec.pieces.find((p) => p.name === op.pieces)!;
      if (zoneOwner.get(op.into) !== "shared")
        throw new SetupError(`setup create must target a shared zone (got '${op.into}')`);
      const created = generatePieces(decl, pieceCounter);
      pieceCounter += created.length;
      for (const piece of created) {
        pieces[piece.id] = piece;
        zones[zoneKey(op.into, null)]!.push({ pieceId: piece.id, bySeat: null });
      }
    } else if (op.op === "shuffle") {
      if (zoneOwner.get(op.zone) !== "shared")
        throw new SetupError(`setup shuffle must target a shared zone (got '${op.zone}')`);
      const key = zoneKey(op.zone, null);
      zones[key] = shuffled(zones[key]!, rng);
    } else {
      if (zoneOwner.get(op.from) !== "shared")
        throw new SetupError(`setup deal must draw from a shared zone (got '${op.from}')`);
      if (zoneOwner.get(op.to) !== "seat")
        throw new SetupError(`setup deal must target an owner: seat zone (got '${op.to}')`);
      const fromKey = zoneKey(op.from, null);
      const source = zones[fromKey]!;
      const perSeat = op.count === "all" ? Number.POSITIVE_INFINITY : op.count;
      let dealt = 0;
      let seat = 0;
      const dealtPerSeat = new Array<number>(seats).fill(0);
      while (source.length > 0) {
        if (op.count !== "all" && dealtPerSeat.every((d) => d >= perSeat)) break;
        if (dealtPerSeat[seat]! < perSeat) {
          const entry = source.pop()!;
          zones[zoneKey(op.to, seat)]!.push(entry);
          dealtPerSeat[seat]!++;
          dealt++;
        }
        seat = (seat + 1) % seats;
        // Safety: if count: all, the loop drains the source; if numeric, the guard above exits.
        if (dealt > 1_000_000) throw new SetupError("deal overflow");
      }
    }
  }

  const events: GameEvent[] = [
    { seq: 0, type: "gameStarted", seats, game: doc.metadata.name },
    { seq: 1, type: "roundStarted", round: 1 },
    { seq: 2, type: "turnStarted", seat: 0, round: 1 },
  ];

  const state: GameState = {
    status: "running",
    seats,
    round: 1,
    activeSeat: 0,
    phaseIndex: 0,
    zones,
    pieces,
    winnerSeat: null,
    seq: events.length,
    consecutiveSkips: 0,
  };
  return { state, events };
}
