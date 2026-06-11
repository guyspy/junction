import type { GameDocument } from "@junction/spec";
import {
  applyAction,
  applySkip,
  buildInitialState,
  createRng,
  legalMoves,
  randomChooser,
  type GameEvent,
  type GameState,
} from "@junction/runtime";
import { announceAll } from "./announcer.js";
import { cardBackSVG, cardFaceSVG } from "./art.js";
import { buildViewModel, type CardVM, type ViewModel } from "./view-model.js";

/**
 * Cadherin's DOM controller: mounts a playable game into a container. Framework-free
 * by design — the game surface should not marry a framework (the same seam discipline
 * as the @pixi/react decision). Re-renders from the projected view-model each step and
 * FLIP-animates cards between renders.
 */

export interface MountOptions {
  readonly seat?: number;
  readonly seats?: number;
  readonly seed?: string;
  /** ms between bot moves (0 in tests). */
  readonly botDelay?: number;
}

export interface GameController {
  readonly dispose: () => void;
}

export function mountGame(container: HTMLElement, doc: GameDocument, options: MountOptions = {}): GameController {
  const seats = options.seats ?? doc.spec.meta.seats.min;
  const viewerSeat = options.seat ?? 0;
  const seed = options.seed ?? `web-${Math.floor(Math.random() * 1e9)}`;
  const botDelay = options.botDelay ?? 750;
  const botRng = createRng(`${seed}:bot`);

  let state: GameState = buildInitialState(doc, seats, createRng(`${seed}:setup`)).state;
  let ticker: string[] = [];
  let timer: ReturnType<typeof setTimeout> | undefined;
  let disposed = false;

  // ---- static scaffold -------------------------------------------------------
  container.innerHTML = "";
  container.classList.add("jx-shell");

  const status = el("p", "jx-status");
  const zonesHost = el("div", "jx-zones");
  const buttonsHost = el("div", "jx-buttons");
  const tickerHost = el("div", "jx-ticker");
  const live = el("div", "visually-hidden");
  live.setAttribute("role", "status");
  live.setAttribute("aria-live", "polite");
  container.append(status, zonesHost, buttonsHost, tickerHost, live);

  // ---- render loop ------------------------------------------------------------
  function render(): void {
    const vm = buildViewModel(doc, state, viewerSeat);
    const firstRects = measureCards(zonesHost);

    status.textContent = vm.statusLine;
    status.classList.toggle("your-turn", vm.yourTurn);

    renderZones(zonesHost, vm, doc.metadata.name, onMove);
    renderButtons(buttonsHost, vm, onMove);
    renderTicker(tickerHost, ticker);

    playFlip(zonesHost, firstRects);
    if (vm.ended) showBanner(container, vm);
  }

  function pushEvents(events: readonly GameEvent[]): void {
    const lines = announceAll(events, viewerSeat);
    if (lines.length === 0) return;
    ticker = [...ticker, ...lines].slice(-6);
    live.textContent = lines[lines.length - 1]!;
  }

  function onMove(move: { action: string; target?: string }): void {
    if (state.status !== "running" || state.activeSeat !== viewerSeat) return;
    const result = applyAction(state, doc.spec, { seat: viewerSeat, ...move });
    if (!result.ok) return; // stale click (legal set changed); the re-render fixes it
    state = result.data.state;
    pushEvents(result.data.events);
    render();
    scheduleBots();
  }

  function scheduleBots(): void {
    if (disposed || state.status !== "running" || state.activeSeat === viewerSeat) return;
    timer = setTimeout(() => {
      if (disposed || state.status !== "running") return;
      const legal = legalMoves(state, doc.spec);
      if (state.activeSeat === viewerSeat) return;
      if (legal.length === 0) {
        const step = applySkip(state, doc.spec);
        state = step.state;
        pushEvents(step.events);
      } else {
        const move = randomChooser(legal, botRng);
        const result = applyAction(state, doc.spec, { seat: state.activeSeat, ...move });
        if (!result.ok) return;
        state = result.data.state;
        pushEvents(result.data.events);
      }
      render();
      scheduleBots();
    }, botDelay);
  }

  // Handle "viewer has no legal move" (auto-skip) so the game never hangs on the human.
  function unstickViewer(): void {
    let guard = 0;
    while (
      !disposed &&
      state.status === "running" &&
      state.activeSeat === viewerSeat &&
      legalMoves(state, doc.spec).length === 0 &&
      guard++ < 64
    ) {
      const step = applySkip(state, doc.spec);
      state = step.state;
      pushEvents(step.events);
    }
  }

  const observer = new MutationObserver(() => undefined); // placeholder for future hooks

  pushEvents([{ seq: 0, type: "gameStarted", seats, game: doc.metadata.name }]);
  unstickViewer();
  render();
  scheduleBots();

  return {
    dispose: () => {
      disposed = true;
      if (timer !== undefined) clearTimeout(timer);
      observer.disconnect();
    },
  };
}

