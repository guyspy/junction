/**
 * The diagnostic kernel — one shape for every feedback loop (family pattern: mantle ADR-0008).
 * Messages are generated from structured fields by the single formatter below; never hand-write them.
 */

export type DiagnosticPhase = "validate" | "test" | "boot" | "runtime";
export type DiagnosticSeverity = "error" | "warning";

export interface Diagnostic {
  readonly code: DiagnosticCode;
  readonly phase: DiagnosticPhase;
  readonly severity: DiagnosticSeverity;
  /** Locus: `<file>#/yaml/pointer`, `manifest:Kind/name#/ptr`, or a runtime locus like `action:play-card`. */
  readonly path: string;
  readonly value?: unknown;
  readonly expected?: string;
  /** Only when the expected set is finite and known. Stripped from runtime wire responses. */
  readonly candidates?: readonly string[];
  /** Only for high-confidence fixes (typo distance ≤ 2). */
  readonly suggestion?: string;
  readonly message: string;
}

export const DiagnosticCodes = {
  // validate
  INVALID_YAML: "INVALID_YAML",
  INVALID_MANIFEST_ENVELOPE: "INVALID_MANIFEST_ENVELOPE",
  UNSUPPORTED_API_VERSION: "UNSUPPORTED_API_VERSION",
  UNSUPPORTED_KIND: "UNSUPPORTED_KIND",
  SCHEMA_VALIDATION_FAILED: "SCHEMA_VALIDATION_FAILED",
  DUPLICATE_NAME: "DUPLICATE_NAME",
  ZONE_REF_UNKNOWN: "ZONE_REF_UNKNOWN",
  PIECE_REF_UNKNOWN: "PIECE_REF_UNKNOWN",
  ACTION_REF_UNKNOWN: "ACTION_REF_UNKNOWN",
  VAR_REF_UNKNOWN: "VAR_REF_UNKNOWN",
  EXPRESSION_SYNTAX_ERROR: "EXPRESSION_SYNTAX_ERROR",
  EXPRESSION_REF_INVALID: "EXPRESSION_REF_INVALID",
  // runtime
  ACTION_NOT_LEGAL: "ACTION_NOT_LEGAL",
  SEAT_NOT_ACTIVE: "SEAT_NOT_ACTIVE",
  GAME_ALREADY_ENDED: "GAME_ALREADY_ENDED",
  INTERNAL_ERROR: "INTERNAL_ERROR",
} as const;

export type DiagnosticCode = (typeof DiagnosticCodes)[keyof typeof DiagnosticCodes];

export interface DiagnosticInput {
  readonly code: DiagnosticCode;
  readonly phase: DiagnosticPhase;
  readonly severity: DiagnosticSeverity;
  readonly path: string;
  readonly value?: unknown;
  readonly expected?: string;
  readonly candidates?: readonly string[];
  readonly suggestion?: string;
}

function describeValue(value: unknown): string {
  if (value === undefined) return "<missing>";
  if (typeof value === "string") return `'${value}'`;
  return JSON.stringify(value);
}

/** The single message formatter. */
function composeMessage(d: DiagnosticInput): string {
  const parts: string[] = [];
  parts.push(`${d.path}: ${humanPhrase(d.code)}`);
  if (d.value !== undefined) parts.push(`got ${describeValue(d.value)}`);
  if (d.expected !== undefined) parts.push(`expected ${d.expected}`);
  let message = parts.join(" — ");
  if (d.suggestion !== undefined) message += `. Did you mean '${d.suggestion}'?`;
  else if (d.candidates !== undefined && d.candidates.length > 0)
    message += `. Known: [${d.candidates.join(", ")}]`;
  return message;
}

function humanPhrase(code: DiagnosticCode): string {
  const phrases: Record<DiagnosticCode, string> = {
    INVALID_YAML: "the document is not valid YAML",
    INVALID_MANIFEST_ENVELOPE: "the manifest envelope is invalid",
    UNSUPPORTED_API_VERSION: "unsupported apiVersion",
    UNSUPPORTED_KIND: "unsupported kind",
    SCHEMA_VALIDATION_FAILED: "the spec does not match the grammar",
    DUPLICATE_NAME: "this name is declared more than once",
    ZONE_REF_UNKNOWN: "reference to an undeclared zone",
    PIECE_REF_UNKNOWN: "reference to an undeclared piece set",
    ACTION_REF_UNKNOWN: "reference to an undeclared action",
    VAR_REF_UNKNOWN: "reference to an undeclared variable",
    EXPRESSION_SYNTAX_ERROR: "the expression has a syntax error",
    EXPRESSION_REF_INVALID: "the expression references an invalid path",
    ACTION_NOT_LEGAL: "the action is not legal in the current state",
    SEAT_NOT_ACTIVE: "it is not this seat's turn",
    GAME_ALREADY_ENDED: "the game has already ended",
    INTERNAL_ERROR: "internal engine error",
  };
  return phrases[code];
}

export function makeDiagnostic(input: DiagnosticInput): Diagnostic {
  return { ...input, message: composeMessage(input) };
}

export function validateError(input: Omit<DiagnosticInput, "phase" | "severity">): Diagnostic {
  return makeDiagnostic({ ...input, phase: "validate", severity: "error" });
}

export function validateWarning(input: Omit<DiagnosticInput, "phase" | "severity">): Diagnostic {
  return makeDiagnostic({ ...input, phase: "validate", severity: "warning" });
}

export function runtimeError(input: Omit<DiagnosticInput, "phase" | "severity">): Diagnostic {
  // Security rule: candidates never leave the runtime on the wire.
  const { candidates: _candidates, ...rest } = input;
  return makeDiagnostic({ ...rest, phase: "runtime", severity: "error" });
}

export function hasErrors(diagnostics: readonly Diagnostic[]): boolean {
  return diagnostics.some((d) => d.severity === "error");
}

/** One line per diagnostic, for `--format text`. */
export function formatDiagnosticText(d: Diagnostic): string {
  const badge = d.severity === "error" ? "✖" : "⚠";
  return `${badge} [${d.code}] ${d.message}`;
}

/** Edit-distance helper for `suggestion` (≤ 2 by house rule). */
export function suggestFrom(value: string, candidates: readonly string[]): string | undefined {
  let best: { c: string; d: number } | undefined;
  for (const c of candidates) {
    const d = editDistance(value, c);
    if (best === undefined || d < best.d) best = { c, d };
  }
  return best !== undefined && best.d <= 2 ? best.c : undefined;
}

function editDistance(a: string, b: string): number {
  const m = a.length;
  const n = b.length;
  const row: number[] = Array.from({ length: n + 1 }, (_, j) => j);
  for (let i = 1; i <= m; i++) {
    let prev = row[0]!;
    row[0] = i;
    for (let j = 1; j <= n; j++) {
      const tmp = row[j]!;
      row[j] = Math.min(row[j]! + 1, row[j - 1]! + 1, prev + (a[i - 1] === b[j - 1] ? 0 : 1));
      prev = tmp;
    }
  }
  return row[n]!;
}
