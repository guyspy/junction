import { parseGameDocument } from "@junction/spec";
import { CADHERIN_CSS } from "./styles.js";
import { mountGame, mountOnlineGame } from "./dom-renderer.js";
import { createPixiVisualAdapter } from "./pixi-adapter.js";

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

  mountGame(container, parsed.data, {
    seat,
    seats,
    ...(seed !== undefined ? { seed } : {}),
    visualAdapter: createPixiVisualAdapter(),
  });
}

/** Online boot: join the room whose code is in the URL (?code=ABCDE) over this host's /ws. */
function bootOnline(options: { containerId?: string } = {}): void {
  if (document.getElementById("jx-style") === null) {
    const style = document.createElement("style");
    style.id = "jx-style";
    style.textContent = CADHERIN_CSS;
    document.head.append(style);
  }
  const container = document.getElementById(options.containerId ?? "junction-root");
  if (container === null) throw new Error("junction: container not found");
  const params = new URLSearchParams(window.location.search);
  const code = params.get("code") ?? "";
  if (code === "") {
    // The classroom join screen: type the code from the projector.
    container.innerHTML = "";
    const form = document.createElement("form");
    form.className = "jx-banner-box";
    form.style.cssText = "margin: 60px auto; max-width: 360px; display: flex; flex-direction: column; gap: 12px;";
    const label = document.createElement("label");
    label.textContent = "Enter your room code";
    label.style.cssText = "font-weight: 800; font-size: 20px;";
    const input = document.createElement("input");
    input.style.cssText =
      "font: inherit; font-size: 28px; font-weight: 800; text-align: center; letter-spacing: 6px; text-transform: uppercase; padding: 10px; border-radius: 10px; border: 2px solid #ccc;";
    input.maxLength = 5;
    input.autofocus = true;
    input.setAttribute("aria-label", "room code");
    const button = document.createElement("button");
    button.type = "submit";
    button.className = "jx-button";
    button.textContent = "Join";
    form.append(label, input, button);
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      const value = input.value.trim().toUpperCase();
      if (value.length === 5) window.location.search = `?code=${encodeURIComponent(value)}`;
    });
    container.append(form);
    return;
  }
  const proto = window.location.protocol === "https:" ? "wss" : "ws";
  const url = `${proto}://${window.location.host}/ws?code=${encodeURIComponent(code)}`;
  const name = params.get("name") ?? undefined;
  mountOnlineGame(container, {
    url,
    ...(name !== undefined ? { name } : {}),
    visualAdapter: createPixiVisualAdapter(),
  });
}

declare global {
  interface Window {
    JunctionGame: {
      boot: (options: BootOptions) => void;
      bootOnline: (options?: { containerId?: string }) => void;
      css: string;
    };
  }
}

window.JunctionGame = { boot, bootOnline, css: CADHERIN_CSS };
