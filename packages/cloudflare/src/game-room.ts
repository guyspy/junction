import { DurableObject } from "cloudflare:workers";
import { Room, parseClientMessage, type RoomSnapshot } from "@junction/rooms";
import { parseGameDocument, type GameDocument } from "@junction/spec";

interface StoredRoom {
  [key: string]: string | number;
  yaml: string;
  seat_count: number;
  seed: string;
  snapshot: string;
}

interface SocketAttachment {
  connId: string;
  token?: string;
}

export interface InitializeResult {
  readonly created: boolean;
  readonly title?: string;
  readonly seats?: number;
  readonly error?: string;
}

export interface RoomInfo {
  readonly initialized: boolean;
  readonly title?: string;
  readonly status?: string;
  readonly activeSeat?: number;
  readonly seq?: number;
}

export class GameRoom extends DurableObject<Env> {
  private room: Room | undefined;
  private title: string | undefined;

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    ctx.blockConcurrencyWhile(async () => {
      this.ctx.storage.sql.exec(`
        CREATE TABLE IF NOT EXISTS room (
          id INTEGER PRIMARY KEY CHECK (id = 1),
          yaml TEXT NOT NULL,
          seat_count INTEGER NOT NULL,
          seed TEXT NOT NULL,
          snapshot TEXT NOT NULL
        )
      `);
      this.restoreFromStorage();
    });
  }

  async initialize(yaml: string, requestedSeats: number | null, seed: string): Promise<InitializeResult> {
    if (this.room !== undefined) return { created: false };

    const parsed = parseGameDocument(yaml, { file: "<cloudflare-room>" });
    if (!parsed.ok)
      return { created: false, error: parsed.diagnostics[0]?.message ?? "The game is invalid." };

    const seats = requestedSeats ?? parsed.data.spec.meta.seats.min;
    if (
      !Number.isInteger(seats) ||
      seats < parsed.data.spec.meta.seats.min ||
      seats > parsed.data.spec.meta.seats.max
    )
      return {
        created: false,
        error: `Seats must be between ${parsed.data.spec.meta.seats.min} and ${parsed.data.spec.meta.seats.max}.`,
      };

    const room = this.attachRoom(parsed.data, yaml, seats, seed);
    this.persist(yaml, seats, seed, room.exportSnapshot());
    return { created: true, title: parsed.data.spec.meta.title, seats };
  }

  async info(): Promise<RoomInfo> {
    if (this.room === undefined) return { initialized: false };
    return { initialized: true, title: this.title, ...this.room.snapshot };
  }

  async fetch(request: Request): Promise<Response> {
    if (this.room === undefined) return Response.json({ error: "Room not found." }, { status: 404 });
    if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket")
      return Response.json({ error: "Expected a WebSocket upgrade." }, { status: 426 });

    const pair = new WebSocketPair();
    const client = pair[0];
    const server = pair[1];
    const attachment: SocketAttachment = { connId: crypto.randomUUID() };
    server.serializeAttachment(attachment);
    this.ctx.acceptWebSocket(server);
    this.room.connect(attachment.connId);
    return new Response(null, { status: 101, webSocket: client });
  }

  async webSocketMessage(ws: WebSocket, message: string | ArrayBuffer): Promise<void> {
    const attachment = readAttachment(ws);
    if (attachment === null || this.room === undefined) {
      ws.send(JSON.stringify({ t: "error", code: "ROOM_STATE", message: "Room state is unavailable." }));
      return;
    }
    if (typeof message !== "string") {
      ws.send(JSON.stringify({ t: "error", code: "BAD_MESSAGE", message: "Text JSON frames are required." }));
      return;
    }
    const parsed = parseClientMessage(message);
    if (parsed === null) {
      ws.send(JSON.stringify({ t: "error", code: "BAD_MESSAGE", message: "Invalid room message." }));
      return;
    }

    this.room.restoreConnection(attachment.connId, attachment.token);
    this.room.handle(attachment.connId, parsed);
    const token = this.room.connectionToken(attachment.connId);
    ws.serializeAttachment({ connId: attachment.connId, ...(token === undefined ? {} : { token }) });
  }

  async webSocketClose(ws: WebSocket): Promise<void> {
    const attachment = readAttachment(ws);
    if (attachment !== null) this.room?.disconnect(attachment.connId);
  }

  async webSocketError(ws: WebSocket): Promise<void> {
    const attachment = readAttachment(ws);
    if (attachment !== null) this.room?.disconnect(attachment.connId);
  }

  private restoreFromStorage(): void {
    const stored = this.ctx.storage.sql.exec<StoredRoom>(
      "SELECT yaml, seat_count, seed, snapshot FROM room WHERE id = 1",
    ).toArray()[0];
    if (stored === undefined) return;

    const parsed = parseGameDocument(stored.yaml, { file: "<cloudflare-storage>" });
    if (!parsed.ok) throw new Error("stored room contains an invalid GameSpec");
    const value: unknown = JSON.parse(stored.snapshot);
    if (!isRoomSnapshot(value)) throw new Error("stored room snapshot is invalid");
    this.attachRoom(parsed.data, stored.yaml, stored.seat_count, stored.seed, value);
  }

  private attachRoom(
    doc: GameDocument,
    yaml: string,
    seats: number,
    seed: string,
    snapshot?: RoomSnapshot,
  ): Room {
    this.title = doc.spec.meta.title;
    const room = new Room({
      doc,
      yaml,
      seatCount: seats,
      hooks: {
        send: (connId, text) => this.socketFor(connId)?.send(text),
        now: () => Date.now(),
        seed,
        makeToken: () => crypto.randomUUID(),
        close: (connId, code, reason) => this.socketFor(connId)?.close(code, reason),
      },
      bots: false,
      ...(snapshot === undefined ? {} : { snapshot }),
      onChange: (next) => this.persist(yaml, seats, seed, next),
    });
    this.room = room;

    for (const ws of this.ctx.getWebSockets()) {
      const attachment = readAttachment(ws);
      if (attachment !== null) room.restoreConnection(attachment.connId, attachment.token);
    }
    return room;
  }

  private persist(yaml: string, seats: number, seed: string, snapshot: RoomSnapshot): void {
    this.ctx.storage.sql.exec(
      `INSERT INTO room (id, yaml, seat_count, seed, snapshot)
       VALUES (1, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         yaml = excluded.yaml,
         seat_count = excluded.seat_count,
         seed = excluded.seed,
         snapshot = excluded.snapshot`,
      yaml,
      seats,
      seed,
      JSON.stringify(snapshot),
    );
  }

  private socketFor(connId: string): WebSocket | undefined {
    return this.ctx.getWebSockets().find((ws) => readAttachment(ws)?.connId === connId);
  }
}

function readAttachment(ws: WebSocket): SocketAttachment | null {
  const value: unknown = ws.deserializeAttachment();
  if (typeof value !== "object" || value === null) return null;
  const attachment = value as Record<string, unknown>;
  if (typeof attachment["connId"] !== "string") return null;
  return {
    connId: attachment["connId"],
    ...(typeof attachment["token"] === "string" ? { token: attachment["token"] } : {}),
  };
}

function isRoomSnapshot(value: unknown): value is RoomSnapshot {
  if (typeof value !== "object" || value === null) return false;
  const snapshot = value as Record<string, unknown>;
  return (
    typeof snapshot["state"] === "object" &&
    snapshot["state"] !== null &&
    Array.isArray(snapshot["log"]) &&
    Array.isArray(snapshot["seats"]) &&
    snapshot["seats"].every(
      (seat) =>
        typeof seat === "object" &&
        seat !== null &&
        typeof (seat as Record<string, unknown>)["token"] === "string" &&
        typeof (seat as Record<string, unknown>)["name"] === "string",
    ) &&
    typeof snapshot["tokenCounter"] === "number"
  );
}
