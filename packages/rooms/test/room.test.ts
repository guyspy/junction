import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { parseGameDocument, type GameDocument } from "@junction/spec";
import { describe, expect, it } from "vitest";
import {
  Room,
  RoomManager,
  autoNickname,
  isValidJoinCode,
  makeJoinCode,
  parseClientMessage,
  type ServerMessage,
} from "../src/index.js";

function loadWar(): { doc: GameDocument; yaml: string } {
  const yaml = readFileSync(fileURLToPath(new URL("../../../games/war.yaml", import.meta.url)), "utf8");
  const parsed = parseGameDocument(yaml);
  if (!parsed.ok) throw new Error("war invalid");
  return { doc: parsed.data, yaml };
}

/** A test harness: collects every frame per connection, runs bots synchronously. */
function harness(seatCount = 2) {
  const { doc, yaml } = loadWar();
  const sent = new Map<string, ServerMessage[]>();
  const room = new Room({
    doc,
    yaml,
    seatCount,
    hooks: {
      send: (connId, text) => {
        const list = sent.get(connId) ?? [];
        list.push(JSON.parse(text) as ServerMessage);
        sent.set(connId, list);
      },
      now: () => 0,
      seed: "test",
    },
    schedule: (fn) => fn(), // bots step synchronously, to completion
  });
  const frames = (connId: string): ServerMessage[] => sent.get(connId) ?? [];
  const last = <T extends ServerMessage["t"]>(connId: string, t: T): Extract<ServerMessage, { t: T }> | undefined =>
    [...frames(connId)].reverse().find((m) => m.t === t) as Extract<ServerMessage, { t: T }> | undefined;
  return { room, frames, last };
}

describe("join codes + nicknames (classroom pattern)", () => {
  it("makes readable codes free of look-alike characters", () => {
    let i = 0;
    const code = makeJoinCode(() => (i++ * 7) % 31);
    expect(isValidJoinCode(code)).toBe(true);
    expect(code).not.toMatch(/[01OIL]/);
  });
  it("auto-nicknames are two friendly words", () => {
    const nick = autoNickname(() => 3);
    expect(nick.split(" ")).toHaveLength(2);
  });
});

describe("protocol guards", () => {
  it("parses valid messages and rejects junk", () => {
    expect(parseClientMessage('{"t":"move","action":"play-card"}')).toEqual({ t: "move", action: "play-card" });
    expect(parseClientMessage('{"t":"join","name":"Sunny Fox"}')).toEqual({ t: "join", name: "Sunny Fox" });
    expect(parseClientMessage("not json")).toBeNull();
    expect(parseClientMessage('{"t":"move"}')).toBeNull(); // missing action
  });
});

