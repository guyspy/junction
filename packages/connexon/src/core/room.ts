import type { GameDocument } from "@junction/spec";
import {
  applyAction,
  applySkip,
  buildInitialState,
  createRng,
  legalMoves,
  projectState,
  randomChooser,
  type GameEvent,
  type GameState,
  type Rng,
} from "@junction/runtime";
import { encode } from "../protocol/messages.js";

/**
 * The Room — Connexon's platform-agnostic heart. It owns authoritative game state and
 * the append-only event log, assigns seats, projects per seat (hidden information never
 * leaves the server), and fans out ordered patches. It speaks to the outside world only
 * through an injected `send(connId, text)` sink + a `now()` clock, so the very same code
 * runs on a Durable Object, a Node `ws` server, or entirely in a test.
 *
 * Bots fill any seat no human holds, and step automatically — so one kid can start a game
 * alone and a late joiner simply takes over a bot's seat.
 */

export interface RoomHooks {
  /** Deliver a frame to one connection (transport-specific). */
  readonly send: (connId: string, text: string) => void;
  /** Monotonic ms clock (DI'd so the core stays pure/replayable). */
  readonly now: () => number;
  /** Seeds for setup, bots, and dice. Pass a fixed seed in tests. */
  readonly seed: string;
}

interface Seat {
  /** The connection currently holding this seat, or null (open → bot-driven). */
  connId: string | null;
  /** Reconnect token; the same human can resume this seat. */
  token: string;
  name: string;
}

interface Conn {
  readonly id: string;
  seat: number;
}

const BOT_DELAY_MS = 700;

export interface RoomConfig {
  readonly doc: GameDocument;
  /** The GameSpec YAML, delivered in `welcome` so clients mount the renderer with no extra fetch. */
  readonly yaml: string;
  readonly seatCount: number;
  readonly hooks: RoomHooks;
  /** Injectable bot scheduler; pass a synchronous one in tests. */
  readonly schedule?: (fn: () => void) => void;
}

export class Room {
  private readonly doc: GameDocument;
  private readonly yaml: string;
  private readonly hooks: RoomHooks;
  private readonly seatCount: number;
  private readonly botRng: Rng;
  private readonly diceRng: Rng;
  private state: GameState;
  /** The full ordered log — the source of truth for resume. */
  private readonly log: GameEvent[] = [];
  private readonly seats: Seat[];
  private readonly conns = new Map<string, Conn>();
  private tokenCounter = 0;
  private botTimer: ReturnType<typeof setTimeout> | undefined;
  /** Injectable so tests can run bots synchronously. */
  private readonly schedule: (fn: () => void) => void;

  constructor(config: RoomConfig) {
    this.doc = config.doc;
    this.yaml = config.yaml;
    this.hooks = config.hooks;
    this.seatCount = config.seatCount;
    this.botRng = createRng(`${config.hooks.seed}:bot`);
    this.diceRng = createRng(`${config.hooks.seed}:dice`);
    this.schedule = config.schedule ?? ((fn) => { this.botTimer = setTimeout(fn, BOT_DELAY_MS); });

    const setup = buildInitialState(this.doc, this.seatCount, createRng(`${config.hooks.seed}:setup`));
    this.state = setup.state;
    this.log.push(...setup.events);
    this.seats = Array.from({ length: this.seatCount }, (_, i) => ({
      connId: null,
      token: `seat${i}-${(this.tokenCounter++).toString(36)}`,
      name: `Player ${i + 1}`,
    }));
    // No maybeStepBots() here — the table waits for its first human (see onJoin).
  }

  /** A new transport connection arrived (not yet seated). */
  connect(connId: string): void {
    this.conns.set(connId, { id: connId, seat: -1 });
  }

  disconnect(connId: string): void {
    const conn = this.conns.get(connId);
    if (conn !== undefined && conn.seat >= 0) {
      const seat = this.seats[conn.seat];
      if (seat !== undefined && seat.connId === connId) seat.connId = null; // seat reverts to bot
    }
    this.conns.delete(connId);
    this.broadcastRoomInfo();
    this.maybeStepBots();
  }

  /** Handle one parsed client message. Returns nothing; effects go through `send`. */
  handle(connId: string, message: { t: string; [k: string]: unknown }): void {
    switch (message.t) {
      case "join":
        this.onJoin(connId, message as { token?: string; name?: string; lastSeq?: number });
        return;
      case "move": {
        const action = message["action"];
        if (typeof action !== "string") return;
        const target = message["target"];
        this.onMove(connId, { action, ...(typeof target === "string" ? { target } : {}) });
        return;
      }
      case "ping":
        this.hooks.send(connId, encode({ t: "pong" }));
        return;
    }
  }

