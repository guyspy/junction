import {
  DiagnosticCodes,
  err,
  ok,
  parseExpression,
  runtimeError,
  type EffectDecl,
  type Expr,
  type GameSpec,
  type Result,
  type ZoneSel,
} from "@junction/spec";
import type { GameEvent, GameEventInit, PieceView } from "../model/events.js";
import { zoneKey, type GameState, type ZoneEntry } from "../model/state.js";
import { evaluateBoolean } from "./expression-evaluator.js";

/**
 * The pure reducer: (state, action) → { state', events[] }.
 * Never mutates its input; all randomness happened at setup (v1alpha has no dice yet).
 * Trigger conditions evaluate against the NEW state — Session 3's hard-won lesson.
 */

export interface PlayerAction {
  readonly seat: number;
  readonly action: string;
}

export interface StepResult {
  readonly state: GameState;
  readonly events: readonly GameEvent[];
}

const MAX_TRIGGER_CASCADE = 100;

interface Draft {
  status: GameState["status"];
  seats: number;
  round: number;
  activeSeat: number;
  phaseIndex: number;
  zones: Record<string, ZoneEntry[]>;
  pieces: GameState["pieces"];
  winnerSeat: number | null;
  seq: number;
  consecutiveSkips: number;
}

function toDraft(state: GameState): Draft {
  return {
    ...state,
    zones: Object.fromEntries(Object.entries(state.zones).map(([k, v]) => [k, [...v]])),
  };
}

function freeze(draft: Draft): GameState {
  return draft;
}

/** Cache parsed expressions per source string (specs are immutable). */
const exprCache = new Map<string, Expr>();
function expr(src: string): Expr {
  let cached = exprCache.get(src);
  if (cached === undefined) {
    const parsed = parseExpression(src);
    if (!parsed.ok) throw new Error(`unvalidated expression reached the reducer: ${src}`);
    cached = parsed.expr;
    exprCache.set(src, cached);
  }
  return cached;
}

function selKey(sel: ZoneSel, spec: GameSpec, actorSeat: number): string {
  const decl = spec.zones.find((z) => z.name === sel.zone)!;
  return decl.owner === "shared" ? zoneKey(sel.zone, null) : zoneKey(sel.zone, actorSeat);
}

function baseZoneName(key: string): string {
  const hash = key.indexOf("#");
  return hash === -1 ? key : key.slice(0, hash);
}

export function legalActions(state: GameState, spec: GameSpec): readonly string[] {
  if (state.status !== "running") return [];
  const phase = spec.turn.phases[state.phaseIndex]!;
  return phase.actions.filter((name) => {
    const action = spec.actions.find((a) => a.name === name)!;
    const fromKey = selKey(action.move.from, spec, state.activeSeat);
    if ((state.zones[fromKey]?.length ?? 0) === 0) return false;
    if (action.requires !== undefined && !evaluateBoolean(expr(action.requires), state, spec))
      return false;
    return true;
  });
}

export function applyAction(
  state: GameState,
  spec: GameSpec,
  playerAction: PlayerAction,
): Result<StepResult> {
  if (state.status === "ended")
    return err([
      runtimeError({ code: DiagnosticCodes.GAME_ALREADY_ENDED, path: `action:${playerAction.action}` }),
    ]);
  if (playerAction.seat !== state.activeSeat)
    return err([
      runtimeError({
        code: DiagnosticCodes.SEAT_NOT_ACTIVE,
        path: `action:${playerAction.action}`,
        value: playerAction.seat,
        expected: `seat ${state.activeSeat}`,
      }),
    ]);
  if (!legalActions(state, spec).includes(playerAction.action))
    return err([
      runtimeError({
        code: DiagnosticCodes.ACTION_NOT_LEGAL,
        path: `action:${playerAction.action}`,
        value: playerAction.action,
        expected: "a legal action for the current phase and state",
      }),
    ]);

  const draft = toDraft(state);
  const events: GameEvent[] = [];
  const emit = (e: GameEventInit): GameEvent => {
    const event = { ...e, seq: draft.seq++ } as GameEvent;
    events.push(event);
    return event;
  };

  const actor = playerAction.seat;
  emit({ type: "actionTaken", seat: actor, action: playerAction.action });

  const action = spec.actions.find((a) => a.name === playerAction.action)!;
  const moved = movePieces(draft, spec, {
    fromKey: selKey(action.move.from, spec, actor),
    toKey: selKey(action.move.to, spec, actor),
    count: 1,
    bySeat: actor,
    reveal: action.move.reveal,
    emit,
  });

  runTriggerCascade(draft, spec, moved, actor, emit);
  draft.consecutiveSkips = 0;
  settleAndAdvance(draft, spec, emit);
  return ok({ state: freeze(draft), events });
}