describe("Room — authoritative online play", () => {
  it("restores authoritative state and seat tokens from a durable snapshot", () => {
    const { doc, yaml } = loadWar();
    const sent = new Map<string, ServerMessage[]>();
    const send = (connId: string, text: string): void => {
      const frames = sent.get(connId) ?? [];
      frames.push(JSON.parse(text) as ServerMessage);
      sent.set(connId, frames);
    };
    const changes: ReturnType<Room["exportSnapshot"]>[] = [];
    const first = new Room({
      doc,
      yaml,
      seatCount: 2,
      hooks: { send, now: () => 0, seed: "durable", makeToken: () => "secure-seat-token" },
      bots: false,
      onChange: (snapshot) => changes.push(snapshot),
    });
    first.connect("c1");
    first.handle("c1", { t: "join", name: "Sunny Fox" });
    const welcome = (sent.get("c1") ?? []).find((message) => message.t === "welcome");
    if (welcome?.t !== "welcome") throw new Error("missing welcome");
    first.handle("c1", { t: "move", action: "play-card" });
    expect(changes.length).toBeGreaterThan(0);

    const restored = new Room({
      doc,
      yaml,
      seatCount: 2,
      hooks: { send, now: () => 1, seed: "durable", makeToken: () => "unused" },
      bots: false,
      snapshot: first.exportSnapshot(),
    });
    expect(restored.snapshot.seq).toBe(first.snapshot.seq);
    expect(restored.restoreConnection("c1b", welcome.token)).toBe(true);
    expect(restored.connectionToken("c1b")).toBe(welcome.token);
  });

  it("seats a player, welcomes them with spec + projection, and bots play the empty seat", () => {
    const { room, last } = harness(2);
    room.connect("c1");
    room.handle("c1", { t: "join", name: "Sunny Fox" });

    const welcome = last("c1", "welcome");
    expect(welcome?.seat).toBe(0);
    expect(welcome?.spec).toContain("kind: Game");
    // War deck is hidden even from its owner.
    const myDeck = welcome?.state.zones.find((z) => z.zone === "deck" && z.ownerSeat === 0);
    expect(myDeck?.entries.every((e) => "hidden" in e)).toBe(true);

    // Seat 1 is bot-held; after our turn it should act automatically (synchronous schedule).
    // It's seat 0's turn at welcome; play one card.
    room.handle("c1", { t: "move", action: "play-card" });
    const patch = last("c1", "patch");
    expect(patch).toBeDefined();
    expect(patch!.events.some((e) => e.type === "pieceMoved")).toBe(true);
    // Bots drove the table back around to seat 0 (or ended) — the game advanced.
    expect(room.snapshot.seq).toBeGreaterThan(welcome!.seq);
  });

  it("rejects an out-of-turn move with NOT_YOUR_TURN", () => {
    const { room, last } = harness(2);
    room.connect("c1");
    room.handle("c1", { t: "join" }); // seat 0
    // Force not-our-turn by playing twice quickly: after the first move + bot, it's seat 0 again,
    // so instead assert the guard directly by moving as a freshly joined second human on seat 1.
    room.connect("c2");
    room.handle("c2", { t: "join" }); // seat 1
    room.handle("c2", { t: "move", action: "play-card" }); // not seat 1's turn at game start
    expect(last("c2", "error")?.code).toBe("NOT_YOUR_TURN");
  });

  it("resume: a reconnecting client replays only the events it missed", () => {
    const { room, frames, last } = harness(2);
    room.connect("c1");
    room.handle("c1", { t: "join", name: "Sunny Fox" }); // seat 0
    const token = last("c1", "welcome")!.token;
    room.handle("c1", { t: "move", action: "play-card" });
    const seqAfter = last("c1", "patch")!.seq;

    // Drop and reconnect with the token + lastSeq = 0 (saw only the initial state).
    room.disconnect("c1");
    room.connect("c1b");
    const before = frames("c1b").length;
    room.handle("c1b", { t: "join", token, lastSeq: 0 });

    const welcome = last("c1b", "welcome");
    expect(welcome?.seat).toBe(0); // same seat reclaimed via token
    const replay = last("c1b", "patch");
    expect(replay).toBeDefined();
    expect(replay!.seq).toBe(seqAfter); // caught back up to current
    expect(frames("c1b").length).toBeGreaterThan(before);
  });

  it("revokes an older connection when its reconnect token resumes elsewhere", () => {
    const { room, last } = harness(2);
    room.connect("c1");
    room.handle("c1", { t: "join" });
    const token = last("c1", "welcome")!.token;

    room.connect("c1b");
    room.handle("c1b", { t: "join", token });
    room.handle("c1", { t: "move", action: "play-card" });
    expect(last("c1", "error")?.code).toBe("SESSION_REPLACED");

    room.handle("c1b", { t: "move", action: "play-card" });
    expect(last("c1b", "patch")).toBeDefined();
  });

  it("a solo player vs a bot can reach the end of a full game", () => {
    const { room, frames } = harness(2);
    room.connect("c1");
    room.handle("c1", { t: "join" });
    // Keep playing seat 0's only action until the game ends (bots auto-fill between).
    for (let i = 0; i < 60 && room.snapshot.status === "running"; i++) {
      if (room.snapshot.activeSeat === 0) room.handle("c1", { t: "move", action: "play-card" });
      else break; // bot seat should have been driven synchronously; guard against a stuck loop
    }
    const ended = frames("c1").some((m) => m.t === "patch" && m.events.some((e) => e.type === "gameEnded"));
    expect(ended).toBe(true);
    expect(room.snapshot.status).toBe("ended");
  });
});

describe("RoomManager — the classroom directory", () => {
  it("opens rooms with unique codes and routes connections by code", () => {
    const { doc, yaml } = loadWar();
    const sent = new Map<string, ServerMessage[]>();
    let counter = 0;
    const mgr = new RoomManager({
      send: (id, text) => {
        const l = sent.get(id) ?? [];
        l.push(JSON.parse(text) as ServerMessage);
        sent.set(id, l);
      },
      now: () => 0,
      randomInt: (max) => (counter++ * 13) % max,
      schedule: (fn) => fn(),
    });

    const code = mgr.open({ doc, yaml, seatCount: 2 });
    expect(isValidJoinCode(code)).toBe(true);
    expect(mgr.openRoomCount).toBe(1);

    expect(mgr.connect("zzz", "c1")).toBe(false); // unknown code
    expect(mgr.connect(code, "c1")).toBe(true);
    mgr.handle("c1", { t: "join", name: "Sunny Fox" });

    const welcome = [...(sent.get("c1") ?? [])].reverse().find((m) => m.t === "welcome");
    expect(welcome?.t).toBe("welcome");
    expect((welcome as { title?: string }).title).toContain("War");

    mgr.close(code);
    expect(mgr.openRoomCount).toBe(0);
  });
});
