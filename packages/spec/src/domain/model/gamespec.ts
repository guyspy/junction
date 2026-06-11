import { z } from "zod";

/**
 * GameSpec v1alpha1 — the tabletop genre module's `Game` kind (blueprint §5, Addendum 2 §3).
 * The grammar is CLOSED: every enum below is the complete v1alpha vocabulary.
 * New keys/effects/events are grammar revisions, never ad-hoc additions.
 */

export const API_VERSION = "games.junction.aotter.net/v1alpha1";
export const SUPPORTED_KINDS = ["Game"] as const;

const name = z
  .string()
  .min(1)
  .regex(/^[a-z][a-z0-9-]*$/, "kebab-case identifier");

// ---- zones ----------------------------------------------------------------
const zoneDecl = z.strictObject({
  name,
  owner: z.enum(["shared", "seat"]),
  visibility: z.enum(["all", "owner", "none"]),
  ordered: z.boolean().default(true),
});

// ---- pieces ---------------------------------------------------------------
const propertyDecl = z.strictObject({
  type: z.enum(["int", "string"]),
});

const pieceDecl = z.strictObject({
  name,
  properties: z.record(z.string(), propertyDecl).default({}),
  /** Cartesian generator: one piece per combination of the listed property values. */
  generate: z
    .strictObject({
      cartesian: z.record(z.string(), z.array(z.union([z.string(), z.number()])).min(1)),
    })
    .optional(),
  /** Fixed property values for non-generated pieces. */
  values: z.record(z.string(), z.union([z.string(), z.number()])).optional(),
  copies: z.number().int().min(1).default(1),
});

// ---- setup ----------------------------------------------------------------
const setupOp = z.discriminatedUnion("op", [
  z.strictObject({ op: z.literal("create"), pieces: name, into: name }),
  z.strictObject({ op: z.literal("shuffle"), zone: name }),
  z.strictObject({
    op: z.literal("deal"),
    from: name,
    to: name,
    count: z.union([z.literal("all"), z.number().int().min(1)]),
    roundRobin: z.boolean().default(true),
  }),
]);

// ---- turn / actions -------------------------------------------------------
const zoneSel = z.strictObject({
  zone: name,
  /** For owner=seat zones: whose instance. v1alpha vocabulary: the acting seat only. */
  owner: z.enum(["actor"]).optional(),
});

const actionDecl = z
  .strictObject({
    name,
    /** Move a piece between zones. `take: top` is automatic; `chosen` requires a player target. */
    move: z
      .strictObject({
        from: zoneSel,
        take: z.enum(["top", "chosen"]).default("top"),
        to: zoneSel,
        reveal: z.boolean().default(false),
      })
      .optional(),
    /** Flip a chosen face-down piece face-up in place (flipping down happens via effects). */
    flip: z
      .strictObject({
        zone: zoneSel,
        target: z.enum(["chosen"]),
        direction: z.enum(["faceUp"]),
      })
      .optional(),
    /** Extra legality condition (total expression). Implicit: a valid source piece must exist. */
    requires: z.string().optional(),
  })
  .refine((a) => (a.move !== undefined) !== (a.flip !== undefined), {
    message: "an action declares exactly one of: move, flip",
  });

const phaseDecl = z.strictObject({
  name,
  actions: z.array(name).min(1),
});

const turnDecl = z.strictObject({
  order: z.enum(["roundRobin"]),
  phases: z.array(phaseDecl).min(1),
});

// ---- triggers -------------------------------------------------------------
const effectDecl = z.union([
  z.strictObject({
    moveAll: z.strictObject({ from: zoneSel, to: zoneSel }),
  }),
  z.strictObject({
    /**
     * Compare each seat's most recently placed piece in `zone` by `property`;
     * strict maximum wins and takes every piece in the zone to its `toWinnerZone`.
     * On tie: `stay` leaves the zone untouched (the pot carries).
     */
    resolveHighest: z.strictObject({
      zone: name,
      property: z.string(),
      toWinnerZone: name,
      onTie: z.enum(["stay"]),
    }),
  }),
  z.strictObject({
    /**
     * Memory-match resolution: when exactly two pieces lie face-up in `zone`,
     * compare them by `property`. Equal ⇒ both move to the actor's `toZone`
     * (and `onMatch: goAgain` lets the actor keep the turn); unequal ⇒ both
     * flip back face-down.
     */
    resolveEqualPair: z.strictObject({
      zone: name,
      property: z.string(),
      toZone: name,
      onMatch: z.enum(["goAgain", "none"]).default("goAgain"),
      onMismatch: z.enum(["flipDown"]),
    }),
  }),
]);

