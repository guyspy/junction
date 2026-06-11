/**
 * The expression language is tiny and TOTAL: comparisons, arithmetic, boolean logic over
 * typed context paths. No loops, no recursion, no side effects — guaranteed termination
 * (blueprint §5; ports the Kotlin ConditionEvaluator's discipline).
 */

export type Expr =
  | { readonly t: "num"; readonly v: number }
  | { readonly t: "bool"; readonly v: boolean }
  | { readonly t: "path"; readonly segs: readonly string[] }
  | { readonly t: "un"; readonly op: "!" | "-"; readonly e: Expr }
  | { readonly t: "bin"; readonly op: BinOp; readonly l: Expr; readonly r: Expr };

export type BinOp =
  | "||"
  | "&&"
  | "=="
  | "!="
  | ">="
  | "<="
  | ">"
  | "<"
  | "+"
  | "-"
  | "*"
  | "/";

/** Collect every path the expression touches (for validate-time reference lints). */
export function collectPaths(e: Expr): readonly string[][] {
  switch (e.t) {
    case "num":
    case "bool":
      return [];
    case "path":
      return [[...e.segs]];
    case "un":
      return collectPaths(e.e);
    case "bin":
      return [...collectPaths(e.l), ...collectPaths(e.r)];
  }
}
