/**
 * GameState — the deterministic world the reducer folds over.
 * Zone instance keys: shared zones use the zone name; owner zones use `name#seatIndex`.
 */

export interface PieceInstance {
  readonly id: string;
  /** The declaring piece set. */
  readonly decl: string;
  readonly properties: Readonly<Record<string, string | number>>;
}

export interface ZoneEntry {
  readonly pieceId: string;
  /** Seat that most recently moved this piece into the zone (null = setup). */
  readonly bySeat: number | null;
}

export type GameStatus = "running" | "ended";

export interface GameState {
  readonly status: GameStatus;
  readonly seats: number;
  /** 1-based round counter. */
  readonly round: number;
  readonly activeSeat: number;
  readonly phaseIndex: number;
  /** zone instance key → ordered entries ("top" = last element). */
  readonly zones: Readonly<Record<string, readonly ZoneEntry[]>>;
  readonly pieces: Readonly<Record<string, PieceInstance>>;
  readonly winnerSeat: number | null;
  /** Next event sequence number. */
  readonly seq: number;
  /** Consecutive skipped phases — the stall guard. */
  readonly consecutiveSkips: number;
}

export function zoneKey(zone: string, ownerSeat: number | null): string {
  return ownerSeat === null ? zone : `${zone}#${ownerSeat}`;
}