// ---- DOM helpers ---------------------------------------------------------------

function el<K extends keyof HTMLElementTagNameMap>(tag: K, className?: string): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  if (className !== undefined) node.className = className;
  return node;
}

function renderZones(
  host: HTMLElement,
  vm: ViewModel,
  gameSeed: string,
  onMove: (move: { action: string; target?: string }) => void,
): void {
  host.innerHTML = "";
  for (const zone of vm.zones) {
    const section = el("section", "jx-zone");
    if (zone.mine) section.classList.add("mine");
    section.dataset["kind"] = zone.kind;

    const heading = el("h2");
    heading.textContent = zone.title;
    section.append(heading);

    const cards = el("div", "jx-cards");
    for (const card of zone.cards) cards.append(renderCard(card, gameSeed, onMove));
    section.append(cards);
    host.append(section);
  }
}

function renderCard(
  card: CardVM,
  gameSeed: string,
  onMove: (move: { action: string; target?: string }) => void,
): HTMLElement {
  const playable = card.move !== undefined;
  const node = el(playable ? "button" : "div", "jx-card");
  node.classList.toggle("face-down", !card.faceUp);
  node.classList.toggle("lifted", card.lifted);
  if (playable) node.classList.add("playable");
  node.dataset["pieceId"] = card.id;
  node.setAttribute("aria-label", playable ? `${card.label} — press to ${card.move!.action.replace(/-/g, " ")}` : card.label);
  if (node instanceof HTMLButtonElement) {
    node.type = "button";
    node.addEventListener("click", () => onMove(card.move!));
  }

  const inner = el("div", "jx-card-inner");
  const face = el("div", "jx-face");
  face.innerHTML = card.properties !== undefined ? cardFaceSVG(card.properties) : cardFaceSVG({});
  const back = el("div", "jx-back");
  back.innerHTML = cardBackSVG(gameSeed);
  inner.append(face, back);
  node.append(inner);
  return node;
}

function renderButtons(
  host: HTMLElement,
  vm: ViewModel,
  onMove: (move: { action: string; target?: string }) => void,
): void {
  host.innerHTML = "";
  for (const button of vm.buttons) {
    const node = el("button", "jx-button");
    node.type = "button";
    node.textContent = button.label;
    node.addEventListener("click", () => onMove(button.move));
    host.append(node);
  }
}

function renderTicker(host: HTMLElement, lines: readonly string[]): void {
  host.innerHTML = "";
  for (const line of lines) {
    const p = el("p");
    p.textContent = line;
    host.append(p);
  }
}

function showBanner(container: HTMLElement, vm: ViewModel): void {
  if (container.querySelector(".jx-banner") !== null) return;
  const banner = el("div", "jx-banner");
  const box = el("div", "jx-banner-box");
  const h2 = el("h2");
  h2.textContent = vm.winnerText ?? "Game over";
  const p = el("p");
  p.textContent = vm.title;
  const again = el("button", "jx-button");
  again.type = "button";
  again.textContent = "Play again";
  again.addEventListener("click", () => window.location.reload());
  box.append(h2, p, again);
  banner.append(box);
  container.append(banner);
}

// ---- FLIP animation --------------------------------------------------------------

function measureCards(host: HTMLElement): Map<string, DOMRect> {
  const rects = new Map<string, DOMRect>();
  for (const node of host.querySelectorAll<HTMLElement>("[data-piece-id]"))
    rects.set(node.dataset["pieceId"]!, node.getBoundingClientRect());
  return rects;
}

function playFlip(host: HTMLElement, firstRects: Map<string, DOMRect>): void {
  if (typeof window === "undefined" || window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) return;
  for (const node of host.querySelectorAll<HTMLElement>("[data-piece-id]")) {
    const id = node.dataset["pieceId"]!;
    const first = firstRects.get(id);
    if (first === undefined) continue;
    const last = node.getBoundingClientRect();
    const dx = first.left - last.left;
    const dy = first.top - last.top;
    if (Math.abs(dx) < 1 && Math.abs(dy) < 1) continue;
    if (typeof node.animate !== "function") continue; // older engines / test DOMs
    node.animate(
      [{ transform: `translate(${dx}px, ${dy}px)` }, { transform: "translate(0, 0)" }],
      { duration: 380, easing: "cubic-bezier(0.3, 1, 0.4, 1)" },
    );
  }
}
