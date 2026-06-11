import { simulate } from "@junction/runtime";
import { parseGameDocument } from "@junction/spec";
import { describe, expect, it } from "vitest";
import { scaffoldGame, type ScaffoldInput } from "../src/index.js";

/**
 * The scaffold contract: every output validates AND terminates for the whole declared
 * seat range. If this ever fails, agents would start from a broken skeleton — so it is
 * pinned hard across parameter combinations.
 */
const cases: ScaffoldInput[] = [
  { genre: "card_game", title: "Default Deck" },
  { genre: "card_game", title: "Big Hands", handSize: 7, valueMax: 13 },
  { genre: "card_game", title: "Tiny", seatsMin: 2, seatsMax: 2, handSize: 2, valueMax: 4 },
  { genre: "card_game", title: "Wide Table", seatsMin: 2, seatsMax: 6, handSize: 3, valueMax: 8 },
];

describe("scaffold_game always produces a sound starting point", () => {
  for (const input of cases) {
    it(`'${input.title}' validates and terminates across its seat range`, () => {
      const { yaml } = scaffoldGame(input);
      const parsed = parseGameDocument(yaml);
      expect(parsed.ok, parsed.ok ? "" : parsed.diagnostics.map((d) => d.message).join("\n")).toBe(true);
      if (!parsed.ok) return;

      const min = parsed.data.spec.meta.seats.min;
      const max = parsed.data.spec.meta.seats.max;
      for (let seats = min; seats <= max; seats++) {
        const report = simulate(parsed.data, { games: 40, seats, seed: `scaffold-${seats}` });
        expect(report.capped, `${input.title} capped at ${seats} seats`).toBe(0);
        expect(report.completed).toBe(40);
      }
    });
  }

  it("derives a kebab-case name from the title", () => {
    expect(scaffoldGame({ genre: "card_game", title: "Fraction Duel!" }).name).toBe("fraction-duel");
    expect(scaffoldGame({ genre: "card_game", title: "123 Go" }).name).toBe("game-123-go");
  });
});
