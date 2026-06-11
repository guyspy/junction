import type { Expr, GameSpec } from "@junction/spec";
import { zoneKey, type GameState } from "../model/state.js";

/**
 * Evaluates the total expression language against (state, spec).
 * Path vocabulary (closed, mirrors the validate-time lint):
 *   zones.<zone>.count | totalCount | allEmpty | anyEmpty
 *   seats.count
 *   turn.round | turn.seatIndex
 */

export class EvalError extends Error {}

type Value = number | boolean;

export function evaluate(expr: Expr, state: GameState, spec: GameSpec): Value {
  switch (expr.t) {
    case "num":
      return expr.v;
    case "bool":
      return expr.v;
    case "path":
      return resolvePath(expr.segs, state, spec);
    case "un": {
      const v = evaluate(expr.e, state, spec);
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
        const l = evaluate(expr.l, state, spec);
        if (typeof l !== "boolean") throw new EvalError(`'${op}' expects booleans`);
        if (op === "&&" && !l) return false;
        if (op === "||" && l) return true;
        const r = evaluate(expr.r, state, spec);
        if (typeof r !== "boolean") throw new EvalError(`'${op}' expects booleans`);
        return r;
      }
      const l = evaluate(expr.l, state, spec);
      const r = evaluate(expr.r, state, spec);
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

export function evaluateBoolean(expr: Expr, state: GameState, spec: GameSpec): boolean {
  const v = evaluate(expr, state, spec);
  if (typeof v !== "boolean") throw new EvalError("expression must evaluate to a boolean");
  return v;
}

function resolvePath(segs: readonly string[], state: GameState, spec: GameSpec): Value {
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
