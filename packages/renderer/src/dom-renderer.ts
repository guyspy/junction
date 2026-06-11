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
import { confettiBurst, scorePop } from "./celebrate.js";
import { createSoundBank, soundForEvent, type SoundName } from "./sound.js";
import { applyThemeTokens, celebrationColors, motionParams, resolveTheme, type MotionParams } from "./theme.js";
import { buildViewModel, type CardVM, type ViewModel } from "./view-model.js";

/**
 * Cadherin's DOM controller: mounts a playable game into a container. Framework-free
 * by design — the game surface should not marry a framework (the same seam discipline
 * as the @pixi/react decision). Re-renders from the projected view-model each step;
 * theme tokens, arc-FLIP motion, synth sound, and celebrations are all driven by the
 * game's presentation data — never the other way around.
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

const MUTE_KEY = "junction-muted";

function readMuted(): boolean {
  try {
    return typeof localStorage !== "undefined" && localStorage.getItem(MUTE_KEY) === "1";
  } catch {
    return false;
  }
}

function storeMuted(muted: boolean): void {
  try {
    if (typeof localStorage !== "undefined") localStorage.setItem(MUTE_KEY, muted ? "1" : "0");
  } catch {
    /* private mode etc. — preference just doesn't persist */
  }
}

export function mountGame(container: HTMLElement, doc: GameDocument, options: MountOptions = {}): GameController {
  const seats = options.seats ?? doc.spec.meta.seats.min;
  const viewerSeat = options.seat ?? 0;
  const seed = options.seed ?? `web-${Math.floor(Math.random() * 1e9)}`;
  const botDelay = options.botDelay ?? 800;
  const botRng = createRng(`${seed}:bot`);

  const theme = resolveTheme(doc.spec);
  const motion = motionParams(theme);
  const colors = celebrationColors(theme);
  const sound = createSoundBank(theme.sound, readMuted());

  let state: GameState = buildInitialState(doc, seats, createRng(`${seed}:setup`)).state;
  let ticker: string[] = [];
  let stepEvents: readonly GameEvent[] = [];
  let matchStreak = 0;
  let timer: ReturnType<typeof setTimeout> | undefined;
  let disposed = false;
  let firstRender = true;

  // ---- static scaffold -------------------------------------------------------
  container.innerHTML = "";
  container.classList.add("jx-shell");
  applyThemeTokens(document.documentElement, container, theme);

  const statusBar = el("div", "jx-statusbar");
  const status = el("p", "jx-status");
  const soundToggle = el("button", "jx-sound");
  soundToggle.type = "button";
  const syncSoundToggle = (): void => {
    const muted = sound.isMuted() || theme.sound === "off";
    soundToggle.textContent = muted ? "🔇" : "🔊";
    soundToggle.setAttribute("aria-label", muted ? "turn sound on" : "turn sound off");
    soundToggle.setAttribute("aria-pressed", String(!muted));
  };
  soundToggle.addEventListener("click", () => {
    sound.setMuted(!sound.isMuted());
    storeMuted(sound.isMuted());
    syncSoundToggle();
    if (!sound.isMuted()) sound.play("tap");
  });
  syncSoundToggle();
  statusBar.append(status, soundToggle);

  const zonesHost = el("div", "jx-zones");
  const buttonsHost = el("div", "jx-buttons");
  const tickerHost = el("div", "jx-ticker");
  const live = el("div", "visually-hidden");
  live.setAttribute("role", "status");
  live.setAttribute("aria-live", "polite");
  container.append(statusBar, zonesHost, buttonsHost, tickerHost, live);

  // Browsers require a user gesture before audio: unlock on the first interaction.
  const unlockOnce = (): void => sound.unlock();
  container.addEventListener("pointerdown", unlockOnce, { capture: true });

  // ---- render loop ------------------------------------------------------------
  function render(): void {
    const vm = buildViewModel(doc, state, viewerSeat);
    const firstRects = measureCards(zonesHost);

    status.textContent = vm.statusLine;
    status.classList.toggle("your-turn", vm.yourTurn);

    renderZones(zonesHost, vm, doc.metadata.name, onMove);
    renderButtons(buttonsHost, vm, onMove);
    renderTicker(tickerHost, ticker);

    if (firstRender) {
      firstRender = false;
      playEntrance(zonesHost, motion);
    } else {
      playFlip(zonesHost, firstRects, motion);
    }
    playCelebrations(vm);
    if (vm.ended) showBanner(container, vm);
  }

  function playCelebrations(vm: ViewModel): void {
    // Score pops: pieces gained into an owner-zone instance this step.
    const gains = new Map<string, number>();
    for (const event of stepEvents)
      if (event.type === "pieceMoved" && event.to.includes("#") && event.bySeat !== null)
        gains.set(event.to, (gains.get(event.to) ?? 0) + 1);
    for (const [zoneKey, count] of gains) {
      if (count < 2) continue;
      const section = zonesHost.querySelector<HTMLElement>(`[data-zone-key="${zoneKey}"]`);
      if (section === null) continue;
      const streakText = matchStreak >= 2 ? ` 🔥×${matchStreak}` : "";
      scorePop(section, `+${count}${streakText}`, "var(--accent)");
    }
    // Confetti on the viewer's victory.
    const ended = stepEvents.find((e) => e.type === "gameEnded");
    if (
      ended?.type === "gameEnded" &&
      ended.winnerSeat === viewerSeat &&
      theme.celebration === "festive" &&
      vm.ended
    )
      confettiBurst(colors);
    stepEvents = [];
  }

  function pushEvents(events: readonly GameEvent[]): void {
    stepEvents = events;
    const lines = announceAll(events, viewerSeat);
    if (lines.length > 0) {
      ticker = [...ticker, ...lines].slice(-6);
      live.textContent = lines[lines.length - 1]!;
    }
    // Streak bookkeeping (consecutive matches by the viewer).
    for (const event of events)
      if (event.type === "pairResolved" && event.bySeat === viewerSeat)
        matchStreak = event.matched ? matchStreak + 1 : 0;
    // Sounds: dedupe per step; a game-end fanfare/defeat owns the moment.
    const names: SoundName[] = [];
    for (const event of events) {
      const name = soundForEvent(event, viewerSeat);
      if (name !== null && !names.includes(name)) names.push(name);
    }
    const finale = names.find((n) => n === "fanfare" || n === "defeat");
    for (const name of finale !== undefined ? [finale] : names.slice(0, 2)) sound.play(name);
  }

  function onMove(move: { action: string; target?: string }): void {
    if (state.status !== "running" || state.activeSeat !== viewerSeat) return;
    sound.unlock();
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
      unstickViewer();
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

  pushEvents([{ seq: 0, type: "gameStarted", seats, game: doc.metadata.name }]);
  unstickViewer();
  render();
  scheduleBots();

  return {
    dispose: () => {
      disposed = true;
      if (timer !== undefined) clearTimeout(timer);
      container.removeEventListener("pointerdown", unlockOnce, { capture: true });
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
    section.dataset["zoneKey"] = `${zone.zone}${zone.ownerSeat === null ? "" : `#${zone.ownerSeat}`}`;

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

// ---- motion --------------------------------------------------------------------

function reducedMotion(): boolean {
  return typeof window === "undefined" || (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false);
}

function measureCards(host: HTMLElement): Map<string, DOMRect> {
  const rects = new Map<string, DOMRect>();
  for (const node of host.querySelectorAll<HTMLElement>("[data-piece-id]"))
    rects.set(node.dataset["pieceId"]!, node.getBoundingClientRect());
  return rects;
}

/** Staggered deal-in on the first render. */
function playEntrance(host: HTMLElement, motion: MotionParams): void {
  if (reducedMotion()) return;
  let index = 0;
  for (const node of host.querySelectorAll<HTMLElement>("[data-piece-id]")) {
    if (typeof node.animate !== "function") return;
    node.animate(
      [
        { opacity: 0, transform: "translateY(18px) scale(0.92)" },
        { opacity: 1, transform: "none" },
      ],
      {
        duration: 340,
        delay: Math.min(index++, 24) * motion.staggerStep,
        easing: motion.easing,
        fill: "backwards",
      },
    );
  }
}

/** FLIP with an arc: cards fly in a lifted curve with a slight tilt, like a thrown card. */
function playFlip(host: HTMLElement, firstRects: Map<string, DOMRect>, motion: MotionParams): void {
  if (reducedMotion()) return;
  for (const node of host.querySelectorAll<HTMLElement>("[data-piece-id]")) {
    const id = node.dataset["pieceId"]!;
    const first = firstRects.get(id);
    if (first === undefined) continue;
    const last = node.getBoundingClientRect();
    const dx = first.left - last.left;
    const dy = first.top - last.top;
    const distance = Math.hypot(dx, dy);
    if (distance < 2) continue;
    if (typeof node.animate !== "function") continue;

    const lift = Math.min(90, distance * motion.arc);
    const tilt = motion.tilt === 0 ? 0 : Math.sign(dx || 1) * Math.min(motion.tilt, distance / 24);
    node.animate(
      [
        { transform: `translate(${dx}px, ${dy}px) rotate(${tilt}deg)` },
        {
          transform: `translate(${dx * 0.5}px, ${dy * 0.5 - lift}px) rotate(${tilt * 0.5}deg) scale(${motion.liftScale})`,
          offset: 0.5,
        },
        { transform: "translate(0, 0) rotate(0deg) scale(1)" },
      ],
      { duration: motion.moveDuration, easing: motion.easing },
    );
  }
}
