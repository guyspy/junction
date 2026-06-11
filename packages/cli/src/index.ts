#!/usr/bin/env node
import { readFileSync, writeFileSync } from "node:fs";
import { formatDiagnosticText, parseGameDocument, type Diagnostic } from "@junction/spec";
import { simulate, type SimulateReport } from "@junction/runtime";
import { runPlay } from "./play.js";
import { renderGameHtml } from "./render.js";

/**
 * junction validate <file> [--format json|text]
 * junction simulate <file> [--games N] [--seed S] [--seats N] [--max-turns M] [--format json|text]
 * junction play <file> [--seat N] [--seats N] [--seed S]
 * junction render <file> [--out file.html] [--seed S]
 * Exit codes: 0 ok, 1 diagnostics with errors / failed run, 2 usage.
 */

interface Flags {
  readonly format: "json" | "text";
  readonly games: number;
  readonly seed: string;
  readonly seats?: number;
  readonly seat: number;
  readonly maxTurns: number;
  readonly out?: string;
}

function parseFlags(args: readonly string[]): { positional: string[]; flags: Flags } {
  const positional: string[] = [];
  let format: "json" | "text" | undefined;
  let games = 100;
  let seed = "42";
  let seats: number | undefined;
  let seat = 0;
  let maxTurns = 1000;
  let out: string | undefined;
  for (let i = 0; i < args.length; i++) {
    const arg = args[i]!;
    const next = (): string => {
      const v = args[++i];
      if (v === undefined) usage(`missing value for ${arg}`);
      return v!;
    };
    if (arg === "--format") {
      const v = next();
      if (v !== "json" && v !== "text") usage(`--format must be json|text, got '${v}'`);
      format = v as "json" | "text";
    } else if (arg === "--games") games = Number(next());
    else if (arg === "--seed") seed = next();
    else if (arg === "--seats") seats = Number(next());
    else if (arg === "--seat") seat = Number(next());
    else if (arg === "--max-turns") maxTurns = Number(next());
    else if (arg === "--out" || arg === "-o") out = next();
    else if (arg.startsWith("--")) usage(`unknown flag ${arg}`);
    else positional.push(arg);
  }
  const resolvedFormat = format ?? (process.stdout.isTTY ? "text" : "json");
  return { positional, flags: { format: resolvedFormat, games, seed, seats, seat, maxTurns, out } };
}

function usage(reason?: string): never {
  if (reason !== undefined) console.error(`junction: ${reason}\n`);
  console.error(
    [
      "Usage:",
      "  junction validate <file.yaml> [--format json|text]",
      "  junction simulate <file.yaml> [--games N] [--seed S] [--seats N] [--max-turns M] [--format json|text]",
      "  junction play     <file.yaml> [--seat N] [--seats N] [--seed S]",
      "  junction render   <file.yaml> [--out file.html] [--seed S]",
    ].join("\n"),
  );
  process.exit(2);
}

function printDiagnostics(diagnostics: readonly Diagnostic[], format: "json" | "text"): void {
  if (format === "json") console.log(JSON.stringify({ diagnostics }, null, 2));
  else for (const d of diagnostics) console.log(formatDiagnosticText(d));
}

function printReport(report: SimulateReport, format: "json" | "text"): void {
  if (format === "json") {
    console.log(JSON.stringify({ report }, null, 2));
    return;
  }
  const rate = (n: number): string => `${((n / report.games) * 100).toFixed(1)}%`;
  console.log(`junction simulate — ${report.game}`);
  console.log(`  games          ${report.games} (${report.seats} seats)`);
  console.log(`  completed      ${report.completed} (${rate(report.completed)})  capped ${report.capped}  stalled ${report.stalled}  draws ${report.draws}`);
  console.log(`  win rates      ${report.winRateBySeat.map((r, i) => `seat${i} ${(r * 100).toFixed(1)}%`).join("   ")}`);
  console.log(`  turns          min ${report.turns.min}  p50 ${report.turns.p50}  p95 ${report.turns.p95}  max ${report.turns.max}  mean ${report.turns.mean.toFixed(1)}`);
  console.log(`  action usage   ${Object.entries(report.actionUsage).map(([k, v]) => `${k}×${v}`).join("  ") || "—"}`);
  console.log("  verdicts");
  for (const note of report.notes) console.log(`    ${note}`);
}

async function main(): Promise<void> {
  const [command, ...rest] = process.argv.slice(2);
  if (command !== "validate" && command !== "simulate" && command !== "play" && command !== "render") usage();
  const { positional, flags } = parseFlags(rest);
  const file = positional[0];
  if (file === undefined) usage("missing <file.yaml>");

  let text: string;
  try {
    text = readFileSync(file!, "utf8");
  } catch {
    console.error(`junction: cannot read '${file}'`);
    process.exit(1);
  }

  const started = performance.now();
  const parsed = parseGameDocument(text, { file });
  if (!parsed.ok) {
    printDiagnostics(parsed.diagnostics, flags.format);
    process.exit(1);
  }

  if (command === "validate") {
    if (flags.format === "json")
      console.log(JSON.stringify({ ok: true, game: parsed.data.metadata.name, diagnostics: parsed.warnings }, null, 2));
    else {
      printDiagnostics(parsed.warnings, "text");
      console.log(`✓ ${file} is a valid Game ('${parsed.data.metadata.name}')`);
    }
    return;
  }

  if (command === "play") {
    const seats = flags.seats ?? parsed.data.spec.meta.seats.min;
    if (flags.seat < 0 || flags.seat >= seats) usage(`--seat must be in [0, ${seats - 1}]`);
    const code = await runPlay(parsed.data, { seats, seat: flags.seat, seed: flags.seed });
    process.exit(code);
  }

  if (command === "render") {
    const outFile = flags.out ?? `${parsed.data.metadata.name}.html`;
    const { html, badges } = renderGameHtml({
      doc: parsed.data,
      yaml: text,
      badgeGames: 400,
      seed: flags.seed,
    });
    writeFileSync(outFile, html);
    const kb = (Buffer.byteLength(html) / 1024).toFixed(0);
    console.log(`✓ rendered ${outFile} (${kb} KB, self-contained) — badges: ${badges.join(" · ")}`);
    return;
  }

  const report = simulate(parsed.data, {
    games: flags.games,
    seed: flags.seed,
    seats: flags.seats,
    maxTurns: flags.maxTurns,
  });
  printReport(report, flags.format);
  if (flags.format === "text")
    console.log(`  elapsed        ${(performance.now() - started).toFixed(0)} ms`);
}

main().catch((error: unknown) => {
  console.error(`junction: ${error instanceof Error ? error.message : String(error)}`);
  process.exit(1);
});
