import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import type { GameDocument } from "@junction/spec";
import { buildGamePageHtml, computeQaBadges } from "@junction/renderer";

/**
 * `junction render` — YAML in, a single self-contained playable HTML file out.
 * The pure page builder lives in @junction/renderer; this module only resolves
 * the standalone bundle from disk (node-side concern).
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

export function loadStandaloneBundle(): string {
  const require = createRequire(import.meta.url);
  const bundlePath = require.resolve("@junction/renderer/standalone.js");
  return readFileSync(bundlePath, "utf8");
}

export function renderGameHtml(input: RenderInput): RenderOutput {
  const badges = computeQaBadges(input.doc, { games: input.badgeGames, seed: input.seed });
  const html = buildGamePageHtml({
    doc: input.doc,
    yaml: input.yaml,
    bundleJs: loadStandaloneBundle(),
    badges,
  });
  return { html, badges };
}
