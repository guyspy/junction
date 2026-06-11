import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { parseGameDocument, type GameDocument } from "@junction/spec";
import { applyAction, buildInitialState, createRng } from "@junction/runtime";
import { describe, expect, it } from "vitest";
import { buildViewModel, cardLabel, cardShortText } from "../src/index.js";

function load(name: string): GameDocument {
  const yaml = readFileSync(fileURLToPath(new URL(`../../../games/${name}.yaml`, import.meta.url)), "utf8");
  const parsed = parseGameDocument(yaml);
  if (!parsed.ok) throw new Error(parsed.diagnostics.map((d) => d.message).join("\n"));
  return parsed.data;
}

describe("view-model: zone presentation inferred from semantics", () => {
  it("war: decks are stacks, pot a row, won piles; play-card is an untargeted button", () => {
    const doc = load("war");
    const { state } = buildInitialState(doc, 2, createRng("vm"));
    const vm = buildViewModel(doc, state, 0);

    const kinds = Object.fromEntries(vm.zones.map((z) => [`${z.zone}#${z.ownerSeat ?? "s"}`, z.kind]));
    expect(kinds["deck#0"]).toBe("stack");
    expect(kinds["pot#s"]).toBe("row");
    expect(kinds["won#1"]).toBe("pile");

    expect(vm.yourTurn).toBe(true);
    expect(vm.buttons).toHaveLength(1);
    expect(vm.buttons[0]!.label).toBe("play card");
    // take: top — no card carries a move.
    expect(vm.zones.flatMap((z) => z.cards).every((c) => c.move === undefined)).toBe(true);
    // Your own war deck is face-down even to you.
    const myDeck = vm.zones.find((z) => z.zone === "deck" && z.mine)!;
    expect(myDeck.cards.every((c) => !c.faceUp)).toBe(true);
    expect(myDeck.title).toBe("your deck (26)");
  });

  it("memory-match: sixteen hidden-but-targetable backs; flipping lifts the tile", () => {
    const doc = load("memory-match");
    const { state } = buildInitialState(doc, 2, createRng("vm"));
    const vm = buildViewModel(doc, state, 0);

    const grid = vm.zones.find((z) => z.zone === "grid")!;
    expect(grid.cards).toHaveLength(16);
    expect(grid.cards.every((c) => !c.faceUp && c.move !== undefined)).toBe(true);
    expect(vm.buttons).toHaveLength(0); // targeted moves attach to cards, not buttons

    const target = grid.cards[0]!.id;
    const step = applyAction(state, doc.spec, { seat: 0, action: "flip", target });
    if (!step.ok) throw new Error("flip failed");
    const vm2 = buildViewModel(doc, step.data.state, 0);
    const grid2 = vm2.zones.find((z) => z.zone === "grid")!;
    const flipped = grid2.cards.find((c) => c.id === target)!;
    expect(flipped.faceUp).toBe(true);
    expect(flipped.lifted).toBe(true); // live selection in a flip zone
    expect(flipped.move).toBeUndefined(); // can't flip an already-up tile
  });

  it("opponent's view of war hides everything it must", () => {
    const doc = load("war");
    const { state } = buildInitialState(doc, 2, createRng("vm"));
    const vm = buildViewModel(doc, state, 1);
    expect(vm.yourTurn).toBe(false);
    expect(vm.buttons).toHaveLength(0);
    const seat0Deck = vm.zones.find((z) => z.zone === "deck" && z.ownerSeat === 0)!;
    expect(seat0Deck.cards.every((c) => !c.faceUp && c.properties === undefined)).toBe(true);
  });
});

describe("card naming", () => {
  it("ranks map to court names and suits to glyphs", () => {
    expect(cardLabel({ suit: "hearts", rank: 12 })).toBe("queen of hearts");
    expect(cardLabel({ suit: "spades", rank: 14 })).toBe("ace of spades");
    expect(cardLabel({ value: 7 })).toBe("card 7");
    const short = cardShortText({ suit: "diamonds", rank: 13 });
    expect(short.corner).toBe("K♦");
    expect(short.tone).toBe("red");
  });
});
