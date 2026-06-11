import { parseGameDocument } from "@junction/spec";
import { CADHERIN_CSS } from "./styles.js";
import { mountGame } from "./dom-renderer.js";

/**
 * The single-file entry: `junction render` inlines this bundle next to the GameSpec
 * YAML and calls JunctionGame.boot(). ?seat= and ?seed= URL params override defaults,
 * making any rendered file a shareable, replayable artifact.
 */

interface BootOptions {
  readonly yaml: string;
  readonly containerId?: string;
}

function boot(options: BootOptions): void {
  if (document.getElementById("jx-style") === null) {
    const style = document.createElement("style");
    style.id = "jx-style";
    style.textContent = CADHERIN_CSS;
    document.head.append(style);
  }

  const container = document.getElementById(options.containerId ?? "junction-root");
  if (container === null) throw new Error("junction: container not found");

  const parsed = parseGameDocument(options.yaml, { file: "embedded" });
  if (!parsed.ok) {
    container.textContent = `This game file is invalid:\n${parsed.diagnostics.map((d) => d.message).join("\n")}`;
    return;
  }

  const params = new URLSearchParams(window.location.search);
  const seatParam = Number(params.get("seat"));
  const seats = parsed.data.spec.meta.seats.min;
  const seat = Number.isInteger(seatParam) && seatParam >= 0 && seatParam < seats ? seatParam : 0;
  const seed = params.get("seed") ?? undefined;

  mountGame(container, parsed.data, { seat, seats, ...(seed !== undefined ? { seed } : {}) });
}

declare global {
  interface Window {
    JunctionGame: { boot: (options: BootOptions) => void; css: string };
  }
}

window.JunctionGame = { boot, css: CADHERIN_CSS };
