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
} from "@junction/runtime";
import { encode, type ClientMessage } from "../protocol/messages.js";
import { HandleCodec } from "./handles.js";

/**
 * A Room owns authoritative game state and
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
  /** Cryptographically secure in transports; deterministic fallback is for tests. */
  readonly makeToken?: () => string;
  /** Terminate a superseded transport connection after token-based resume. */
  readonly close?: (connId: string, code: number, reason: string) => void;
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

export interface RoomSnapshot {
  readonly state: GameState;
  readonly log: readonly GameEvent[];
  readonly seats: readonly { readonly token: string; readonly name: string }[];
  readonly tokenCounter: number;
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
  /** Restore authoritative state after a process restart or Durable Object eviction. */
  readonly snapshot?: RoomSnapshot;
  /** Cloudflare rooms wait for humans; the local Node host defaults to bot-filled seats. */
  readonly bots?: boolean;
  /** Called synchronously after authoritative state changes. */
  readonly onChange?: (snapshot: RoomSnapshot) => void;
}

export class Room {
  private readonly doc: GameDocument;
  private readonly yaml: string;
  private readonly hooks: RoomHooks;
  private readonly seed: string;
  private readonly seatCount: number;
  private readonly bots: boolean;
  private readonly onChange: ((snapshot: RoomSnapshot) => void) | undefined;
  private state: GameState;
  /** The full ordered log — the source of truth for resume. */
  private readonly log: GameEvent[] = [];
  private readonly seats: Seat[];
  private readonly conns = new Map<string, Conn>();
  private tokenCounter = 0;
  private botTimer: ReturnType<typeof setTimeout> | undefined;
  /** Server-secret pieceId↔handle mapping — nothing creation-ordered crosses the wire. */
  private readonly codec: HandleCodec;
  /** Injectable so tests can run bots synchronously. */
  private readonly schedule: (fn: () => void) => void;

  constructor(config: RoomConfig) {
    this.doc = config.doc;
    this.yaml = config.yaml;
    this.hooks = config.hooks;
    this.seed = config.hooks.seed;
    this.seatCount = config.seatCount;
    this.bots = config.bots ?? true;
    this.onChange = config.onChange;
    this.schedule = config.schedule ?? ((fn) => { this.botTimer = setTimeout(fn, BOT_DELAY_MS); });

    if (config.snapshot === undefined) {
      const setup = buildInitialState(this.doc, this.seatCount, createRng(`${this.seed}:setup`));
      this.state = setup.state;
      this.log.push(...setup.events);
      this.seats = Array.from({ length: this.seatCount }, (_, i) => ({
        connId: null,
        token: config.hooks.makeToken?.() ?? `seat${i}-${(this.tokenCounter++).toString(36)}`,
        name: `Player ${i + 1}`,
      }));
    } else {
      if (config.snapshot.seats.length !== this.seatCount)
        throw new Error("room snapshot seat count does not match room configuration");
      this.state = config.snapshot.state;
      this.log.push(...config.snapshot.log);
      this.tokenCounter = config.snapshot.tokenCounter;
      this.seats = config.snapshot.seats.map((seat) => ({ ...seat, connId: null }));
    }
    this.codec = new HandleCodec(Object.keys(this.state.pieces), `${config.hooks.seed}:handles`);
    // No maybeStepBots() here — the table waits for its first human (see onJoin).
  }

  /** A new transport connection arrived (not yet seated). */
  connect(connId: string): void {
    this.conns.set(connId, { id: connId, seat: -1 });
  }

  /** Reattach a hibernating transport connection without emitting a second welcome. */
  restoreConnection(connId: string, token: string | undefined): boolean {
    const existing = this.conns.get(connId);
    if (existing?.seat === -2) return false;
    if (existing === undefined) this.connect(connId);
    if (token === undefined) return false;
    const seatIndex = this.seats.findIndex((seat) => seat.token === token);
    if (seatIndex === -1) return false;
    this.assignSeat(connId, seatIndex);
    return true;
  }

  connectionToken(connId: string): string | undefined {
    const seat = this.conns.get(connId)?.seat ?? -1;
    return seat < 0 ? undefined : this.seats[seat]?.token;
  }

