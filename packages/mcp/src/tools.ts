import { parseGameDocument, type Diagnostic } from "@junction/spec";
import { simulate, type SimulateReport } from "@junction/runtime";
import { describeGrammar, type GrammarReference } from "./grammar-reference.js";
import { scaffoldGame, type ScaffoldInput } from "./scaffold.js";

/**
 * Pure tool implementations — no transport, no I/O, fully unit-testable. The server
 * wraps these for MCP; a Workers adapter could wrap the same functions. Every result
 * carries a short human/agent-readable `summary` plus bounded `structured` data
 * (agent-safe receipts: no raw dumps, no host paths).
 */

export interface ToolResult<T> {
  readonly ok: boolean;
  readonly summary: string;
  readonly structured: T;
}

export interface ReferenceGame {
  readonly name: string;
  readonly title: string;
  readonly description: string;
  readonly yaml: string;
}

export interface ValidateOutput {
  readonly ok: boolean;
  readonly game?: string;
  readonly diagnostics: readonly Diagnostic[];
}

export function runValidate(yaml: string): ToolResult<ValidateOutput> {
  const parsed = parseGameDocument(yaml, { file: "<draft>" });
  if (!parsed.ok) {
    const errs = parsed.diagnostics;
    const head = errs.slice(0, 5).map((d) => `• ${d.message}`).join("\n");
    const more = errs.length > 5 ? `\n…and ${errs.length - 5} more` : "";
    return {
      ok: false,
      summary: `Invalid: ${errs.length} error(s).\n${head}${more}`,
      structured: { ok: false, diagnostics: errs },
    };
  }
  const warnings = parsed.warnings;
  const summary =
    warnings.length === 0
      ? `Valid Game '${parsed.data.metadata.name}'.`
      : `Valid Game '${parsed.data.metadata.name}' with ${warnings.length} warning(s).`;
  return { ok: true, summary, structured: { ok: true, game: parsed.data.metadata.name, diagnostics: warnings } };
}

export interface SimulateInput {
  readonly yaml: string;
  readonly games?: number;
  readonly seats?: number;
  readonly seed?: string;
}

export interface SimulateOutput {
  readonly ok: boolean;
  readonly diagnostics?: readonly Diagnostic[];
  readonly report?: SimulateReport;
}

export function runSimulate(input: SimulateInput): ToolResult<SimulateOutput> {
  const parsed = parseGameDocument(input.yaml, { file: "<draft>" });
  if (!parsed.ok)
    return {
      ok: false,
      summary: `Cannot simulate — the game is invalid. Run validate_game first.`,
      structured: { ok: false, diagnostics: parsed.diagnostics },
    };

  const report = simulate(parsed.data, {
    games: clamp(input.games ?? 200, 1, 5000),
    seats: input.seats,
    seed: input.seed ?? "integrin",
  });
  const summary = [
    `Simulated ${report.games} games of '${report.game}' (${report.seats} seats):`,
    ...report.notes.map((n) => `  ${n}`),
    `  turns: p50 ${report.turns.p50}, p95 ${report.turns.p95}, max ${report.turns.max}`,
  ].join("\n");
  return { ok: true, summary, structured: { ok: true, report } };
}

export interface ScaffoldOutput {
  readonly name: string;
  readonly yaml: string;
}

export function runScaffold(input: ScaffoldInput): ToolResult<ScaffoldOutput> {
  const { name, yaml } = scaffoldGame(input);
  return {
    ok: true,
    summary:
      `Scaffolded '${name}' (${input.genre}). This skeleton already validates and terminates — ` +
      `customize it, then call validate_game and simulate_game.`,
    structured: { name, yaml },
  };
}

export function runDescribeGrammar(): ToolResult<GrammarReference> {
  const ref = describeGrammar();
  return {
    ok: true,
    summary:
      `GameSpec ${ref.apiVersion}. Zones own/visibility, typed pieces, ${ref.effects.length} effect kinds, ` +
      `${ref.triggers.events.length} trigger events, a total expression language. See structured content for the full closed vocabulary.`,
    structured: ref,
  };
}

export function runListReferences(refs: readonly ReferenceGame[]): ToolResult<{ games: { name: string; title: string; description: string }[] }> {
  const games = refs.map((r) => ({ name: r.name, title: r.title, description: r.description }));
  return {
    ok: true,
    summary: `${games.length} reference game(s): ${games.map((g) => g.name).join(", ")}. Fetch one with get_reference_game.`,
    structured: { games },
  };
}

export function runGetReference(refs: readonly ReferenceGame[], name: string): ToolResult<{ name?: string; yaml?: string }> {
  const found = refs.find((r) => r.name === name);
  if (found === undefined)
    return {
      ok: false,
      summary: `No reference game '${name}'. Available: ${refs.map((r) => r.name).join(", ")}.`,
      structured: {},
    };
  return { ok: true, summary: `Reference game '${found.name}' (${found.title}).`, structured: { name: found.name, yaml: found.yaml } };
}

function clamp(n: number, lo: number, hi: number): number {
  return Math.max(lo, Math.min(hi, Math.trunc(n)));
}
