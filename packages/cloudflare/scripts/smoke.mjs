import assert from "node:assert/strict";

const base = new URL(process.argv.slice(2).find((arg) => arg !== "--") ?? "http://127.0.0.1:8787");
const check = await fetch(new URL("/check", base));
assert.equal(check.status, 200, "/check must succeed");

const createdResponse = await fetch(new URL("/api/rooms?game=war", base), { method: "POST" });
assert.equal(createdResponse.status, 201, "room creation must succeed");
const created = await createdResponse.json();
assert.match(created.code, /^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{5}$/);

const wsUrl = new URL(`/ws?code=${created.code}`, base);
wsUrl.protocol = base.protocol === "https:" ? "wss:" : "ws:";

const first = await connect(wsUrl);
const firstWelcomePromise = first.next("welcome");
first.socket.send(JSON.stringify({ t: "join", name: "Smoke One" }));
const firstWelcome = await firstWelcomePromise;
assert.equal(firstWelcome.seat, 0);
assert.ok(hiddenDeck(firstWelcome), "seat 0 deck identities must be hidden");

const second = await connect(wsUrl);
const secondWelcomePromise = second.next("welcome");
second.socket.send(JSON.stringify({ t: "join", name: "Smoke Two" }));
const secondWelcome = await secondWelcomePromise;
assert.equal(secondWelcome.seat, 1);
assert.ok(hiddenDeck(secondWelcome), "seat 1 deck identities must be hidden");

const firstPatchPromise = first.next("patch");
const secondPatchPromise = second.next("patch");
first.socket.send(JSON.stringify({ t: "move", action: "play-card" }));
const [firstPatch, secondPatch] = await Promise.all([firstPatchPromise, secondPatchPromise]);
assert.equal(firstPatch.seq, secondPatch.seq);
assert.ok(hiddenDeck(firstPatch), "hidden deck identities must remain projected after a move");

const token = firstWelcome.token;
const lastSeq = firstPatch.seq;
first.socket.close(1000, "reconnect smoke");

const resumed = await connect(wsUrl);
const resumedWelcomePromise = resumed.next("welcome");
resumed.socket.send(JSON.stringify({ t: "join", token, lastSeq }));
const resumedWelcome = await resumedWelcomePromise;
assert.equal(resumedWelcome.seat, 0);
assert.equal(resumedWelcome.token, token);
assert.equal(resumedWelcome.seq, lastSeq);

const resumedPatchPromise = resumed.next("patch");
const secondRoundPatchPromise = second.next("patch");
second.socket.send(JSON.stringify({ t: "move", action: "play-card" }));
await Promise.all([resumedPatchPromise, secondRoundPatchPromise]);

second.socket.close(1000, "smoke complete");
resumed.socket.close(1000, "smoke complete");

console.log(JSON.stringify({ ok: true, base: base.origin, code: created.code, reconnect: true, hiddenProjection: true }));

async function connect(url) {
  const socket = new WebSocket(url);
  const waiters = new Map();
  socket.addEventListener("message", (event) => {
    const frame = JSON.parse(String(event.data));
    const queue = waiters.get(frame.t);
    queue?.shift()?.resolve(frame);
  });
  await new Promise((resolve, reject) => {
    socket.addEventListener("open", resolve, { once: true });
    socket.addEventListener("error", () => reject(new Error(`WebSocket failed: ${url}`)), { once: true });
  });
  return {
    socket,
    next(type) {
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error(`Timed out waiting for ${type}`)), 5000);
        const queue = waiters.get(type) ?? [];
        queue.push({ resolve: (frame) => { clearTimeout(timer); resolve(frame); } });
        waiters.set(type, queue);
      });
    },
  };
}

function hiddenDeck(frame) {
  const deck = frame.state?.zones?.find((zone) => zone.zone === "deck");
  return deck?.entries?.length > 0 && deck.entries.every((entry) => entry.hidden === true && /^p\d+$/.test(entry.handle));
}
