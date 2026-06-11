import { cardShortText } from "./view-model.js";

/**
 * Procedural art — no binary assets, ever (v1alpha). Card faces and backs are
 * generated from the game's own data: faces from piece properties, backs from a
 * deterministic pattern seeded by the game name. Agents write SVG, not PNGs.
 */

function hash32(text: string): number {
  let h = 0x811c9dc5;
  for (let i = 0; i < text.length; i++) {
    h ^= text.charCodeAt(i);
    h = Math.imul(h, 0x01000193);
  }
  return h >>> 0;
}

/** A small seeded generator for back patterns (presentation-only randomness). */
function mulberry(seed: number): () => number {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** Deterministic generative card back: a lattice of motifs in the game's hue. */
export function cardBackSVG(gameSeed: string): string {
  const rng = mulberry(hash32(gameSeed));
  const hue = Math.floor(rng() * 360);
  const motifs: string[] = [];
  for (let row = 0; row < 5; row++) {
    for (let col = 0; col < 4; col++) {
      const cx = 12 + col * 16 + (row % 2 === 0 ? 0 : 8);
      const cy = 14 + row * 16;
      const r = 2.6 + rng() * 2.2;
      const kind = rng();
      if (kind < 0.34) motifs.push(`<circle cx="${cx}" cy="${cy}" r="${r.toFixed(1)}"/>`);
      else if (kind < 0.67)
        motifs.push(`<rect x="${(cx - r).toFixed(1)}" y="${(cy - r).toFixed(1)}" width="${(r * 2).toFixed(1)}" height="${(r * 2).toFixed(1)}" rx="1.2" transform="rotate(45 ${cx} ${cy})"/>`);
      else
        motifs.push(`<path d="M ${cx} ${(cy - r).toFixed(1)} L ${(cx + r).toFixed(1)} ${cy} L ${cx} ${(cy + r).toFixed(1)} L ${(cx - r).toFixed(1)} ${cy} Z"/>`);
    }
  }
  return (
    `<svg viewBox="0 0 76 106" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">` +
    `<rect x="1.5" y="1.5" width="73" height="103" rx="8" fill="hsl(${hue} 45% 38%)"/>` +
    `<rect x="5.5" y="5.5" width="65" height="95" rx="5.5" fill="none" stroke="hsl(${hue} 60% 78%)" stroke-width="1.4"/>` +
    `<g fill="hsl(${hue} 55% 62%)" opacity="0.75">${motifs.join("")}</g>` +
    `</svg>`
  );
}

/** Card face: corner indices + a large center glyph, from piece properties alone. */
export function cardFaceSVG(properties: Readonly<Record<string, string | number>>): string {
  const { corner, center, tone } = cardShortText(properties);
  const ink = tone === "red" ? "#c0273a" : "#1f2733";
  const cornerText = escapeXml(corner);
  const big = escapeXml(center);
  const bigSize = big.length > 2 ? 26 : 40;
  return (
    `<svg viewBox="0 0 76 106" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">` +
    `<rect x="1.5" y="1.5" width="73" height="103" rx="8" fill="#fdfcf7"/>` +
    `<rect x="1.5" y="1.5" width="73" height="103" rx="8" fill="none" stroke="#d8d4c8" stroke-width="1"/>` +
    `<text x="8" y="20" font-size="13" font-weight="700" fill="${ink}" font-family="ui-rounded, 'Segoe UI', system-ui, sans-serif">${cornerText}</text>` +
    `<text x="68" y="98" font-size="13" font-weight="700" fill="${ink}" text-anchor="end" transform="rotate(180 68 93.5)" font-family="ui-rounded, 'Segoe UI', system-ui, sans-serif">${cornerText}</text>` +
    `<text x="38" y="53" font-size="${bigSize}" font-weight="800" fill="${ink}" text-anchor="middle" dominant-baseline="central" font-family="ui-rounded, 'Segoe UI', system-ui, sans-serif">${big}</text>` +
    `</svg>`
  );
}

function escapeXml(text: string): string {
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
