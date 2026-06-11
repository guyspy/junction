import { createServer, type IncomingMessage, type Server } from "node:http";
import { randomInt } from "node:crypto";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { WebSocketServer, type WebSocket } from "ws";
import type { GameDocument } from "@junction/spec";
import { RoomManager, parseClientMessage } from "@junction/connexon";
import { buildOnlinePageHtml, computeQaBadges } from "@junction/renderer";

/**
 * The Node room server — Connexon's portability hedge (blueprint §9) and the local
 * classroom host. One process serves: the play page (online client), `/ws?code=`
 * websockets into rooms, and `/check` (the school-IT self-test). The same Room core
 * will run on Durable Objects; this adapter exists so CI and a €4 VPS can run it too.
 */

export interface ServeOptions {
  readonly doc: GameDocument;
  readonly yaml: string;
  readonly seats?: number;
  readonly port?: number;
  readonly host?: string;
}

export interface RunningServer {
  readonly port: number;
  readonly code: string;
  readonly url: string;
  readonly close: () => Promise<void>;
}

function loadBundle(): string {
  const require = createRequire(import.meta.url);
  return readFileSync(require.resolve("@junction/renderer/standalone.js"), "utf8");
}

export async function startNodeServer(options: ServeOptions): Promise<RunningServer> {
  const seats = options.seats ?? options.doc.spec.meta.seats.min;
  const host = options.host ?? "127.0.0.1";

  // The play page is built once; badges simulated up front.
  const badges = computeQaBadges(options.doc, { games: 200, seed: "serve" });
  const page = buildOnlinePageHtml({ title: options.doc.spec.meta.title, badges, bundleJs: loadBundle() });

  let connCounter = 0;
  const sockets = new Map<string, WebSocket>();
  const manager = new RoomManager({
    send: (connId, text) => {
      const ws = sockets.get(connId);
      if (ws !== undefined && ws.readyState === ws.OPEN) ws.send(text);
    },
    now: () => Date.now(),
    randomInt: (max) => randomInt(max),
  });
  const code = manager.open({ doc: options.doc, yaml: options.yaml, seatCount: seats });

  const http: Server = createServer((req, res) => {
    const url = new URL(req.url ?? "/", "http://local");
    if (url.pathname === "/" || url.pathname === "/play") {
      res.writeHead(200, { "content-type": "text/html; charset=utf-8" });
      res.end(page);
      return;
    }
    if (url.pathname === "/check") {
      res.writeHead(200, { "content-type": "application/json" });
      res.end(JSON.stringify({ ok: true, websocket: "/ws", rooms: manager.openRoomCount }));
      return;
    }
    res.writeHead(404, { "content-type": "text/plain" });
    res.end("not found");
  });

  const wss = new WebSocketServer({ noServer: true });
  http.on("upgrade", (req: IncomingMessage, socket, head) => {
    const url = new URL(req.url ?? "/", "http://local");
    if (url.pathname !== "/ws") {
      socket.destroy();
      return;
    }
    const roomCode = (url.searchParams.get("code") ?? "").toUpperCase();
    wss.handleUpgrade(req, socket, head, (ws) => {
      const connId = `c${connCounter++}`;
      if (!manager.connect(roomCode, connId)) {
        ws.send(JSON.stringify({ t: "error", code: "ROOM_NOT_FOUND", message: `No room '${roomCode}'.` }));
        ws.close();
        return;
      }
      sockets.set(connId, ws);
      ws.on("message", (data) => {
        const message = parseClientMessage(String(data));
        if (message !== null) manager.handle(connId, message as unknown as { t: string; [k: string]: unknown });
      });
      ws.on("close", () => {
        sockets.delete(connId);
        manager.disconnect(connId);
      });
    });
  });

  await new Promise<void>((resolve) => http.listen(options.port ?? 0, host, resolve));
  const address = http.address();
  const port = typeof address === "object" && address !== null ? address.port : (options.port ?? 0);

  return {
    port,
    code,
    url: `http://${host}:${port}/?code=${code}`,
    close: async () => {
      manager.close(code);
      wss.close();
      await new Promise<void>((resolve) => http.close(() => resolve()));
    },
  };
}
