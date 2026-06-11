import type { Expr, GameSpec } from "@junction/spec";
import { zoneKey, type GameState, type PieceInstance } from "../model/state.js";

/**
 * Evaluates the total expression language against (state, spec, context).
 * Path vocabulary (closed, mirrors the validate-time lint):
 *   zones.<zone>.count | faceUpCount | totalCount | allEmpty | anyEmpty
 *   seats.count · turn.round | turn.seatIndex
 *   vars.<global> · seat.<perSeat> (actor) · seatVars.<perSeat>.min|max|sum
 *   this.<intProp> (trigger piece) · target.<intProp> (chosen piece)
 */

export class EvalError extends Error {}

type Value = number | boolean;

/** Optional evaluation context: which seat acts, which pieces are in scope. */
export interface EvalContext {
  readonly actorSeat?: number;
  readonly eventPiece?: PieceInstance;
  readonly targetPiece?: PieceInstance;
}

export function evaluate(expr: Expr, state: GameState, spec: GameSpec, ctx: EvalContext = {}): Value {
  switch (expr.t) {
    case "num":
      return expr.v;
    case "bool":
      return expr.v;
    case "path":
      return resolvePath(expr.segs, state, spec, ctx);
    case "un": {
      const v = evaluate(expr.e, state, spec, ctx);
      if (expr.op === "!") {
        if (typeof v !== "boolean") throw new EvalError(`'!' expects a boolean`);
        return !v;
      }
      if (typeof v !== "number") throw new EvalError(`unary '-' expects a number`);
      return -v;
    }
    case "bin": {
      const op = expr.op;
      if (op === "&&" || op === "||") {
        const l = evaluate(expr.l, state, spec, ctx);
        if (typeof l !== "boolean") throw new EvalError(`'${op}' expects booleans`);
        if (op === "&&" && !l) return false;
        if (op === "||" && l) return true;
        const r = evaluate(expr.r, state, spec, ctx);
        if (typeof r !== "boolean") throw new EvalError(`'${op}' expects booleans`);
        return r;
      }
      const l = evaluate(expr.l, state, spec, ctx);
      const r = evaluate(expr.r, state, spec, ctx);
      if (op === "==") return l === r;
      if (op === "!=") return l !== r;
      if (typeof l !== "number" || typeof r !== "number")
        throw new EvalError(`'${op}' expects numbers`);
      switch (op) {
        case ">=":
          return l >= r;
        case "<=":
          return l <= r;
        case ">":
          return l > r;
        case "<":
          return l < r;
        case "+":
          return l + r;
        case "-":
          return l - r;
        case "*":
          return l * r;
        case "/":
          return r === 0 ? 0 : Math.trunc(l / r);
      }
    }
  }
}

export function evaluateBoolean(expr: Expr, state: GameState, spec: GameSpec, ctx: EvalContext = {}): boolean {
  const v = evaluate(expr, state, spec, ctx);
  if (typeof v !== "boolean") throw new EvalError("expression must evaluate to a boolean");
  return v;
}

export function evaluateNumber(expr: Expr, state: GameState, spec: GameSpec, ctx: EvalContext = {}): number {
  const v = evaluate(expr, state, spec, ctx);
  if (typeof v !== "number") throw new EvalError("expression must evaluate to a number");
  return v;
}

function pieceInt(piece: PieceInstance | undefined, prop: string, root: string): number {
  if (piece === undefined) throw new EvalError(`'${root}' is not available here`);
  const value = piece.properties[prop];
  if (typeof value !== "number") throw new EvalError(`${root}.${prop} is not an int property`);
  return value;
}

function resolvePath(segs: readonly string[], state: GameState, spec: GameSpec, ctx: EvalContext): Value {
  if (segs[0] === "vars") {
    const value = state.vars[segs[1] ?? ""];
    if (value === undefined) throw new EvalError(`unknown global variable '${segs[1]}'`);
    return value;
  }
  if (segs[0] === "seat") {
    const values = state.seatVars[segs[1] ?? ""];
    if (values === undefined) throw new EvalError(`unknown perSeat variable '${segs[1]}'`);
    const seat = ctx.actorSeat ?? state.activeSeat;
    return values[seat] ?? 0;
  }
  if (segs[0] === "seatVars") {
    const values = state.seatVars[segs[1] ?? ""];
    if (values === undefined) throw new EvalError(`unknown perSeat variable '${segs[1]}'`);
    const field = segs[2];
    if (field === "min") return Math.min(...values);
    if (field === "max") return Math.max(...values);
    if (field === "sum") return values.reduce((a, b) => a + b, 0);
    throw new EvalError(`unknown seatVars aggregate '${field}'`);
  }
  if (segs[0] === "this") return pieceInt(ctx.eventPiece, segs[1] ?? "", "this");
  if (segs[0] === "target") return pieceInt(ctx.targetPiece, segs[1] ?? "", "target");
  if (segs[0] === "seats" && segs[1] === "count") return state.seats;
  if (segs[0] === "turn" && segs[1] === "round") return state.round;
  if (segs[0] === "turn" && segs[1] === "seatIndex") return state.activeSeat;
  if (segs[0] === "zones") {
    const zoneName = segs[1] ?? "";
    const field = segs[2] ?? "";
    const decl = spec.zones.find((z) => z.name === zoneName);
    if (decl === undefined) throw new EvalError(`unknown zone '${zoneName}'`);
    const keys =
      decl.owner === "shared"
        ? [zoneKey(zoneName, null)]
        : Array.from({ length: state.seats }, (_, seat) => zoneKey(zoneName, seat));
    const counts = keys.map((k) => state.zones[k]?.length ?? 0);
    switch (field) {
      case "count":
        if (decl.owner !== "shared") throw new EvalError(`'count' is shared-zone-only`);
        return counts[0]!;
      case "faceUpCount": {
        if (decl.owner !== "shared") throw new EvalError(`'faceUpCount' is shared-zone-only`);
        const entries = state.zones[keys[0]!] ?? [];
        return entries.filter((e) => state.pieces[e.pieceId]?.faceUp === true).length;
      }
      case "totalCount":
        return counts.reduce((a, b) => a + b, 0);
      case "allEmpty":
        return counts.every((c) => c === 0);
      case "anyEmpty":
        return counts.some((c) => c === 0);
      default:
        throw new EvalError(`unknown zone field '${field}'`);
    }
  }
  throw new EvalError(`unknown path '${segs.join(".")}'`);
}
