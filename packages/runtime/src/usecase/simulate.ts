import type { GameDocument } from "@junction/spec";
import { runGame } from "./run-game.js";

/**
 * The hero feature (blueprint §6): play the game N times headlessly and report
 * what no one should have to discover in a classroom.
 */

export interface SimulateOptions {
  readonly games?: number;
  readonly seed?: string | number;
  readonly seats?: number;
  readonly maxTurns?: number;
}

export interface SimulateReport {
  readonly game: string;
  readonly games: number;
  readonly seats: number;
  /** Games that reached an end condition. */
  readonly completed: number;
  /** Games that hit the turn cap — a termination red flag. */
  readonly capped: number;
  /** Games that ended via the stall guard. */
  readonly stalled: number;
  readonly draws: number;
  readonly winsBySeat: readonly number[];
  readonly winRateBySeat: readonly number[];
  readonly turns: {
    readonly min: number;
    readonly max: number;
    readonly mean: number;
    readonly p50: number;
    readonly p95: number;
  };
  readonly actionUsage: Readonly<Record<string, number>>;
  /** Human/agent-readable verdicts. */
  readonly notes: readonly string[];
}

export function simulate(doc: GameDocument, options: SimulateOptions = {}): SimulateReport {
  const games = options.games ?? 100;
  const seed = options.seed ?? 42;
  const seats = options.seats ?? doc.spec.meta.seats.min;
  const maxTurns = options.maxTurns ?? 1000;

  let completed = 0;
  let capped = 0;
  let stalled = 0;
  let draws = 0;
  const winsBySeat = new Array<number>(seats).fill(0);
  const turnCounts: number[] = [];
  const actionUsage: Record<string, number> = {};

  for (let i = 0; i < games; i++) {
    const result = runGame(doc, { seats, seed: `${seed}#${i}`, maxTurns });
    turnCounts.push(result.steps);
    if (result.capped) {
      capped++;
      continue;
    }
    const ended = result.events.find((e) => e.type === "gameEnded");
    if (ended?.type === "gameEnded" && ended.reason === "stalled") stalled++;
    else completed++;
    if (result.state.winnerSeat === null) draws++;
    else winsBySeat[result.state.winnerSeat]!++;
    for (const event of result.events)
      if (event.type === "actionTaken")
        actionUsage[event.action] = (actionUsage[event.action] ?? 0) + 1;
  }

  const sorted = [...turnCounts].sort((a, b) => a - b);
  const pick = (q: number): number => sorted[Math.min(sorted.length - 1, Math.floor(q * sorted.length))] ?? 0;
  const decided = completed + stalled;
  const winRateBySeat = winsBySeat.map((w) => (decided === 0 ? 0 : w / decided));

  const notes: string[] = [];
  notes.push(
    capped === 0
      ? `✓ all ${games} games terminated (max ${sorted[sorted.length - 1] ?? 0} turns)`
      : `✖ ${capped}/${games} games hit the ${maxTurns}-turn cap — the game may not terminate`,
  );
  if (stalled > 0) notes.push(`⚠ ${stalled} games ended via the stall guard (no legal actions for a full round)`);
  const uniform = 1 / seats;
  const worst = Math.max(...winRateBySeat.map((r) => Math.abs(r - uniform)), 0);
  notes.push(
    worst <= 0.15
      ? `✓ seat win rates within ±15% of uniform (${winRateBySeat.map((r) => (r * 100).toFixed(1) + "%").join(" / ")})`
      : `⚠ seat balance skew: ${winRateBySeat.map((r) => (r * 100).toFixed(1) + "%").join(" / ")} — check first-player advantage`,
  );
  if (draws > 0) notes.push(`ℹ ${draws} draws (${((draws / games) * 100).toFixed(1)}%)`);
  const unusedActions = doc.spec.actions.filter((a) => (actionUsage[a.name] ?? 0) === 0);
  if (unusedActions.length > 0)
    notes.push(`⚠ actions never used: ${unusedActions.map((a) => a.name).join(", ")}`);

  return {
    game: doc.metadata.name,
    games,
    seats,
    completed,
    capped,
    stalled,
    draws,
    winsBySeat,
    winRateBySeat,
    turns: {
      min: sorted[0] ?? 0,
      max: sorted[sorted.length - 1] ?? 0,
      mean: turnCounts.length === 0 ? 0 : turnCounts.reduce((a, b) => a + b, 0) / turnCounts.length,
      p50: pick(0.5),
      p95: pick(0.95),
    },
    actionUsage,
    notes,
  };
}
