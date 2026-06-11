import type { GameDocument } from "@junction/spec";
import { simulate } from "@junction/runtime";

/**
 * The single-file game page, as pure functions: every consumer (CLI `render`,
 * Integrin's `render_game`, the future studio) builds the same artifact —
 * engine + renderer bundle + GameSpec inlined, QA badges stamped at build time.
 * Platform-agnostic: callers supply the bundle JS; nothing here touches fs.
 */

export interface QaBadgeOptions {
  readonly games?: number;
  readonly seed?: string;
}

/** Certification badges, computed by playing the game — the page carries its proof. */
export function computeQaBadges(doc: GameDocument, options: QaBadgeOptions = {}): string[] {
  const report = simulate(doc, { games: options.games ?? 400, seed: options.seed ?? "badges" });
  const badges: string[] = [];
  badges.push(report.capped === 0 ? "✓ always ends" : `⚠ ${report.capped} unfinished runs`);
  const uniform = 1 / report.seats;
  const worstSkew = Math.max(...report.winRateBySeat.map((r) => Math.abs(r - uniform)), 0);
  badges.push(worstSkew <= 0.15 ? "✓ fair seats" : "⚠ seat skew");
  badges.push(`~${report.turns.p50} turns`);
  if (doc.spec.meta.estMinutes !== undefined) badges.push(`~${doc.spec.meta.estMinutes} min`);
  badges.push(
    `${doc.spec.meta.seats.min === doc.spec.meta.seats.max ? doc.spec.meta.seats.min : `${doc.spec.meta.seats.min}–${doc.spec.meta.seats.max}`} players`,
  );
  return badges;
}

export interface GamePageInput {
  readonly doc: GameDocument;
  /** The original YAML (inlined so the page is self-describing and remixable). */
  readonly yaml: string;
  /** The standalone renderer+engine IIFE (dist/standalone.js contents). */
  readonly bundleJs: string;
  readonly badges: readonly string[];
}

function escapeHtml(text: string): string {
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

/** One self-contained playable HTML document. */
export function buildGamePageHtml(input: GamePageInput): string {
  const { doc, yaml } = input;
  const badgeHtml = input.badges
    .map((b) => `<span class="jx-badge${b.startsWith("⚠") ? " warn" : ""}">${escapeHtml(b)}</span>`)
    .join("");
  const bundle = input.bundleJs.replace(/<\/script/gi, "<\\/script");
  const yamlJson = JSON.stringify(yaml).replace(/</g, "\\u003c");
  const title = escapeHtml(doc.spec.meta.title);
  const description = escapeHtml(doc.spec.meta.description ?? "An educational game made with Junction.");

  return `<!doctype html>
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
}