/** When the active seat has no legal action, the phase is skipped (the stall guard). */
export function applySkip(state: GameState, spec: GameSpec): StepResult {
  const draft = toDraft(state);
  const events: GameEvent[] = [];
  const emit = (e: GameEventInit): GameEvent => {
    const event = { ...e, seq: draft.seq++ } as GameEvent;
    events.push(event);
    return event;
  };
  const phase = spec.turn.phases[draft.phaseIndex]!;
  emit({ type: "turnSkipped", seat: draft.activeSeat, phase: phase.name });
  draft.consecutiveSkips++;
  const skipBudget = draft.seats * spec.turn.phases.length;
  if (draft.consecutiveSkips >= skipBudget) {
    endGame(draft, spec, "stalled", emit);
    return { state: freeze(draft), events };
  }
  settleAndAdvance(draft, spec, emit);
  return { state: freeze(draft), events };
}

// ---- internals --------------------------------------------------------------

interface MoveArgs {
  readonly fromKey: string;
  readonly toKey: string;
  readonly count: number;
  readonly bySeat: number | null;
  readonly reveal: boolean;
  readonly emit: (e: GameEventInit) => GameEvent;
}

function movePieces(draft: Draft, spec: GameSpec, args: MoveArgs): GameEvent[] {
  const movedEvents: GameEvent[] = [];
  for (let i = 0; i < args.count; i++) {
    const entry = draft.zones[args.fromKey]!.pop();
    if (entry === undefined) break;
    draft.zones[args.toKey]!.push({ pieceId: entry.pieceId, bySeat: args.bySeat });
    const revealed: PieceView | undefined = args.reveal
      ? { pieceId: entry.pieceId, ...pieceView(draft, entry.pieceId) }
      : undefined;
    movedEvents.push(
      args.emit({
        type: "pieceMoved",
        pieceId: entry.pieceId,
        from: args.fromKey,
        to: args.toKey,
        bySeat: args.bySeat,
        ...(revealed !== undefined ? { revealed } : {}),
      }),
    );
  }
  return movedEvents;
}

function pieceView(draft: Draft, pieceId: string): { decl: string; properties: Record<string, string | number> } {
  const piece = draft.pieces[pieceId]!;
  return { decl: piece.decl, properties: { ...piece.properties } };
}

function runTriggerCascade(
  draft: Draft,
  spec: GameSpec,
  initialMoves: readonly GameEvent[],
  actorSeat: number,
  emit: (e: GameEventInit) => GameEvent,
): void {
  let queue = [...initialMoves];
  let depth = 0;
  while (queue.length > 0) {
    if (++depth > MAX_TRIGGER_CASCADE)
      throw new Error(`trigger cascade exceeded ${MAX_TRIGGER_CASCADE} steps — non-total spec?`);
    const moveEvent = queue.shift()!;
    if (moveEvent.type !== "pieceMoved") continue;
    for (const trigger of spec.triggers) {
      if (trigger.on.event !== "pieceMoved") continue;
      if (
        trigger.on.intoZone !== undefined &&
        baseZoneName(moveEvent.to) !== trigger.on.intoZone
      )
        continue;
      // Conditions see the NEW state (Session 3's lesson).
      if (trigger.when !== undefined && !evaluateBoolean(expr(trigger.when), freeze(draft), spec))
        continue;
      emit({ type: "triggerFired", trigger: trigger.name });
      for (const effect of trigger.effects) {
        const produced = applyEffect(draft, spec, effect, actorSeat, emit);
        queue.push(...produced);
      }
    }
  }
}

