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
import { buildViewModel, buildViewModelFromProjection, type CardVM, type ViewModel } from "./view-model.js";

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
  const diceRng = createRng(`${seed}:dice`);

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

  const statsHost = el("div", "jx-stats");
  const zonesHost = el("div", "jx-zones");
  const buttonsHost = el("div", "jx-buttons");
  const tickerHost = el("div", "jx-ticker");
  const live = el("div", "visually-hidden");
  live.setAttribute("role", "status");
  live.setAttribute("aria-live", "polite");
  container.append(statusBar, statsHost, zonesHost, buttonsHost, tickerHost, live);

  // Browsers require a user gesture before audio: unlock on the first interaction.
  const unlockOnce = (): void => sound.unlock();
  container.addEventListener("pointerdown", unlockOnce, { capture: true });

  // ---- render loop ------------------------------------------------------------
  function render(): void {
    const vm = buildViewModel(doc, state, viewerSeat);
    const firstRects = measureCards(zonesHost);

    status.textContent = vm.statusLine;
    status.classList.toggle("your-turn", vm.yourTurn);

    renderStats(statsHost, vm);
    renderZones(zonesHost, vm, doc.metadata.name, onMove);
    renderButtons(buttonsHost, vm, onMove);
    renderTicker(tickerHost, ticker);
    pulseChangedStats(statsHost, stepEvents);

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
    const result = applyAction(state, doc.spec, { seat: viewerSeat, ...move }, diceRng);
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
        const result = applyAction(state, doc.spec, { seat: state.activeSeat, ...move }, diceRng);
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

function renderStats(host: HTMLElement, vm: ViewModel): void {
  host.innerHTML = "";
  if (vm.seatStats.length === 0 && vm.globalStats.length === 0) {
    host.style.display = "none";
    return;
  }
  host.style.display = "";
  for (const group of vm.seatStats) {
    const box = el("div", "jx-stat-group");
    if (group.mine) box.classList.add("mine");
    const who = el("span", "jx-stat-who");
    who.textContent = group.title;
    box.append(who);
    for (const stat of group.stats) {
      const chip = el("span", "jx-stat");
      chip.dataset["stat"] = `seat-${group.seat}-${stat.name}`;
      chip.textContent = `${stat.name} ${stat.value}`;
      box.append(chip);
    }
    host.append(box);
  }
  if (vm.globalStats.length > 0) {
    const box = el("div", "jx-stat-group");
    for (const stat of vm.globalStats) {
      const chip = el("span", "jx-stat");
      chip.dataset["stat"] = `global-${stat.name}`;
      chip.textContent = `${stat.name} ${stat.value}`;
      box.append(chip);
    }
    host.append(box);
  }
}

/** Flash the chips whose variables changed this step (damage lands visibly). */
function pulseChangedStats(host: HTMLElement, events: readonly GameEvent[]): void {
  for (const event of events) {
    if (event.type !== "varChanged") continue;
    const key = event.scope === "seat" ? `seat-${event.seat}-${event.var}` : `global-${event.var}`;
    const chip = host.querySelector<HTMLElement>(`[data-stat="${key}"]`);
    if (chip === null || typeof chip.animate !== "function") continue;
    const dropped = event.to < event.from;
    chip.animate(
      [
        { transform: "scale(1)", background: "rgba(255,255,255,0.14)" },
        {
          transform: "scale(1.35)",
          background: dropped ? "rgba(255, 90, 90, 0.7)" : "rgba(120, 230, 140, 0.7)",
          offset: 0.35,
        },
        { transform: "scale(1)", background: "rgba(255,255,255,0.14)" },
      ],
      { duration: 650, easing: "cubic-bezier(0.3, 1.2, 0.4, 1)" },
    );
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

// ---- online play -----------------------------------------------------------------

export interface OnlineMountOptions {
  /** WebSocket URL including the room code, e.g. wss://host/ws?code=ABCDE */
  readonly url: string;
  readonly name?: string;
}

interface WireWelcome {
  t: "welcome";
  seat: number;
  token: string;
  title: string;
  spec: string;
  state: import("@junction/runtime").ProjectedState;
  seq: number;
  moves: { action: string; target?: string }[];
}
interface WirePatch {
  t: "patch";
  events: GameEvent[];
  state: import("@junction/runtime").ProjectedState;
  seq: number;
  moves: { action: string; target?: string }[];
}

/**
 * Mount an ONLINE game: the server is authoritative; this client renders projections
 * and sends chosen moves. Same view-model, same juice — different state source.
 * Reconnects with its token + lastSeq, so a dropped Chromebook resumes seamlessly.
 */
export function mountOnlineGame(container: HTMLElement, options: OnlineMountOptions): GameController {
  container.innerHTML = "";
  container.classList.add("jx-shell");
  const status = el("p", "jx-status");
  status.textContent = "Connecting…";
  container.append(status);

  let disposed = false;
  let socket: WebSocket | undefined;
  let token: string | undefined;
  let lastSeq = -1;
  let retryTimer: ReturnType<typeof setTimeout> | undefined;

  // Set after welcome (we need the spec to build the doc + theme).
  let game: {
    doc: GameDocument;
    theme: ReturnType<typeof resolveTheme>;
    motion: MotionParams;
    colors: string[];
    sound: ReturnType<typeof createSoundBank>;
    viewerSeat: number;
    statusEl: HTMLElement;
    zonesHost: HTMLElement;
    buttonsHost: HTMLElement;
    statsHost: HTMLElement;
    tickerHost: HTMLElement;
    live: HTMLElement;
    ticker: string[];
    stepEvents: readonly GameEvent[];
    matchStreak: number;
    firstRender: boolean;
  } | undefined;

  function send(message: unknown): void {
    if (socket !== undefined && socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify(message));
  }

  function scaffold(welcome: WireWelcome): void {
    const { parseGameDocument } = junctionSpec();
    const parsed = parseGameDocument(welcome.spec, { file: "<online>" });
    if (!parsed.ok) {
      status.textContent = "This room's game failed to load.";
      return;
    }
    const doc = parsed.data;
    const theme = resolveTheme(doc.spec);
    container.innerHTML = "";
    applyThemeTokens(document.documentElement, container, theme);

    const statusBar = el("div", "jx-statusbar");
    const statusEl = el("p", "jx-status");
    const sound = createSoundBank(theme.sound, readMuted());
    const soundToggle = el("button", "jx-sound");
    soundToggle.type = "button";
    const syncToggle = (): void => {
      const muted = sound.isMuted() || theme.sound === "off";
      soundToggle.textContent = muted ? "🔇" : "🔊";
      soundToggle.setAttribute("aria-label", muted ? "turn sound on" : "turn sound off");
    };
    soundToggle.addEventListener("click", () => {
      sound.setMuted(!sound.isMuted());
      storeMuted(sound.isMuted());
      syncToggle();
    });
    syncToggle();
    statusBar.append(statusEl, soundToggle);

    const statsHost = el("div", "jx-stats");
    const zonesHost = el("div", "jx-zones");
    const buttonsHost = el("div", "jx-buttons");
    const tickerHost = el("div", "jx-ticker");
    const live = el("div", "visually-hidden");
    live.setAttribute("role", "status");
    live.setAttribute("aria-live", "polite");
    container.append(statusBar, statsHost, zonesHost, buttonsHost, tickerHost, live);
    container.addEventListener("pointerdown", () => sound.unlock(), { capture: true });

    game = {
      doc,
      theme,
      motion: motionParams(theme),
      colors: celebrationColors(theme),
      sound,
      viewerSeat: welcome.seat,
      statusEl,
      zonesHost,
      buttonsHost,
      statsHost,
      tickerHost,
      live,
      ticker: [],
      stepEvents: [],
      matchStreak: 0,
      firstRender: true,
    };
  }

  function renderOnline(state: import("@junction/runtime").ProjectedState, moves: { action: string; target?: string }[]): void {
    if (game === undefined) return;
    const g = game;
    const vm = buildViewModelFromProjection(g.doc, state, moves);
    const firstRects = measureCards(g.zonesHost);
    g.statusEl.textContent = vm.statusLine;
    g.statusEl.classList.toggle("your-turn", vm.yourTurn);
    renderStats(g.statsHost, vm);
    renderZones(g.zonesHost, vm, g.doc.metadata.name, (move) => {
      g.sound.unlock();
      send({ t: "move", ...move });
    });
    renderButtons(g.buttonsHost, vm, (move) => {
      g.sound.unlock();
      send({ t: "move", ...move });
    });
    renderTicker(g.tickerHost, g.ticker);
    pulseChangedStats(g.statsHost, g.stepEvents);
    if (g.firstRender) {
      g.firstRender = false;
      playEntrance(g.zonesHost, g.motion);
    } else {
      playFlip(g.zonesHost, firstRects, g.motion);
    }
    // celebrations
    const ended = g.stepEvents.find((e) => e.type === "gameEnded");
    if (ended?.type === "gameEnded" && ended.winnerSeat === g.viewerSeat && g.theme.celebration === "festive")
      confettiBurst(g.colors);
    g.stepEvents = [];
    if (vm.ended) showBanner(container, vm);
  }

  function onMessage(raw: string): void {
    let msg: { t?: string };
    try {
      msg = JSON.parse(raw) as { t?: string };
    } catch {
      return;
    }
    if (msg.t === "welcome") {
      const welcome = msg as WireWelcome;
      token = welcome.token;
      lastSeq = welcome.seq;
      scaffold(welcome);
      renderOnline(welcome.state, welcome.moves);
    } else if (msg.t === "patch") {
      const patch = msg as WirePatch;
      lastSeq = patch.seq;
      if (game !== undefined) {
        game.stepEvents = patch.events;
        const lines = announceAll(patch.events, game.viewerSeat);
        if (lines.length > 0) {
          game.ticker = [...game.ticker, ...lines].slice(-6);
          game.live.textContent = lines[lines.length - 1]!;
        }
        for (const event of patch.events)
          if (event.type === "pairResolved" && event.bySeat === game.viewerSeat)
            game.matchStreak = event.matched ? game.matchStreak + 1 : 0;
        const names: SoundName[] = [];
        for (const event of patch.events) {
          const name = soundForEvent(event, game.viewerSeat);
          if (name !== null && !names.includes(name)) names.push(name);
        }
        const finale = names.find((n) => n === "fanfare" || n === "defeat");
        for (const name of finale !== undefined ? [finale] : names.slice(0, 2)) game.sound.play(name);
      }
      renderOnline(patch.state, patch.moves);
    } else if (msg.t === "error") {
      const err = msg as { message?: string };
      if (game === undefined) status.textContent = err.message ?? "Room error.";
    }
  }

  function connect(): void {
    if (disposed) return;
    socket = new WebSocket(options.url);
    socket.addEventListener("open", () => {
      send({ t: "join", ...(options.name !== undefined ? { name: options.name } : {}), ...(token !== undefined ? { token, lastSeq } : {}) });
    });
    socket.addEventListener("message", (event) => onMessage(String(event.data)));
    socket.addEventListener("close", () => {
      if (disposed) return;
      (game?.statusEl ?? status).textContent = "Reconnecting…";
      retryTimer = setTimeout(connect, 1200);
    });
  }
  connect();

  return {
    dispose: () => {
      disposed = true;
      if (retryTimer !== undefined) clearTimeout(retryTimer);
      socket?.close();
    },
  };
}

/** Indirection so the spec import stays at module top (bundled) without circularity. */
import { parseGameDocument as _parseGameDocument } from "@junction/spec";
function junctionSpec(): { parseGameDocument: typeof _parseGameDocument } {
  return { parseGameDocument: _parseGameDocument };
}
