import { stringify } from "yaml";

/**
 * Produce a valid, playable, guaranteed-terminating skeleton GameSpec the agent then
 * customizes (blueprint §10: scaffold returns a valid skeleton; agents don't start cold).
 * v1alpha ships one genre: card_game (a simple trick-take). A test pins that every
 * scaffold output validates and simulates to termination for 2–4 seats.
 */

export interface ScaffoldInput {
  readonly genre: "card_game";
  readonly title: string;
  readonly description?: string;
  readonly seatsMin?: number;
  readonly seatsMax?: number;
  /** Cards dealt to each seat; the game lasts this many rounds. */
  readonly handSize?: number;
  /** Card values run 1..valueMax. */
  readonly valueMax?: number;
}

export interface ScaffoldResult {
  readonly name: string;
  readonly yaml: string;
}

function toName(title: string): string {
  const slug = title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
  return /^[a-z]/.test(slug) ? slug : `game-${slug || "untitled"}`;
}

export function scaffoldGame(input: ScaffoldInput): ScaffoldResult {
  const seatsMin = input.seatsMin ?? 2;
  const seatsMax = input.seatsMax ?? Math.max(seatsMin, 4);
  const handSize = input.handSize ?? 4;
  const valueMax = input.valueMax ?? 10;
  // Enough cards for the largest table: seatsMax * handSize, generated as copies of 1..valueMax.
  const copies = Math.max(1, Math.ceil((seatsMax * handSize) / valueMax));
  const name = toName(input.title);

  const spec = {
    meta: {
      title: input.title,
      description:
        input.description ??
        "A simple trick-take: each seat plays one card a round; the highest value takes the trick. Most cards wins. Customize the pieces, values, and scoring to fit your lesson.",
      seats: { min: seatsMin, max: seatsMax },
      estMinutes: 10,
    },
    zones: [
      { name: "deck", owner: "shared", visibility: "none" },
      { name: "hand", owner: "seat", visibility: "owner" },
      { name: "table", owner: "shared", visibility: "all" },
      { name: "score", owner: "seat", visibility: "all" },
    ],
    pieces: [
      {
        name: "card",
        properties: { value: { type: "int" } },
        generate: { cartesian: { value: range(1, valueMax) } },
        copies,
      },
    ],
    setup: [
      { op: "create", pieces: "card", into: "deck" },
      { op: "shuffle", zone: "deck" },
      { op: "deal", from: "deck", to: "hand", count: handSize },
    ],
    turn: {
      order: "roundRobin",
      phases: [{ name: "play", actions: ["play-card"] }],
    },
    actions: [
      {
        name: "play-card",
        move: {
          from: { zone: "hand", owner: "actor" },
          take: "chosen",
          to: { zone: "table" },
          reveal: true,
        },
      },
    ],
    triggers: [
      {
        name: "resolve-trick",
        on: { event: "pieceMoved", intoZone: "table" },
        when: "zones.table.count >= seats.count && turn.seatIndex == seats.count - 1",
        effects: [
          {
            resolveHighest: {
              zone: "table",
              property: "value",
              toWinnerZone: "score",
              onTie: "stay",
            },
          },
        ],
      },
    ],
    end: {
      when: "zones.hand.allEmpty",
      winner: { mostPiecesIn: "score" },
    },
  };

  const doc = {
    apiVersion: "games.junction.aotter.net/v1alpha1",
    kind: "Game",
    metadata: { name },
    spec,
  };

  const header =
    `# ${input.title} — scaffolded by Junction (genre: ${input.genre}).\n` +
    `# A valid, terminating starting point. Customize pieces/values/triggers, then\n` +
    `# re-run validate_game and simulate_game to keep it sound.\n`;

  return { name, yaml: header + stringify(doc, { lineWidth: 0 }) };
}

function range(lo: number, hi: number): number[] {
  return Array.from({ length: hi - lo + 1 }, (_, i) => lo + i);
}