function applyEffect(
  draft: Draft,
  spec: GameSpec,
  effect: EffectDecl,
  actorSeat: number,
  emit: (e: GameEventInit) => GameEvent,
): GameEvent[] {
  if ("moveAll" in effect) {
    const fromKey = selKey(effect.moveAll.from, spec, actorSeat);
    const toKey = selKey(effect.moveAll.to, spec, actorSeat);
    const count = draft.zones[fromKey]!.length;
    return movePieces(draft, spec, { fromKey, toKey, count, bySeat: actorSeat, reveal: false, emit });
  }

  const { zone, property, toWinnerZone } = effect.resolveHighest;
  const key = zoneKey(zone, null);
  const entries = draft.zones[key]!;
  // Newest entry per seat, scanning from the top.
  const newestPerSeat = new Map<number, string>();
  for (let i = entries.length - 1; i >= 0; i--) {
    const entry = entries[i]!;
    if (entry.bySeat === null || newestPerSeat.has(entry.bySeat)) continue;
    newestPerSeat.set(entry.bySeat, entry.pieceId);
  }
  if (newestPerSeat.size < 2) return [];

  let winnerSeat: number | null = null;
  let best = Number.NEGATIVE_INFINITY;
  let tied = false;
  for (const [seat, pieceId] of newestPerSeat) {
    const value = Number(draft.pieces[pieceId]!.properties[property] ?? Number.NEGATIVE_INFINITY);
    if (value > best) {
      best = value;
      winnerSeat = seat;
      tied = false;
    } else if (value === best) {
      tied = true;
    }
  }
  if (tied) winnerSeat = null;

  emit({ type: "zoneResolved", zone, property, winnerSeat });
  if (winnerSeat === null) return []; // onTie: stay — the pot carries.
  return movePieces(draft, spec, {
    fromKey: key,
    toKey: zoneKey(toWinnerZone, winnerSeat),
    count: entries.length,
    bySeat: winnerSeat,
    reveal: false,
    emit,
  });
}

function computeWinner(draft: Draft, spec: GameSpec): number | null {
  const zone = spec.end.winner.mostPiecesIn;
  let winner: number | null = null;
  let best = -1;
  let tied = false;
  for (let seat = 0; seat < draft.seats; seat++) {
    const count = draft.zones[zoneKey(zone, seat)]?.length ?? 0;
    if (count > best) {
      best = count;
      winner = seat;
      tied = false;
    } else if (count === best) {
      tied = true;
    }
  }
  return tied ? null : winner;
}

function endGame(
  draft: Draft,
  spec: GameSpec,
  reason: "endCondition" | "stalled",
  emit: (e: GameEventInit) => GameEvent,
): void {
  draft.status = "ended";
  draft.winnerSeat = computeWinner(draft, spec);
  emit({ type: "gameEnded", winnerSeat: draft.winnerSeat, reason });
}

function settleAndAdvance(
  draft: Draft,
  spec: GameSpec,
  emit: (e: GameEventInit) => GameEvent,
): void {
  if (evaluateBoolean(expr(spec.end.when), freeze(draft), spec)) {
    endGame(draft, spec, "endCondition", emit);
    return;
  }
  draft.phaseIndex++;
  if (draft.phaseIndex >= spec.turn.phases.length) {
    draft.phaseIndex = 0;
    draft.activeSeat = (draft.activeSeat + 1) % draft.seats;
    if (draft.activeSeat === 0) {
      draft.round++;
      emit({ type: "roundStarted", round: draft.round });
    }
    emit({ type: "turnStarted", seat: draft.activeSeat, round: draft.round });
  }
}
