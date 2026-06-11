/**
 * Celebrations — confetti bursts and floating score pops. Plain DOM + WAAPI,
 * deterministic rules untouched: these subscribe to events, they never cause them.
 */

function reducedMotion(): boolean {
  return typeof window !== "undefined" && (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false);
}

function canAnimate(node: HTMLElement): boolean {
  return typeof node.animate === "function";
}

/** A full-screen confetti burst from the top center. */
export function confettiBurst(colors: readonly string[], pieces = 90): void {
  if (reducedMotion() || typeof document === "undefined") return;
  const layer = document.createElement("div");
  layer.setAttribute("aria-hidden", "true");
  layer.style.cssText = "position:fixed;inset:0;pointer-events:none;overflow:hidden;z-index:50;";
  document.body.append(layer);

  const w = window.innerWidth;
  const h = window.innerHeight;
  let pending = 0;
  for (let i = 0; i < pieces; i++) {
    const bit = document.createElement("div");
    const size = 6 + Math.random() * 7;
    const color = colors[i % colors.length]!;
    const round = Math.random() < 0.4;
    bit.style.cssText =
      `position:absolute;left:${w / 2}px;top:-12px;width:${size}px;height:${size * (round ? 1 : 0.55)}px;` +
      `background:${color};border-radius:${round ? "50%" : "2px"};will-change:transform,opacity;`;
    layer.append(bit);
    if (!canAnimate(bit)) continue;

    const driftX = (Math.random() - 0.5) * w * 0.9;
    const fall = h * (0.55 + Math.random() * 0.5);
    const spin = (Math.random() - 0.5) * 1080;
    const duration = 1400 + Math.random() * 1200;
    pending++;
    const animation = bit.animate(
      [
        { transform: "translate(0, 0) rotate(0deg)", opacity: 1 },
        { transform: `translate(${driftX * 0.6}px, ${fall * 0.45}px) rotate(${spin * 0.5}deg)`, opacity: 1, offset: 0.5 },
        { transform: `translate(${driftX}px, ${fall}px) rotate(${spin}deg)`, opacity: 0 },
      ],
      { duration, easing: "cubic-bezier(0.2, 0.6, 0.4, 1)", delay: Math.random() * 220, fill: "forwards" },
    );
    animation.onfinish = () => {
      if (--pending === 0) layer.remove();
    };
  }
  if (pending === 0) layer.remove();
  // Backstop: never leave the layer behind.
  setTimeout(() => layer.remove(), 3500);
}

/** A floating "+N!" pop above an element (e.g. the zone that just gained cards). */
export function scorePop(anchor: HTMLElement, text: string, color: string): void {
  if (reducedMotion() || typeof document === "undefined") return;
  const rect = anchor.getBoundingClientRect();
  const pop = document.createElement("div");
  pop.textContent = text;
  pop.setAttribute("aria-hidden", "true");
  pop.style.cssText =
    `position:fixed;left:${rect.left + rect.width / 2}px;top:${rect.top + 8}px;transform:translateX(-50%);` +
    `font-weight:900;font-size:26px;color:${color};text-shadow:0 2px 6px rgba(0,0,0,0.45);` +
    "pointer-events:none;z-index:51;will-change:transform,opacity;";
  document.body.append(pop);
  if (!canAnimate(pop)) {
    setTimeout(() => pop.remove(), 100);
    return;
  }
  const animation = pop.animate(
    [
      { transform: "translateX(-50%) translateY(8px) scale(0.7)", opacity: 0 },
      { transform: "translateX(-50%) translateY(-14px) scale(1.15)", opacity: 1, offset: 0.3 },
      { transform: "translateX(-50%) translateY(-46px) scale(1)", opacity: 0 },
    ],
    { duration: 900, easing: "cubic-bezier(0.3, 1.2, 0.4, 1)" },
  );
  animation.onfinish = () => pop.remove();
  setTimeout(() => pop.remove(), 1500);
}