  private onJoin(connId: string, msg: { token?: string; name?: string; lastSeq?: number }): void {
    let seatIndex = msg.token !== undefined ? this.seats.findIndex((s) => s.token === msg.token) : -1;
    if (seatIndex === -1) seatIndex = this.seats.findIndex((s) => s.connId === null);
    if (seatIndex === -1) {
      this.hooks.send(connId, encode({ t: "error", code: "ROOM_FULL", message: "All seats are taken." }));
      return;
    }
    const seat = this.seats[seatIndex]!;
    seat.connId = connId;
    if (msg.name !== undefined && msg.name.trim() !== "") seat.name = msg.name.trim().slice(0, 24);
    const conn = this.conns.get(connId);
    if (conn !== undefined) conn.seat = seatIndex;

    this.hooks.send(
      connId,
      encode({
        t: "welcome",
        seat: seatIndex,
        seats: this.seatCount,
        token: seat.token,
        game: this.doc.metadata.name,
        title: this.doc.spec.meta.title,
        spec: this.yaml,
        state: projectState(this.state, this.doc.spec, seatIndex),
        seq: this.lastSeq,
      }),
    );
    // Resume: replay any events the client missed (lastSeq < current).
    if (msg.lastSeq !== undefined && msg.lastSeq < this.lastSeq) {
      const missed = this.log.filter((e) => e.seq > msg.lastSeq!);
      if (missed.length > 0) this.sendPatch(connId, seatIndex, missed);
    }
    this.broadcastRoomInfo();
    this.maybeStepBots();
  }

  private onMove(connId: string, msg: { action: string; target?: string }): void {
    const conn = this.conns.get(connId);
    if (conn === undefined || conn.seat < 0) return;
    if (this.state.status !== "running" || this.state.activeSeat !== conn.seat) {
      this.hooks.send(connId, encode({ t: "error", code: "NOT_YOUR_TURN", message: "It is not your turn." }));
      return;
    }
    const result = applyAction(this.state, this.doc.spec, { seat: conn.seat, action: msg.action, target: msg.target }, this.diceRng);
    if (!result.ok) {
      this.hooks.send(connId, encode({ t: "error", code: result.diagnostics[0]?.code ?? "ILLEGAL", message: result.diagnostics[0]?.message ?? "Illegal move." }));
      return;
    }
    this.commit(result.data.state, result.data.events);
    this.maybeStepBots();
  }

  // ---- engine plumbing -------------------------------------------------------

  private get lastSeq(): number {
    return this.state.seq - 1;
  }

  private commit(next: GameState, events: readonly GameEvent[]): void {
    this.state = next;
    this.log.push(...events);
    for (const conn of this.conns.values())
      if (conn.seat >= 0) this.sendPatch(conn.id, conn.seat, events);
    this.broadcastRoomInfo();
  }

  private sendPatch(connId: string, seat: number, events: readonly GameEvent[]): void {
    this.hooks.send(
      connId,
      encode({
        t: "patch",
        events: events.map((e) => e), // v1alpha reveals are public; per-seat event filtering lands with private reveals
        state: projectState(this.state, this.doc.spec, seat),
        seq: this.lastSeq,
      }),
    );
  }

  private broadcastRoomInfo(): void {
    const seatsFilled = this.seats.filter((s) => s.connId !== null).length;
    const status = this.state.status === "ended" ? "ended" : seatsFilled === 0 ? "waiting" : "playing";
    const info = encode({ t: "room", seatsFilled, seats: this.seatCount, status });
    for (const conn of this.conns.values()) this.hooks.send(conn.id, info);
  }

  /** Drive bots: while the active seat is bot-held (no connection) and the game runs. */
  private maybeStepBots(): void {
    if (this.botTimer !== undefined) {
      clearTimeout(this.botTimer);
      this.botTimer = undefined;
    }
    if (this.state.status !== "running") return;
    // Don't burn a game before anyone is watching — the table waits for its first human.
    if (this.seats.every((s) => s.connId === null)) return;
    const activeSeatIsBot = this.seats[this.state.activeSeat]?.connId === null;
    if (!activeSeatIsBot) {
      // Still auto-skip a human with no legal move so the table never deadlocks.
      if (legalMoves(this.state, this.doc.spec).length === 0) {
        const step = applySkip(this.state, this.doc.spec);
        this.commit(step.state, step.events);
        this.maybeStepBots();
      }
      return;
    }
    this.schedule(() => this.stepOneBot());
  }

  private stepOneBot(): void {
    this.botTimer = undefined;
    if (this.state.status !== "running") return;
    if (this.seats[this.state.activeSeat]?.connId !== null) return; // a human took the seat
    const legal = legalMoves(this.state, this.doc.spec);
    if (legal.length === 0) {
      const step = applySkip(this.state, this.doc.spec);
      this.commit(step.state, step.events);
    } else {
      const move = randomChooser(legal, this.botRng);
      const result = applyAction(this.state, this.doc.spec, { seat: this.state.activeSeat, ...move }, this.diceRng);
      if (!result.ok) return;
      this.commit(result.data.state, result.data.events);
    }
    this.maybeStepBots();
  }

  /** Test/inspection hook. */
  get snapshot(): { status: string; activeSeat: number; seq: number; logLength: number } {
    return { status: this.state.status, activeSeat: this.state.activeSeat, seq: this.lastSeq, logLength: this.log.length };
  }

  dispose(): void {
    if (this.botTimer !== undefined) clearTimeout(this.botTimer);
  }
}
