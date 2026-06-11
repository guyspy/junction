import { describe, expect, it } from "vitest";
import { announce } from "../src/index.js";

describe("announcer: events become plain language", () => {
  it("speaks reveals from your perspective", () => {
    expect(
      announce(
        {
          seq: 1,
          type: "pieceMoved",
          pieceId: "x",
          from: "deck#0",
          to: "pot",
          bySeat: 0,
          revealed: { pieceId: "x", decl: "playing-card", properties: { suit: "hearts", rank: 14 } },
        },
        0,
      ),
    ).toBe("You play the ace of hearts.");
  });

  it("speaks the opponent in third person", () => {
    expect(
      announce(
        { seq: 1, type: "zoneResolved", zone: "pot", property: "rank", winnerSeat: 1 },
        0,
      ),
    ).toBe("Seat 1 takes the pot!");
  });

  it("narrates matches and mismatches", () => {
    expect(announce({ seq: 1, type: "pairResolved", zone: "grid", property: "value", matched: true, bySeat: 0 }, 0)).toBe(
      "A match! You keep the pair and go again.",
    );
    expect(announce({ seq: 1, type: "pairResolved", zone: "grid", property: "value", matched: false, bySeat: 1 }, 0)).toBe(
      "No match — the cards flip back.",
    );
  });

  it("stays quiet on engine bookkeeping", () => {
    expect(announce({ seq: 1, type: "actionTaken", seat: 0, action: "play-card" }, 0)).toBeNull();
    expect(announce({ seq: 1, type: "triggerFired", trigger: "resolve-battle" }, 0)).toBeNull();
  });

  it("announces endings relative to the viewer", () => {
    expect(announce({ seq: 1, type: "gameEnded", winnerSeat: 0, reason: "endCondition" }, 0)).toBe("Game over — you win! 🏆");
    expect(announce({ seq: 1, type: "gameEnded", winnerSeat: null, reason: "endCondition" }, 0)).toBe("Game over — it's a draw.");
  });
});
