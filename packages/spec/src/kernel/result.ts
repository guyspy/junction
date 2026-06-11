import type { Diagnostic } from "./diagnostic.js";

/**
 * Result objects, not exceptions, at package boundaries (family pattern).
 * Tests assert on `diagnostic.code`, never on exception types.
 */
export type Result<T> =
  | { readonly ok: true; readonly data: T; readonly warnings: readonly Diagnostic[] }
  | { readonly ok: false; readonly diagnostics: readonly Diagnostic[] };

export function ok<T>(data: T, warnings: readonly Diagnostic[] = []): Result<T> {
  return { ok: true, data, warnings };
}

export function err<T>(diagnostics: readonly Diagnostic[]): Result<T> {
  return { ok: false, diagnostics };
}
