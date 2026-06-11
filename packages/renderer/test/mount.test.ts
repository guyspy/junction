// @vitest-environment happy-dom
import { parseGameDocument } from "@junction/spec";
import { describe, expect, it } from "vitest";
import { mountGame } from "../src/index.js";

// Hermetic mini-War (2 cards): happy-dom rewrites import.meta.url, so no disk reads here.
const MINI = `
apiVersion: games.junction.aotter.net/v1alpha1
kind: Game
metadata: { name: mini }
spec:
  meta:
    title: Mini Battle
    seats: { min: 2, max: 2 }
  zones:
    - { name: deck, owner: seat, visibility: none }
    - { name: pot, owner: shared, visibility: all }
    - { name: won, owner: seat, visibility: all }
  pieces:
    - name: card
      properties: { rank: { type: int } }
      generate: { cartesian: { rank: [2, 3] } }
  setup:
    - { op: create, pieces: card, into: pot }
    - { op: deal, from: pot, to: deck, count: all }
  turn:
    order: roundRobin
    phases: [{ name: battle, actions: [play] }]
  actions:
    - name: play
      move: { from: { zone: deck, owner: actor }, to: { zone: pot }, reveal: true }
  triggers:
    - name: resolve
      on: { event: pieceMoved, intoZone: pot }
      when: "zones.pot.count >= seats.count && turn.seatIndex == seats.count - 1"
      effects:
        - resolveHighest: { zone: pot, property: rank, toWinnerZone: won, onTie: stay }
  end:
    when: "zones.deck.allEmpty"
    winner: { mostPiecesIn: won }
`;

function loadMini() {
  const parsed = parseGameDocument(MINI);
  if (!parsed.ok) throw new Error(parsed.diagnostics.map((d) => d.message).join("\n"));
  return parsed.data;
}

describe("mountGame (happy-dom smoke)", () => {
  it("mounts a playable board: zones, a move button, and a live region", () => {
    const doc = loadMini();
    const host = document.createElement("div");
    document.body.append(host);
    const controller = mountGame(host, doc, { seat: 0, seed: "dom", botDelay: 1 });

    expect(host.querySelectorAll(".jx-zone")).toHaveLength(5); // deck×2, pot, won×2
    expect(host.querySelectorAll(".jx-card")).toHaveLength(2);
    const button = host.querySelector<HTMLButtonElement>(".jx-button");
    expect(button?.textContent).toBe("play");
    expect(host.querySelector("[aria-live]")).not.toBeNull();
    expect(host.querySelector(".jx-status")?.textContent).toContain("your turn");

    // A click moves a card to the pot and narrates it.
    button!.click();
    const ticker = host.querySelector(".jx-ticker")!.textContent ?? "";
    expect(ticker).toContain("You play the");

    controller.dispose();
  });
});
