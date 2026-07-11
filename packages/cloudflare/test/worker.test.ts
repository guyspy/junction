import { env } from "cloudflare:workers";
import { evictDurableObject } from "cloudflare:test";
import { afterEach, describe, expect, it } from "vitest";
import worker from "../src/index.js";

interface Frame {
  t: string;
  [key: string]: unknown;
}

const sockets: WebSocket[] = [];

afterEach(() => {
  for (const socket of sockets.splice(0)) socket.close(1000, "test complete");
});

describe("Cloudflare room Worker", () => {
  it("creates a SQLite-backed room and exposes the health contract", async () => {
    const check = await worker.fetch(new Request("https://example.com/check"), env);
    expect(check.status).toBe(200);
    await expect(check.json()).resolves.toMatchObject({ ok: true, websocket: "/ws?code=ABCDE" });

    const response = await worker.fetch(
      new Request("https://example.com/api/rooms?game=war", { method: "POST" }),
      env,
    );
    expect(response.status).toBe(201);
    const created = await response.json<{ code: string; title: string; seats: number }>();
    expect(created.code).toMatch(/^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{5}$/);
    expect(created.title).toContain("War");
    expect(created.seats).toBe(2);

    const info = await env.ROOMS.getByName(created.code).info();
    expect(info).toMatchObject({ initialized: true, status: "running", activeSeat: 0 });
  });

  it("keeps projections, state, and live sockets valid across Durable Object eviction", async () => {
    const createdResponse = await worker.fetch(
      new Request("https://example.com/api/rooms?game=war", { method: "POST" }),
      env,
    );
    const { code } = await createdResponse.json<{ code: string }>();
    const stub = env.ROOMS.getByName(code);

    const first = await openSocket(stub);
    const firstWelcomePromise = nextFrame(first);
    first.send(JSON.stringify({ t: "join", name: "First" }));
    const firstWelcome = await firstWelcomePromise;
    expect(firstWelcome.t).toBe("welcome");
    expect(firstWelcome.seat).toBe(0);
    expect(deckEntries(firstWelcome).every((entry) => "hidden" in entry)).toBe(true);

    // A second player may arrive after the first connection has already hibernated.
    await evictDurableObject(stub);

    const second = await openSocket(stub);
    const secondWelcomePromise = nextFrame(second);
    second.send(JSON.stringify({ t: "join", name: "Second" }));
    const secondWelcome = await secondWelcomePromise;
    expect(secondWelcome.t).toBe("welcome");
    expect(secondWelcome.seat).toBe(1);

    const firstPatchPromise = nextFrame(first, "patch");
    const secondPatchPromise = nextFrame(second, "patch");
    first.send(JSON.stringify({ t: "move", action: "play-card" }));
    const [firstPatch, secondPatch] = await Promise.all([firstPatchPromise, secondPatchPromise]);
    expect(firstPatch.seq).toBe(secondPatch.seq);
    expect(Number(firstPatch.seq)).toBeGreaterThan(Number(firstWelcome.seq));
    expect(deckEntries(firstPatch).every((entry) => "hidden" in entry)).toBe(true);

    const seq = Number(firstPatch.seq);
    await evictDurableObject(stub);
    await expect(stub.info()).resolves.toMatchObject({ initialized: true, seq });

    const pongPromise = nextFrame(first, "pong");
    first.send(JSON.stringify({ t: "ping" }));
    await expect(pongPromise).resolves.toMatchObject({ t: "pong" });

    const token = String(firstWelcome.token);
    first.close(1000, "reconnect test");
    const reconnected = await openSocket(stub);
    const resumedPromise = nextFrame(reconnected, "welcome");
    reconnected.send(JSON.stringify({ t: "join", token, lastSeq: seq }));
    await expect(resumedPromise).resolves.toMatchObject({ t: "welcome", seat: 0, token, seq });
  });
});

async function openSocket(stub: DurableObjectStub<import("../src/index.js").GameRoom>): Promise<WebSocket> {
  const response = await stub.fetch("https://example.com/ws", { headers: { Upgrade: "websocket" } });
  const socket = response.webSocket;
  if (socket === null) throw new Error("expected a WebSocket response");
  socket.accept();
  sockets.push(socket);
  return socket;
}

function nextFrame(socket: WebSocket, wanted?: string): Promise<Frame> {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error(`timed out waiting for ${wanted ?? "frame"}`)), 3000);
    const onMessage = (event: MessageEvent): void => {
      const frame = JSON.parse(String(event.data)) as Frame;
      if (wanted !== undefined && frame.t !== wanted) return;
      clearTimeout(timeout);
      socket.removeEventListener("message", onMessage);
      resolve(frame);
    };
    socket.addEventListener("message", onMessage);
  });
}

function deckEntries(frame: Frame): Record<string, unknown>[] {
  const state = frame.state as { zones?: { zone: string; entries: Record<string, unknown>[] }[] } | undefined;
  return state?.zones?.find((zone) => zone.zone === "deck")?.entries ?? [];
}
