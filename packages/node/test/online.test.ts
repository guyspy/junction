import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import WebSocket from "ws";
import { parseGameDocument, type GameDocument } from "@junction/spec";
import { afterAll, describe, expect, it } from "vitest";
import { startNodeServer, type RunningServer } from "../src/index.js";

/**
 * The E3 acceptance test: two REAL WebSocket clients play a full game of War through
 * the authoritative server — seat assignment, server-sent options, handle-translated
 * hidden information, ordered seqs, and a clean ending on both sides.
 */

function loadWar(): { doc: GameDocument; yaml: string } {
  const yaml = readFileSync(fileURLToPath(new URL("../../../games/war.yaml", import.meta.url)), "utf8");
  const parsed = parseGameDocument(yaml);
  if (!parsed.ok) throw new Error("war invalid");
  return { doc: parsed.data, yaml };
}

interface Frame {
  t: string;
  [k: string]: unknown;
}

/** A tiny test client: buffers frames, lets us await the next one matching a predicate. */
function client(url: string): {
  send: (m: unknown) => void;
  next: (match: (f: Frame) => boolean, timeoutMs?: number) => Promise<Frame>;
  frames: Frame[];
  close: () => void;
} {
  const ws = new WebSocket(url);
  const frames: Frame[] = [];
  const waiters: { match: (f: Frame) => boolean; resolve: (f: Frame) => void }[] = [];
  ws.on("message", (data) => {
    const frame = JSON.parse(String(data)) as Frame;
    frames.push(frame);
    const index = waiters.findIndex((w) => w.match(frame));
    if (index !== -1) waiters.splice(index, 1)[0]!.resolve(frame);
  });
  return {
    frames,
    send: (m) => {
      const payload = JSON.stringify(m);
      if (ws.readyState === WebSocket.OPEN) ws.send(payload);
      else ws.on("open", () => ws.send(payload));
    },
    next: (match, timeoutMs = 4000) =>
      new Promise<Frame>((resolve, reject) => {
        const existing = frames.find(match);
        if (existing !== undefined) {
          resolve(existing);
          return;
        }
        const timer = setTimeout(() => reject(new Error("timed out waiting for frame")), timeoutMs);
        waiters.push({
          match,
          resolve: (f) => {
            clearTimeout(timer);
            resolve(f);
          },
        });
      }),
    close: () => ws.close(),
  };
}

let server: RunningServer | undefined;
afterAll(async () => {
  await server?.close();
});

describe("two browsers, one authoritative room (E3 end-to-end)", () => {
  it("plays a full game of War over real websockets", async () => {
    const { doc, yaml } = loadWar();
    server = await startNodeServer({ doc, yaml, seats: 2, port: 0 });
    expect(server.code).toMatch(/^[2-9A-HJ-NP-Z]{5}$/);

    const wsUrl = `ws://127.0.0.1:${server.port}/ws?code=${server.code}`;
    const alice = client(wsUrl);
    const bob = client(wsUrl);
    alice.send({ t: "join", name: "Alice" });
    const aWelcome = await alice.next((f) => f.t === "welcome");
    bob.send({ t: "join", name: "Bob" });
    const bWelcome = await bob.next((f) => f.t === "welcome");

    expect(aWelcome["seat"]).toBe(0);
    expect(bWelcome["seat"]).toBe(1);

    // Hidden information is handle-translated: no creation-ordered ids on the wire.
    const aState = aWelcome["state"] as { zones: { zone: string; ownerSeat: number | null; entries: Record<string, unknown>[] }[] };
    const aDeck = aState.zones.find((z) => z.zone === "deck" && z.ownerSeat === 0)!;
    expect(aDeck.entries.every((e) => "hidden" in e && /^p\d+$/.test(String(e["handle"])))).toBe(true);

    // Server-sent options: it's Alice's turn; Bob has none.
    expect((aWelcome["moves"] as unknown[]).length).toBeGreaterThan(0);
    expect((bWelcome["moves"] as unknown[]).length).toBe(0);

    // Play to the end: whoever holds moves plays them (52 plays total).
    let aSeq = -1;
    let bSeq = -1;
    let ended = false;
    for (let step = 0; step < 200 && !ended; step++) {
      const aLast = [...alice.frames].reverse().find((f) => f.t === "welcome" || f.t === "patch");
      const bLast = [...bob.frames].reverse().find((f) => f.t === "welcome" || f.t === "patch");
      const aMoves = (aLast?.["moves"] ?? []) as { action: string; target?: string }[];
      const bMoves = (bLast?.["moves"] ?? []) as { action: string; target?: string }[];
      if (aMoves.length > 0) {
        const seqBefore = Number(aLast!["seq"]);
        alice.send({ t: "move", ...aMoves[0]! });
        const patch = await alice.next((f) => f.t === "patch" && Number(f["seq"]) > seqBefore);
        aSeq = Number(patch["seq"]);
        ended = (patch["events"] as { type: string }[]).some((e) => e.type === "gameEnded");
        if (!ended) await bob.next((f) => f.t === "patch" && Number(f["seq"]) >= aSeq);
      } else if (bMoves.length > 0) {
        const seqBefore = Number(bLast!["seq"]);
        bob.send({ t: "move", ...bMoves[0]! });
        const patch = await bob.next((f) => f.t === "patch" && Number(f["seq"]) > seqBefore);
        bSeq = Number(patch["seq"]);
        ended = (patch["events"] as { type: string }[]).some((e) => e.type === "gameEnded");
        if (!ended) await alice.next((f) => f.t === "patch" && Number(f["seq"]) >= bSeq);
      } else {
        await new Promise((r) => setTimeout(r, 25));
      }
    }
    expect(ended).toBe(true);

    // Both clients saw the same ordered story: identical event-type sequences.
    const story = (frames: Frame[]): string =>
      frames
        .filter((f) => f.t === "patch")
        .flatMap((f) => (f["events"] as { type: string; seq: number }[]).map((e) => `${e.seq}:${e.type}`))
        .join(",");
    await bob.next((f) => f.t === "patch" && (f["events"] as { type: string }[]).some((e) => e.type === "gameEnded"));
    expect(story(alice.frames)).toBe(story(bob.frames));

    alice.close();
    bob.close();
  }, 20000);

  it("serves the play page and the /check self-test", async () => {
    const { doc, yaml } = loadWar();
    const s = await startNodeServer({ doc, yaml, seats: 2, port: 0 });
    const page = await (await fetch(`http://127.0.0.1:${s.port}/`)).text();
    expect(page).toContain("JunctionGame.bootOnline()");
    expect(page).toContain("LIVE");
    const check = (await (await fetch(`http://127.0.0.1:${s.port}/check`)).json()) as { ok: boolean };
    expect(check.ok).toBe(true);
    await s.close();
  });
});
