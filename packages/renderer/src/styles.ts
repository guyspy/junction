/**
 * Cadherin's stylesheet, exported as a string so `junction render` can inline it into
 * a single self-contained HTML file. Fully token-driven: the theme system overrides
 * the custom properties below (rung 2 of the customization ladder). Keyboard focus,
 * reduced motion, and small screens are first-class (WCAG 2.1 AA posture).
 */
export const CADHERIN_CSS = `
:root {
  --felt-light: #2a8a5c;
  --felt: #1e6b46;
  --felt-edge: #145033;
  --paper: #fdfcf7;
  --ink: #1f2733;
  --accent: #ffd166;
  --accent-ink: #3a2c00;
  --accent-glow: rgba(255, 209, 102, 0.55);
  --card-w: 76px;
  --card-h: 106px;
  color-scheme: light;
}
* { box-sizing: border-box; }
html, body { margin: 0; padding: 0; }
body {
  font-family: ui-rounded, "Segoe UI", system-ui, -apple-system, sans-serif;
  background:
    radial-gradient(1100px 600px at 50% -10%, var(--felt-light) 0%, var(--felt) 55%, var(--felt-edge) 100%);
  background-attachment: fixed;
  color: #f4f7f4;
  min-height: 100vh;
}
.jx-shell { max-width: 980px; margin: 0 auto; padding: 16px 16px 48px; }
.jx-header { display: flex; flex-wrap: wrap; align-items: baseline; gap: 10px 14px; padding: 6px 2px 12px; }
.jx-title { font-size: 26px; font-weight: 800; letter-spacing: 0.2px; margin: 0; }
.jx-badges { display: flex; flex-wrap: wrap; gap: 6px; }
.jx-badge {
  font-size: 12px; font-weight: 600; padding: 3px 9px; border-radius: 999px;
  background: rgba(255,255,255,0.13); border: 1px solid rgba(255,255,255,0.25);
}
.jx-badge.warn { background: rgba(255, 180, 60, 0.25); border-color: rgba(255, 200, 90, 0.6); }

.jx-statusbar { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin: 6px 0 14px; }
.jx-status { font-size: 16px; font-weight: 650; margin: 0; min-height: 24px; }
.jx-status.your-turn { color: var(--accent); }
.jx-sound {
  font: inherit; font-size: 17px; line-height: 1; padding: 7px 10px; border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.3); background: rgba(255,255,255,0.12); color: inherit;
  cursor: pointer;
}
.jx-sound:hover { background: rgba(255,255,255,0.2); }
.jx-sound:focus-visible { outline: 3px solid #fff; outline-offset: 2px; }

.jx-stats { display: flex; flex-wrap: wrap; gap: 10px; margin: 0 0 14px; }
.jx-stat-group {
  display: flex; align-items: center; gap: 6px; padding: 6px 10px;
  background: rgba(0,0,0,0.18); border: 1px solid rgba(255,255,255,0.10); border-radius: 999px;
}
.jx-stat-group.mine { border-color: var(--accent-glow); }
.jx-stat-who { font-size: 12px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.6px; opacity: 0.85; margin-right: 2px; }
.jx-stat {
  font-size: 13px; font-weight: 700; padding: 3px 10px; border-radius: 999px;
  background: rgba(255,255,255,0.14); white-space: nowrap;
}

.jx-zones { display: flex; flex-direction: column; gap: 14px; }
.jx-zone {
  background: rgba(0,0,0,0.18); border: 1px solid rgba(255,255,255,0.10);
  border-radius: 14px; padding: 10px 12px 14px;
}
.jx-zone.mine { border-color: var(--accent-glow); }
.jx-zone h2 {
  margin: 0 0 8px; font-size: 12.5px; font-weight: 700; text-transform: uppercase;
  letter-spacing: 0.8px; opacity: 0.85;
}
.jx-cards { display: flex; flex-wrap: wrap; gap: 8px; min-height: calc(var(--card-h) + 4px); align-items: flex-start; }
.jx-zone[data-kind="stack"] .jx-cards, .jx-zone[data-kind="pile"] .jx-cards { gap: 0; }
.jx-zone[data-kind="stack"] .jx-card:not(:first-child),
.jx-zone[data-kind="pile"] .jx-card:not(:first-child) { margin-left: calc(var(--card-w) * -0.86); }
.jx-zone[data-kind="hand"] .jx-card:not(:first-child) { margin-left: calc(var(--card-w) * -0.35); }

.jx-card {
  width: var(--card-w); height: var(--card-h); border: 0; padding: 0; background: none;
  perspective: 700px; position: relative; border-radius: 9px;
  transition: transform 0.16s ease;
}
.jx-card svg { width: 100%; height: 100%; display: block; border-radius: 9px;
  filter: drop-shadow(0 2px 3px rgba(0,0,0,0.35)); }
.jx-card-inner {
  width: 100%; height: 100%; position: relative;
  transform-style: preserve-3d; transition: transform 0.45s cubic-bezier(0.3, 1.2, 0.4, 1);
}
.jx-card.face-down .jx-card-inner { transform: rotateY(180deg); }
.jx-face, .jx-back { position: absolute; inset: 0; backface-visibility: hidden; }
.jx-back { transform: rotateY(180deg); }

button.jx-card { cursor: default; }
button.jx-card.playable { cursor: pointer; }
button.jx-card.playable:hover, button.jx-card.playable:focus-visible { transform: translateY(-10px); }
button.jx-card.playable:active { transform: translateY(-4px) scale(0.94); }
button.jx-card.playable::after {
  content: ""; position: absolute; inset: -3px; border-radius: 12px;
  border: 2.5px solid var(--accent); box-shadow: 0 0 14px var(--accent-glow);
  pointer-events: none;
  animation: jx-pulse 2.4s ease-in-out infinite;
}
@keyframes jx-pulse {
  0%, 100% { box-shadow: 0 0 10px var(--accent-glow); opacity: 0.85; }
  50% { box-shadow: 0 0 22px var(--accent-glow); opacity: 1; }
}
.jx-card:focus-visible { outline: 3px solid #fff; outline-offset: 3px; }
.jx-card.lifted { transform: translateY(-14px); }
.jx-card.lifted::before {
  content: ""; position: absolute; inset: -3px; border-radius: 12px;
  border: 2.5px solid #7be3ff; box-shadow: 0 0 14px rgba(123, 227, 255, 0.6);
  pointer-events: none; z-index: 1;
}

.jx-buttons { display: flex; gap: 10px; margin: 12px 0; flex-wrap: wrap; }
.jx-button {
  font: inherit; font-weight: 750; font-size: 15px; padding: 10px 22px; border-radius: 12px;
  border: 0; background: var(--accent); color: var(--accent-ink); cursor: pointer;
  box-shadow: 0 3px 0 rgba(0,0,0,0.25);
  transition: transform 0.08s ease, box-shadow 0.08s ease;
}
.jx-button:hover { filter: brightness(1.06); }
.jx-button:focus-visible { outline: 3px solid #fff; outline-offset: 2px; }
.jx-button:active { transform: translateY(2px) scale(0.98); box-shadow: 0 1px 0 rgba(0,0,0,0.25); }

.jx-ticker {
  margin-top: 16px; background: rgba(0,0,0,0.22); border-radius: 12px; padding: 10px 14px;
  font-size: 13.5px; line-height: 1.55; min-height: 56px;
}
.jx-ticker p { margin: 0; opacity: 0.55; }
.jx-ticker p:last-child { opacity: 1; font-weight: 650; }

.jx-banner {
  position: fixed; inset: 0; display: flex; align-items: center; justify-content: center;
  background: rgba(8, 28, 18, 0.78); backdrop-filter: blur(3px); z-index: 10;
}
.jx-banner-box {
  background: var(--paper); color: var(--ink); border-radius: 18px; padding: 34px 44px;
  text-align: center; box-shadow: 0 18px 60px rgba(0,0,0,0.5); max-width: 80vw;
}
.jx-banner-box h2 { margin: 0 0 8px; font-size: 30px; }
.jx-banner-box p { margin: 0 0 18px; opacity: 0.75; }

.jx-footer { margin-top: 26px; font-size: 12px; opacity: 0.6; text-align: center; }
.jx-footer a { color: inherit; }

.visually-hidden {
  position: absolute; width: 1px; height: 1px; margin: -1px; padding: 0;
  overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; border: 0;
}

@media (max-width: 560px) {
  :root { --card-w: 56px; --card-h: 78px; }
  .jx-title { font-size: 21px; }
}
@media (prefers-reduced-motion: reduce) {
  .jx-card, .jx-card-inner, .jx-button { transition: none !important; }
  button.jx-card.playable::after { animation: none !important; }
}
`;
