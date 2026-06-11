import { createInterface } from "node:readline";
import { stdin, stdout } from "node:process";
import type { GameDocument } from "@junction/spec";
import {
  applyAction,
  applySkip,
  buildInitialState,
  createRng,
  legalMoves,
  projectState,
  randomChooser,
  type GameState,
  type PlayerMove,
  type ProjectedState,
} from "@junction/runtime";

/**
 * `junction play` — a human plays one seat against random bots, in the terminal,
 * seeing only what their seat is entitled to see (per-seat projection). The first
 * time a person plays a Junction game in the TypeScript era.
 */

export interface PlayOptions {
  readonly seats: number;
  readonly seat: number;
  readonly seed: string;
}

/**
 * A line reader that buffers input and hands out lines on demand, surviving the
 * readline/promises buffered-pipe race. Returns null at end of input.
 */
function createLineReader(): { next: () => Promise<string | null>; close: () => void } {
  const rl = createInterface({ input: stdin, terminal: false });
  const buffered: string[] = [];
  const waiters: ((line: string | null) => void)[] = [];
  let ended = false;

  rl.on("line", (line) => {
    const waiter = waiters.shift();
    if (waiter !== undefined) waiter(line);
    else buffered.push(line);
  });
  rl.on("close", () => {
    ended = true;
    while (waiters.length > 0) waiters.shift()!(null);
  });

  return {
    next: () =>
      new Promise<string | null>((resolve) => {
        if (buffered.length > 0) resolve(buffered.shift()!);
        else if (ended) resolve(null);
        else waiters.push(resolve);
      }),
    close: () => rl.close(),
  };
}

function renderView(view: ProjectedState, doc: GameDocument): string {
  const lines: string[] = [];
  const phase = doc.spec.turn.phases[view.phaseIndex]?.name ?? "?";
  lines.push(`── round ${view.round} · phase ${phase} · seat ${view.activeSeat} to act ──`);
  for (const zone of view.zones) {
    const label = zone.owner === "seat" ? `${zone.zone}#${zone.ownerSeat}` : zone.zone;
    const mine = zone.ownerSeat === view.viewerSeat ? " (you)" : "";
    const contents = zone.entries
      .map((e) => ("hidden" in e ? "🂠" : describePiece(e.decl, e.properties)))
      .join(" ");
    lines.push(`  ${label}${mine}: [${zone.count}] ${contents}`);
  }
  return lines.join("\n");
}

function describePiece(decl: string, props: Readonly<Record<string, string | number>>): string {
  const entries = Object.entries(props);
  if (entries.length === 0) return decl;
  return entries.map(([, v]) => String(v)).join("/");
}

function describeMove(move: PlayerMove, state: GameState): string {
  if (move.target === undefined) return move.action;
  const piece = state.pieces[move.target];
  const detail = piece !== undefined && piece.faceUp ? ` (${describePiece(piece.decl, piece.properties)})` : "";
  return `${move.action} → ${move.target}${detail}`;
}

export async function runPlay(doc: GameDocument, options: PlayOptions): Promise<number> {
  const input = createLineReader();
  const botRng = createRng(`${options.seed}:bot`);
  const setupRng = createRng(`${options.seed}:setup`);

  let state = buildInitialState(doc, options.seats, setupRng).state;
  stdout.write(`\n🎴  ${doc.spec.meta.title} — you are seat ${options.seat}\n`);

  try {
    let guard = 0;
    while (state.status === "running" && guard++ < 100_000) {
      const legal = legalMoves(state, doc.spec);

      if (legal.length === 0) {
        state = applySkip(state, doc.spec).state;
        continue;
      }

      if (state.activeSeat !== options.seat) {
        // Bot turn.
        const move = randomChooser(legal, botRng);
        const result = applyAction(state, doc.spec, { seat: state.activeSeat, ...move });
        if (!result.ok) throw new Error(result.diagnostics[0]?.message);
        state = result.data.state;
        continue;
      }

      // Human turn.
      stdout.write(`\n${renderView(projectState(state, doc.spec, options.seat), doc)}\n`);
      legal.forEach((move, i) => stdout.write(`  [${i}] ${describeMove(move, state)}\n`));
      stdout.write("your move (number, or q to quit) > ");
      const line = await input.next();
      if (line === null) {
        stdout.write("\n(end of input)\n");
        return 0;
      }
      const answer = line.trim();
      if (answer === "q") {
        stdout.write("game abandoned.\n");
        return 0;
      }
      const choice = Number(answer);
      if (answer === "" || !Number.isInteger(choice) || choice < 0 || choice >= legal.length) {
        stdout.write("  invalid choice — try again.\n");
        continue;
      }
      const result = applyAction(state, doc.spec, { seat: options.seat, ...legal[choice]! });
      if (!result.ok) {
        stdout.write(`  ✖ ${result.diagnostics[0]?.message}\n`);
        continue;
      }
      state = result.data.state;
    }

    const finalView = projectState(state, doc.spec, options.seat);
    stdout.write(`\n${renderView(finalView, doc)}\n`);
    if (state.winnerSeat === null) stdout.write("\n🤝  the game is a draw.\n");
    else if (state.winnerSeat === options.seat) stdout.write("\n🏆  you win!\n");
    else stdout.write(`\n💀  seat ${state.winnerSeat} wins.\n`);
    return 0;
  } finally {
    input.close();
  }
}
