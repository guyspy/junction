import type { GameDocument } from "@junction/spec";
import { Room, type RoomHooks } from "./room.js";
import { makeJoinCode } from "./join-code.js";
import type { ClientMessage } from "../protocol/messages.js";

/**
 * RoomManager — the classroom directory: open a room (teacher), get a join code,
 * students connect by code. Platform-agnostic; a Durable Object holds one Room and
 * a tiny KV maps code→DO id, while a Node server can hold the whole manager in memory.
 * Randomness and the clock are injected (no Math.random/Date in core).
 */

export interface OpenRoomInput {
  readonly doc: GameDocument;
  readonly yaml: string;
  readonly seatCount: number;
}

export interface ManagerHooks {
  /** Per-connection send sink, shared by every room. */
  readonly send: (connId: string, text: string) => void;
  readonly now: () => number;
  /** Uniform int in [0, max). The code/seed source. */
  readonly randomInt: (maxExclusive: number) => number;
  readonly makeToken?: () => string;
  readonly close?: (connId: string, code: number, reason: string) => void;
  /** Optional bot scheduler passed through to rooms (synchronous in tests). */
  readonly schedule?: (fn: () => void) => void;
}

export class RoomManager {
  private readonly hooks: ManagerHooks;
  private readonly rooms = new Map<string, Room>();
  /** Which room each connection is attached to (for routing handle/disconnect). */
  private readonly connRoom = new Map<string, string>();

  constructor(hooks: ManagerHooks) {
    this.hooks = hooks;
  }

  /** Open a fresh room; returns its unique join code. */
  open(input: OpenRoomInput): string {
    let code = makeJoinCode(this.hooks.randomInt);
    let guard = 0;
    while (this.rooms.has(code) && guard++ < 50) code = makeJoinCode(this.hooks.randomInt);
    const roomHooks: RoomHooks = {
      send: this.hooks.send,
      now: this.hooks.now,
      seed: `${code}:${this.hooks.now()}`,
      ...(this.hooks.makeToken !== undefined ? { makeToken: this.hooks.makeToken } : {}),
      ...(this.hooks.close !== undefined ? { close: this.hooks.close } : {}),
    };
    const room = new Room({
      doc: input.doc,
      yaml: input.yaml,
      seatCount: input.seatCount,
      hooks: roomHooks,
      ...(this.hooks.schedule !== undefined ? { schedule: this.hooks.schedule } : {}),
    });
    this.rooms.set(code, room);
    return code;
  }

  has(code: string): boolean {
    return this.rooms.has(code);
  }

  /** A connection joins the room behind `code`. Returns false if the code is unknown. */
  connect(code: string, connId: string): boolean {
    const room = this.rooms.get(code);
    if (room === undefined) return false;
    this.connRoom.set(connId, code);
    room.connect(connId);
    return true;
  }

  handle(connId: string, message: ClientMessage): void {
    this.roomFor(connId)?.handle(connId, message);
  }

  disconnect(connId: string): void {
    this.roomFor(connId)?.disconnect(connId);
    this.connRoom.delete(connId);
  }

  /** Close a finished/abandoned room and free its code. */
  close(code: string): void {
    this.rooms.get(code)?.dispose();
    this.rooms.delete(code);
  }

  private roomFor(connId: string): Room | undefined {
    const code = this.connRoom.get(connId);
    return code === undefined ? undefined : this.rooms.get(code);
  }

  get openRoomCount(): number {
    return this.rooms.size;
  }
}