const triggerDecl = z.strictObject({
  name,
  on: z.strictObject({
    event: z.enum(["pieceMoved", "pieceFlipped"]),
    /** pieceMoved filter: the destination zone. */
    intoZone: name.optional(),
    /** pieceFlipped filter: the zone the piece lies in. */
    inZone: name.optional(),
  }),
  when: z.string().optional(),
  effects: z.array(effectDecl).min(1),
});

// ---- presentation -----------------------------------------------------------
/**
 * Presentation hints (rung 2 of the customization ladder). Pure data, closed
 * vocabulary, zero effect on outcomes — the renderer maps these to CSS tokens,
 * motion parameters, and synth sound sets. All optional with defaults.
 */
const themeDecl = z.strictObject({
  /** Table palette. */
  table: z.enum(["forest", "ocean", "sunset", "slate", "plum"]).default("forest"),
  /** Accent for playable highlights and buttons. */
  accent: z.enum(["gold", "sky", "coral", "lime"]).default("gold"),
  /** Card scale. */
  cardSize: z.enum(["compact", "regular", "large"]).default("regular"),
  /** Motion intensity: calm = no overshoot/arcs, bouncy = maximum spring. */
  motion: z.enum(["calm", "lively", "bouncy"]).default("lively"),
  /** Celebration level at wins and matches. */
  celebration: z.enum(["subtle", "festive"]).default("festive"),
  /** Synthesized sound set (no audio assets exist anywhere). */
  sound: z.enum(["off", "soft", "arcade"]).default("soft"),
});

const presentationDecl = z.strictObject({
  theme: themeDecl.prefault({}),
});

// ---- end ------------------------------------------------------------------
const winnerRule = z.strictObject({
  /** Winner = seat with the most pieces in its instance of this owner zone. Tie ⇒ draw. */
  mostPiecesIn: name,
});

const endDecl = z.strictObject({
  when: z.string(),
  winner: winnerRule,
});

// ---- the Game spec --------------------------------------------------------
export const gameSpecSchema = z.strictObject({
  meta: z.strictObject({
    title: z.string().min(1),
    description: z.string().optional(),
    seats: z.strictObject({
      min: z.number().int().min(1),
      max: z.number().int().min(1),
    }),
    estMinutes: z.number().int().min(1).optional(),
  }),
  zones: z.array(zoneDecl).min(1),
  pieces: z.array(pieceDecl).min(1),
  setup: z.array(setupOp).min(1),
  turn: turnDecl,
  actions: z.array(actionDecl).min(1),
  triggers: z.array(triggerDecl).default([]),
  end: endDecl,
  presentation: presentationDecl.prefault({}),
});

export type GameSpec = z.infer<typeof gameSpecSchema>;
export type ThemeDecl = z.infer<typeof themeDecl>;
export type ZoneDecl = z.infer<typeof zoneDecl>;
export type PieceDecl = z.infer<typeof pieceDecl>;
export type SetupOp = z.infer<typeof setupOp>;
export type ActionDecl = z.infer<typeof actionDecl>;
export type TriggerDecl = z.infer<typeof triggerDecl>;
export type EffectDecl = z.infer<typeof effectDecl>;
export type ZoneSel = z.infer<typeof zoneSel>;

// ---- the manifest envelope ------------------------------------------------
export const envelopeSchema = z.object({
  apiVersion: z.string(),
  kind: z.string(),
  // Plain z.object strips unknown metadata keys (provenance etc. ride along unvalidated in v1alpha).
  metadata: z.object({ name }),
  spec: z.unknown(),
});

export interface GameDocument {
  readonly apiVersion: typeof API_VERSION;
  readonly kind: "Game";
  readonly metadata: { readonly name: string };
  readonly spec: GameSpec;
}