  private assignSeat(connId: string, seatIndex: number): void {
    const conn = this.conns.get(connId);
    if (conn === undefined) return;

    if (conn.seat >= 0 && conn.seat !== seatIndex) {
      const oldSeat = this.seats[conn.seat];
      if (oldSeat?.connId === connId) oldSeat.connId = null;
    }

    const seat = this.seats[seatIndex]!;
    const replacedConnId = seat.connId;
    if (replacedConnId !== null && replacedConnId !== connId) {
      const replaced = this.conns.get(replacedConnId);
      if (replaced !== undefined) replaced.seat = -2;
      this.hooks.close?.(replacedConnId, 4001, "Seat resumed elsewhere");
    }
    seat.connId = connId;
    conn.seat = seatIndex;
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
  handle(connId: string, message: ClientMessage): void {
    switch (message.t) {
      case "join":
        this.onJoin(connId, message);
        return;
      case "move": {
        this.onMove(connId, message);
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
    this.assignSeat(connId, seatIndex);
    if (msg.name !== undefined && msg.name.trim() !== "") {
      const name = msg.name.trim().slice(0, 24);
      if (name !== seat.name) {
        seat.name = name;
        this.notifyChange();
      }
    }
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
        state: this.codec.translateState(projectState(this.state, this.doc.spec, seatIndex)),
        seq: this.lastSeq,
        moves: this.movesFor(seatIndex),
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
    if (conn === undefined) return;
    if (conn.seat === -2) {
      this.hooks.send(connId, encode({ t: "error", code: "SESSION_REPLACED", message: "This seat resumed elsewhere." }));
      return;
    }
    if (conn.seat < 0) return;
    if (this.seats[conn.seat]?.connId !== connId) {
      this.hooks.send(connId, encode({ t: "error", code: "SESSION_REPLACED", message: "This seat resumed elsewhere." }));
      return;
    }
    if (this.state.status !== "running" || this.state.activeSeat !== conn.seat) {
      this.hooks.send(connId, encode({ t: "error", code: "NOT_YOUR_TURN", message: "It is not your turn." }));
      return;
    }
    const target = msg.target === undefined ? undefined : this.codec.fromWire(msg.target);
    const diceRng = createRng(`${this.seed}:dice:${this.state.seq}`);
    const result = applyAction(this.state, this.doc.spec, { seat: conn.seat, action: msg.action, ...(target !== undefined ? { target } : {}) }, diceRng);
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
    this.notifyChange();
    for (const conn of this.conns.values())
      if (conn.seat >= 0) this.sendPatch(conn.id, conn.seat, events);
    this.broadcastRoomInfo();
  }

  private sendPatch(connId: string, seat: number, events: readonly GameEvent[]): void {
    this.hooks.send(
      connId,
      encode({
        t: "patch",
        // v1alpha reveals are public, so all seats get the same (handle-translated) events;
        // per-seat event filtering lands with private reveals.
        events: events.map((e) => this.codec.translateEvent(e)),
        state: this.codec.translateState(projectState(this.state, this.doc.spec, seat)),
        seq: this.lastSeq,
        moves: this.movesFor(seat),
      }),
    );
  }

  /** Server-sent options: this seat's legal moves right now (handle-translated). */
  private movesFor(seat: number): readonly import("@junction/runtime").PlayerMove[] {
    if (this.state.status !== "running" || this.state.activeSeat !== seat) return [];
    return this.codec.translateMoves(legalMoves(this.state, this.doc.spec));
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
    if (!this.bots) return;
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
      const move = randomChooser(legal, createRng(`${this.seed}:bot:${this.state.seq}`));
      const result = applyAction(
        this.state,
        this.doc.spec,
        { seat: this.state.activeSeat, ...move },
        createRng(`${this.seed}:dice:${this.state.seq}`),
      );
      if (!result.ok) return;
      this.commit(result.data.state, result.data.events);
    }
    this.maybeStepBots();
  }

  /** Test/inspection hook. */
  get snapshot(): { status: string; activeSeat: number; seq: number; logLength: number } {
    return { status: this.state.status, activeSeat: this.state.activeSeat, seq: this.lastSeq, logLength: this.log.length };
  }

  exportSnapshot(): RoomSnapshot {
    return {
      state: this.state,
      log: this.log,
      seats: this.seats.map(({ token, name }) => ({ token, name })),
      tokenCounter: this.tokenCounter,
    };
  }

  private notifyChange(): void {
    this.onChange?.(this.exportSnapshot());
  }

  dispose(): void {
    if (this.botTimer !== undefined) clearTimeout(this.botTimer);
  }
}
