import type { GameSpec, ThemeDecl } from "@junction/spec";

/**
 * Rung 2 of the customization ladder: theme tokens → CSS custom properties and
 * motion parameters. Pure data in, pure data out — presentation never touches rules.
 */

export type Theme = ThemeDecl;

export function resolveTheme(spec: GameSpec): Theme {
  return spec.presentation.theme;
}

interface Palette {
  readonly feltLight: string;
  readonly felt: string;
  readonly feltEdge: string;
}

const TABLES: Record<Theme["table"], Palette> = {
  forest: { feltLight: "#2a8a5c", felt: "#1e6b46", feltEdge: "#145033" },
  ocean: { feltLight: "#2b6f9e", felt: "#1d5379", feltEdge: "#143d5a" },
  sunset: { feltLight: "#9e5a3c", felt: "#7d3f54", feltEdge: "#532a47" },
  slate: { feltLight: "#5a6572", felt: "#3f4853", feltEdge: "#2b323b" },
  plum: { feltLight: "#7a4f9e", felt: "#5b3a78", feltEdge: "#3f2856" },
};

interface AccentColors {
  readonly accent: string;
  readonly accentInk: string;
  readonly accentGlow: string;
}

const ACCENTS: Record<Theme["accent"], AccentColors> = {
  gold: { accent: "#ffd166", accentInk: "#3a2c00", accentGlow: "rgba(255, 209, 102, 0.55)" },
  sky: { accent: "#7be3ff", accentInk: "#063246", accentGlow: "rgba(123, 227, 255, 0.55)" },
  coral: { accent: "#ff8e7a", accentInk: "#4a1408", accentGlow: "rgba(255, 142, 122, 0.55)" },
  lime: { accent: "#c5e86c", accentInk: "#2a3a00", accentGlow: "rgba(197, 232, 108, 0.55)" },
};

const CARD_SIZES: Record<Theme["cardSize"], { w: number; h: number }> = {
  compact: { w: 60, h: 84 },
  regular: { w: 76, h: 106 },
  large: { w: 92, h: 128 },
};

/** Apply theme tokens as CSS custom properties on the page root + shell. */
export function applyThemeTokens(root: HTMLElement, shell: HTMLElement, theme: Theme): void {
  const palette = TABLES[theme.table];
  const accent = ACCENTS[theme.accent];
  const size = CARD_SIZES[theme.cardSize];
  const vars: Record<string, string> = {
    "--felt-light": palette.feltLight,
    "--felt": palette.felt,
    "--felt-edge": palette.feltEdge,
    "--accent": accent.accent,
    "--accent-ink": accent.accentInk,
    "--accent-glow": accent.accentGlow,
    "--card-w": `${size.w}px`,
    "--card-h": `${size.h}px`,
  };
  for (const [key, value] of Object.entries(vars)) {
    root.style.setProperty(key, value);
    shell.style.setProperty(key, value);
  }
}

export interface MotionParams {
  /** Flight duration for card movement (ms). */
  readonly moveDuration: number;
  /** WAAPI easing for flights. */
  readonly easing: string;
  /** Arc lift as a fraction of travel distance (0 = straight line). */
  readonly arc: number;
  /** Max rotation during flight (deg). */
  readonly tilt: number;
  /** Per-card delay in the deal-in cascade (ms). */
  readonly staggerStep: number;
  /** Mid-flight scale boost. */
  readonly liftScale: number;
}

const SPRING = "cubic-bezier(0.34, 1.45, 0.5, 1)";
const SMOOTH = "cubic-bezier(0.3, 0.9, 0.4, 1)";

export function motionParams(theme: Theme): MotionParams {
  switch (theme.motion) {
    case "calm":
      return { moveDuration: 300, easing: SMOOTH, arc: 0, tilt: 0, staggerStep: 22, liftScale: 1 };
    case "lively":
      return { moveDuration: 420, easing: SPRING, arc: 0.18, tilt: 5, staggerStep: 30, liftScale: 1.06 };
    case "bouncy":
      return { moveDuration: 520, easing: SPRING, arc: 0.3, tilt: 9, staggerStep: 40, liftScale: 1.12 };
  }
}

/** Confetti colors derived from the theme (accent + paper + felt-light). */
export function celebrationColors(theme: Theme): string[] {
  return [ACCENTS[theme.accent].accent, "#fdfcf7", TABLES[theme.table].feltLight, "#ff8e7a", "#7be3ff"];
}
