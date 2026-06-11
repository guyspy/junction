/**
 * A compact, structured description of the closed GameSpec v1alpha grammar — the
 * vocabulary an agent is allowed to use. Returned by the describe_grammar tool so
 * agents never guess at keys or enum values (house rule: the grammar is closed).
 *
 * This mirrors the Zod schema in @junction/spec; a test pins the enums to reality.
 */

export interface GrammarReference {
  readonly apiVersion: string;
  readonly kinds: readonly string[];
  readonly envelope: string;
  readonly zones: { readonly owner: readonly string[]; readonly visibility: readonly string[]; readonly notes: string };
  readonly pieces: { readonly propertyTypes: readonly string[]; readonly notes: string };
  readonly setup: readonly string[];
  readonly actions: { readonly kinds: readonly string[]; readonly takeModes: readonly string[]; readonly notes: string };
  readonly triggers: { readonly events: readonly string[]; readonly notes: string };
  readonly effects: readonly { readonly name: string; readonly summary: string }[];
  readonly expressions: { readonly grammar: string; readonly paths: readonly string[]; readonly notes: string };
  readonly end: string;
  readonly presentation: {
    readonly theme: Readonly<Record<string, readonly string[]>>;
    readonly notes: string;
  };
}

export const GRAMMAR_REFERENCE: GrammarReference = {
  apiVersion: "games.junction.aotter.net/v1alpha1",
  kinds: ["Game"],
  envelope:
    "Top level: apiVersion, kind: Game, metadata: { name (kebab-case) }, spec. One Game per file.",
  zones: {
    owner: ["shared", "seat"],
    visibility: ["all", "owner", "none"],
    notes:
      "A `seat` zone has one instance per seat. Visibility controls who sees a piece's identity; a face-up piece is always visible regardless. Reference a seat zone for the acting seat with { zone, owner: actor }.",
  },
  pieces: {
    propertyTypes: ["int", "string"],
    notes:
      "Declare a piece set with typed properties. `generate.cartesian` makes one piece per combination of listed property values; `copies` multiplies. `values` sets fixed properties for a non-generated set. Pieces start face-down.",
  },
  setup: [
    "create: { op: create, pieces: <set>, into: <shared zone> }",
    "shuffle: { op: shuffle, zone: <shared zone> }",
    "deal: { op: deal, from: <shared zone>, to: <seat zone>, count: all | <int>, roundRobin: true }",
  ],
  actions: {
    kinds: ["move", "flip"],
    takeModes: ["top", "chosen"],
    notes:
      "An action declares exactly one of move | flip. move: { from, to (zoneSel), take: top|chosen, reveal }. flip: { zone, target: chosen, direction: faceUp }. Optional `requires`: a boolean expression gating legality (a non-empty source is always implied).",
  },
  triggers: {
    events: ["pieceMoved", "pieceFlipped"],
    notes:
      "on: { event, intoZone? (pieceMoved filter), inZone? (pieceFlipped filter) }. Optional `when`: boolean expression evaluated against the NEW state. effects: run in order.",
  },
  effects: [
    { name: "moveAll", summary: "Move every piece from one zone to another: { moveAll: { from, to } }." },
    {
      name: "resolveHighest",
      summary:
        "Compare each seat's most recent piece in a shared zone by an int property; strict max wins the whole zone to its seat's toWinnerZone. onTie: stay (pot carries). { resolveHighest: { zone, property, toWinnerZone, onTie: stay } }.",
    },
    {
      name: "resolveEqualPair",
      summary:
        "When exactly two pieces are face-up in a shared zone, compare by property: equal → both move to the actor's toZone (onMatch: goAgain keeps the turn); unequal → both flip face-down (onMismatch: flipDown). { resolveEqualPair: { zone, property, toZone, onMatch, onMismatch } }.",
    },
  ],
  expressions: {
    grammar:
      "Total: comparisons (== != > < >= <=), arithmetic (+ - * /), boolean (&& || !), parentheses, int/bool literals, and context paths. No loops, no function calls.",
    paths: [
      "zones.<shared>.count",
      "zones.<shared>.faceUpCount",
      "zones.<zone>.totalCount (owner zones sum across seats)",
      "zones.<zone>.allEmpty",
      "zones.<zone>.anyEmpty",
      "seats.count",
      "turn.round",
      "turn.seatIndex",
    ],
    notes: "count/faceUpCount are shared-zone-only; use totalCount/allEmpty/anyEmpty for owner zones.",
  },
  end: "end: { when: <boolean expression>, winner: { mostPiecesIn: <seat zone> } }. A tie in the winner zone is a draw.",
  presentation: {
    theme: {
      table: ["forest", "ocean", "sunset", "slate", "plum"],
      accent: ["gold", "sky", "coral", "lime"],
      cardSize: ["compact", "regular", "large"],
      motion: ["calm", "lively", "bouncy"],
      celebration: ["subtle", "festive"],
      sound: ["off", "soft", "arcade"],
    },
    notes:
      "Optional `presentation.theme` block — pure presentation data, zero effect on rules or simulation. All fields optional with defaults (forest/gold/regular/lively/festive/soft). Pick a palette and feel that fits the lesson's mood; sound is synthesized, no assets exist.",
  },
};

export function describeGrammar(): GrammarReference {
  return GRAMMAR_REFERENCE;
}
