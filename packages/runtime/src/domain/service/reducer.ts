import {
  DiagnosticCodes,
  err,
  ok,
  parseExpression,
  runtimeError,
  type ActionDecl,
  type EffectDecl,
  type Expr,
  type GameSpec,
  type Result,
  type ZoneSel,
} from "@junction/spec";
import type { GameEvent, GameEventInit, PieceView } from "../model/events.js";
import { zoneKey, type GameState, type PieceInstance, type ZoneEntry } from "../model/state.js";
import { evaluateBoolean } from "./expression-evaluator.js";

/**
 * The pure reducer: (state, action) → { state', events[] }.
 * Never mutates its input; all randomness happened at setup (v1alpha has no dice yet).
 * Trigger conditions evaluate against the NEW state — Session 3's hard-won lesson.
 */

/** A concrete move: an action plus its chosen target piece, when the action takes one. */
export interface PlayerMove {
  readonly action: string;
  readonly target?: string;
}

export interface PlayerAction extends PlayerMove {
  readonly seat: number;
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
  pieces: Record<string, PieceInstance>;
  winnerSeat: number | null;
  seq: number;
  consecutiveSkips: number;
  pendingGoAgain: boolean;
}

function toDraft(state: GameState): Draft {
  return {
    ...state,
    zones: Object.fromEntries(Object.entries(state.zones).map(([k, v]) => [k, [...v]])),
    pieces: { ...state.pieces },
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

/** Enumerate every concrete legal move for the active seat in the current phase. */
export function legalMoves(state: GameState, spec: GameSpec): readonly PlayerMove[] {
  if (state.status !== "running") return [];
  const phase = spec.turn.phases[state.phaseIndex]!;
  const moves: PlayerMove[] = [];
  for (const name of phase.actions) {
    const action = spec.actions.find((a) => a.name === name)!;
    if (action.requires !== undefined && !evaluateBoolean(expr(action.requires), state, spec))
      continue;
    if (action.move !== undefined) {
      const fromKey = selKey(action.move.from, spec, state.activeSeat);
      const entries = state.zones[fromKey] ?? [];
      if (entries.length === 0) continue;
      if (action.move.take === "top") moves.push({ action: name });
      else for (const entry of entries) moves.push({ action: name, target: entry.pieceId });
    } else if (action.flip !== undefined) {
      const key = selKey(action.flip.zone, spec, state.activeSeat);
      for (const entry of state.zones[key] ?? [])
        if (state.pieces[entry.pieceId]!.faceUp === false)
          moves.push({ action: name, target: entry.pieceId });
    }
  }
  return moves;
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
  const isLegal = legalMoves(state, spec).some(
    (m) => m.action === playerAction.action && m.target === playerAction.target,
  );
  if (!isLegal)
    return err([
      runtimeError({
        code: DiagnosticCodes.ACTION_NOT_LEGAL,
        path: `action:${playerAction.action}`,
        value: playerAction.target === undefined ? playerAction.action : `${playerAction.action}→${playerAction.target}`,
        expected: "a legal move for the current phase and state",
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
  const produced = executeAction(draft, spec, action, playerAction, emit);

  runTriggerCascade(draft, spec, produced, actor, emit);
  draft.consecutiveSkips = 0;
  settleAndAdvance(draft, spec, emit);
  return ok({ state: freeze(draft), events });
}

function executeAction(
  draft: Draft,
  spec: GameSpec,
  action: ActionDecl,
  playerAction: PlayerAction,
  emit: (e: GameEventInit) => GameEvent,
): GameEvent[] {
  const actor = playerAction.seat;
  if (action.move !== undefined) {
    const fromKey = selKey(action.move.from, spec, actor);
    const toKey = selKey(action.move.to, spec, actor);
    if (action.move.take === "top")
      return moveTopPieces(draft, { fromKey, toKey, count: 1, bySeat: actor, reveal: action.move.reveal, emit });
    return moveSpecificPieces(draft, {
      fromKey,
      toKey,
      pieceIds: [playerAction.target!],
      bySeat: actor,
      reveal: action.move.reveal,
      emit,
    });
  }
  // flip
  const key = selKey(action.flip!.zone, spec, actor);
  return [setPieceFace(draft, playerAction.target!, baseZoneName(key), true, actor, emit)];
}

/** When the active seat has no legal move, the phase is skipped (the stall guard). */
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

interface TopMoveArgs {
  readonly fromKey: string;
  readonly toKey: string;
  readonly count: number;
  readonly bySeat: number | null;
  readonly reveal: boolean;
  readonly emit: (e: GameEventInit) => GameEvent;
}

function moveTopPieces(draft: Draft, args: TopMoveArgs): GameEvent[] {
  const moved: GameEvent[] = [];
  for (let i = 0; i < args.count; i++) {
    const entry = draft.zones[args.fromKey]!.pop();
    if (entry === undefined) break;
    moved.push(placePiece(draft, entry.pieceId, args.fromKey, args.toKey, args.bySeat, args.reveal, args.emit));
  }
  return moved;
}

interface SpecificMoveArgs {
  readonly fromKey: string;
  readonly toKey: string;
  readonly pieceIds: readonly string[];
  readonly bySeat: number | null;
  readonly reveal: boolean;
  readonly emit: (e: GameEventInit) => GameEvent;
}

function moveSpecificPieces(draft: Draft, args: SpecificMoveArgs): GameEvent[] {
  const moved: GameEvent[] = [];
  for (const pieceId of args.pieceIds) {
    const source = draft.zones[args.fromKey]!;
    const index = source.findIndex((e) => e.pieceId === pieceId);
    if (index === -1) continue;
    source.splice(index, 1);
    moved.push(placePiece(draft, pieceId, args.fromKey, args.toKey, args.bySeat, args.reveal, args.emit));
  }
  return moved;
}

function placePiece(
  draft: Draft,
  pieceId: string,
  fromKey: string,
  toKey: string,
  bySeat: number | null,
  reveal: boolean,
  emit: (e: GameEventInit) => GameEvent,
): GameEvent {
  draft.zones[toKey]!.push({ pieceId, bySeat });
  if (reveal && draft.pieces[pieceId]!.faceUp === false)
    draft.pieces[pieceId] = { ...draft.pieces[pieceId]!, faceUp: true };
  const revealed: PieceView | undefined = reveal ? pieceView(draft, pieceId) : undefined;
  return emit({
    type: "pieceMoved",
    pieceId,
    from: fromKey,
    to: toKey,
    bySeat,
    ...(revealed !== undefined ? { revealed } : {}),
  });
}

function setPieceFace(
  draft: Draft,
  pieceId: string,
  zone: string,
  faceUp: boolean,
  bySeat: number | null,
  emit: (e: GameEventInit) => GameEvent,
): GameEvent {
  draft.pieces[pieceId] = { ...draft.pieces[pieceId]!, faceUp };
  return emit({
    type: "pieceFlipped",
    pieceId,
    zone,
    faceUp,
    bySeat,
    ...(faceUp ? { revealed: pieceView(draft, pieceId) } : {}),
  });
}

function pieceView(draft: Draft, pieceId: string): PieceView {
  const piece = draft.pieces[pieceId]!;
  return { pieceId, decl: piece.decl, properties: { ...piece.properties } };
}

function runTriggerCascade(
  draft: Draft,
  spec: GameSpec,
  initialEvents: readonly GameEvent[],
  actorSeat: number,
  emit: (e: GameEventInit) => GameEvent,
): void {
  const queue = [...initialEvents];
  let depth = 0;
  while (queue.length > 0) {
    if (++depth > MAX_TRIGGER_CASCADE)
      throw new Error(`trigger cascade exceeded ${MAX_TRIGGER_CASCADE} steps — non-total spec?`);
    const event = queue.shift()!;
    for (const trigger of spec.triggers) {
      if (event.type !== trigger.on.event) continue;
      if (
        trigger.on.intoZone !== undefined &&
        !(event.type === "pieceMoved" && baseZoneName(event.to) === trigger.on.intoZone)
      )
        continue;
      if (
        trigger.on.inZone !== undefined &&
        !(event.type === "pieceFlipped" && event.zone === trigger.on.inZone)
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
    return moveTopPieces(draft, { fromKey, toKey, count, bySeat: actorSeat, reveal: false, emit });
  }

  if ("resolveHighest" in effect) {
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
    return moveTopPieces(draft, {
      fromKey: key,
      toKey: zoneKey(toWinnerZone, winnerSeat),
      count: entries.length,
      bySeat: winnerSeat,
      reveal: false,
      emit,
    });
  }

  // resolveEqualPair
  const { zone, property, toZone, onMatch } = effect.resolveEqualPair;
  const key = zoneKey(zone, null);
  const faceUpIds = (draft.zones[key] ?? [])
    .map((e) => e.pieceId)
    .filter((id) => draft.pieces[id]!.faceUp === true);
  if (faceUpIds.length !== 2) return [];

  const [a, b] = faceUpIds as [string, string];
  const matched = draft.pieces[a]!.properties[property] === draft.pieces[b]!.properties[property];
  emit({ type: "pairResolved", zone, property, matched, bySeat: actorSeat });

  if (matched) {
    if (onMatch === "goAgain") draft.pendingGoAgain = true;
    return moveSpecificPieces(draft, {
      fromKey: key,
      toKey: zoneKey(toZone, actorSeat),
      pieceIds: [a, b],
      bySeat: actorSeat,
      reveal: false,
      emit,
    });
  }
  // onMismatch: flipDown
  return [
    setPieceFace(draft, a, zone, false, actorSeat, emit),
    setPieceFace(draft, b, zone, false, actorSeat, emit),
  ];
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
  if (draft.pendingGoAgain) {
    // A matched pair (or future goAgain effects): same seat restarts its turn.
    draft.pendingGoAgain = false;
    draft.phaseIndex = 0;
    emit({ type: "turnStarted", seat: draft.activeSeat, round: draft.round });
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
