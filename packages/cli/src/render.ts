import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import type { GameDocument } from "@junction/spec";
import { simulate } from "@junction/runtime";

/**
 * `junction render` — YAML in, a single self-contained playable HTML file out.
 * The engine + renderer bundle and the GameSpec are inlined; QA badges from a
 * fresh simulation are stamped into the page. Email it, drop it in a classroom
 * LMS, open it offline — it just plays.
 */

export interface RenderInput {
  readonly doc: GameDocument;
  readonly yaml: string;
  readonly badgeGames: number;
  readonly seed: string;
}

export interface RenderOutput {
  readonly html: string;
  readonly badges: readonly string[];
}

function loadStandaloneBundle(): string {
  const require = createRequire(import.meta.url);
  const bundlePath = require.resolve("@junction/renderer/standalone.js");
  return readFileSync(bundlePath, "utf8");
}

function escapeHtml(text: string): string {
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

export function renderGameHtml(input: RenderInput): RenderOutput {
  const { doc, yaml } = input;

  // Certification badges, computed at render time — the page carries its proof.
  const report = simulate(doc, { games: input.badgeGames, seed: input.seed });
  const badges: string[] = [];
  badges.push(report.capped === 0 ? "✓ always ends" : `⚠ ${report.capped} unfinished runs`);
  const uniform = 1 / report.seats;
  const worstSkew = Math.max(...report.winRateBySeat.map((r) => Math.abs(r - uniform)), 0);
  badges.push(worstSkew <= 0.15 ? "✓ fair seats" : "⚠ seat skew");
  badges.push(`~${report.turns.p50} turns`);
  if (doc.spec.meta.estMinutes !== undefined) badges.push(`~${doc.spec.meta.estMinutes} min`);
  badges.push(`${doc.spec.meta.seats.min === doc.spec.meta.seats.max ? doc.spec.meta.seats.min : `${doc.spec.meta.seats.min}–${doc.spec.meta.seats.max}`} players`);

  const badgeHtml = badges
    .map((b) => `<span class="jx-badge${b.startsWith("⚠") ? " warn" : ""}">${escapeHtml(b)}</span>`)
    .join("");

  const bundle = loadStandaloneBundle().replace(/<\/script/gi, "<\\/script");
  const yamlJson = JSON.stringify(yaml).replace(/</g, "\\u003c");
  const title = escapeHtml(doc.spec.meta.title);
  const description = escapeHtml(doc.spec.meta.description ?? "An educational game made with Junction.");

  const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="${description}">
<title>${title} · Junction</title>
</head>
<body>
<div class="jx-shell">
  <header class="jx-header">
    <h1 class="jx-title">${title}</h1>
    <div class="jx-badges">${badgeHtml}</div>
  </header>
  <main id="junction-root" aria-label="${title} game board"></main>
  <footer class="jx-footer">
    Made with Junction — the game is data, the engine is the judge.
    Tip: add <code>?seed=anything</code> to the URL to replay the same shuffle, or <code>?seat=1</code> to sit elsewhere.
  </footer>
</div>
<script>${bundle}</script>
<script>
window.JunctionGame.boot({ yaml: ${yamlJson} });
</script>
</body>
</html>
`;
  return { html, badges };
}
