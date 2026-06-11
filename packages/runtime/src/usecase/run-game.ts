import type { GameDocument } from "@junction/spec";
import { createRng, type Rng } from "../kernel/rng.js";
import type { GameEvent } from "../domain/model/events.js";
import type { GameState } from "../domain/model/state.js";
import { applyAction, applySkip, legalMoves, type PlayerMove } from "../domain/service/reducer.js";
import { buildInitialState } from "../domain/service/setup.js";

export interface Chooser {
  (legal: readonly PlayerMove[], rng: Rng): PlayerMove;
}

/** The default playtest bot: uniformly random over legal moves. */
export const randomChooser: Chooser = (legal, rng) => legal[rng.int(legal.length)]!;

export interface RunGameOptions {
  readonly seats: number;
  readonly seed: string | number;
  readonly maxTurns?: number;
  readonly chooser?: Chooser;
}

export interface RunGameResult {
  readonly state: GameState;
  readonly events: readonly GameEvent[];
  readonly steps: number;
  /** True when maxTurns elapsed before the game ended — a termination red flag. */
  readonly capped: boolean;
}

/**
 * Drive one full deterministic game: same doc + same options ⇒ byte-identical event log.
 * Setup randomness and bot randomness use independent streams derived from the seed.
 */
export function runGame(doc: GameDocument, options: RunGameOptions): RunGameResult {
  const maxTurns = options.maxTurns ?? 1000;
  const chooser = options.chooser ?? randomChooser;
  const setupRng = createRng(`${options.seed}:setup`);
  const botRng = createRng(`${options.seed}:bot`);

  const setup = buildInitialState(doc, options.seats, setupRng);
  let state = setup.state;
  const events: GameEvent[] = [...setup.events];
  let steps = 0;

  while (state.status === "running" && steps < maxTurns) {
    const legal = legalMoves(state, doc.spec);
    if (legal.length === 0) {
      const result = applySkip(state, doc.spec);
      state = result.state;
      events.push(...result.events);
    } else {
      const move = chooser(legal, botRng);
      const result = applyAction(state, doc.spec, { seat: state.activeSeat, ...move });
      if (!result.ok) {
        // The chooser picked from `legal`, so this is an engine bug — surface loudly.
        throw new Error(`reducer rejected a legal move: ${result.diagnostics[0]?.message}`);
      }
      state = result.data.state;
      events.push(...result.data.events);
    }
    steps++;
  }

  return { state, events, steps, capped: state.status === "running" };
}
