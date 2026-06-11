import type { GameDocument } from "@junction/spec";
import { legalMoves, projectState, type GameState, type PlayerMove } from "@junction/runtime";

/**
 * The pure view-model: (doc, state, viewerSeat) → what to draw, with zero DOM.
 * Zone presentation is INFERRED from semantics (owner + visibility) — the blueprint's
 * promise that semantic concepts make auto-rendering fall out for free.
 */

export interface CardVM {
  /** Stable handle for FLIP animation + targeting (pieceId or opaque handle). */
  readonly id: string;
  readonly faceUp: boolean;
  /** In flip-zones: this piece is currently flipped (a live selection) — render it raised. */
  readonly lifted: boolean;
  /** Accessible name: "queen of hearts" | "face-down card". */
  readonly label: string;
  readonly properties?: Readonly<Record<string, string | number>>;
  /** Set when clicking this card performs a legal move for the viewer. */
  readonly move?: PlayerMove;
}

export type ZoneKind = "stack" | "hand" | "row" | "pile";

export interface ZoneVM {
  readonly zone: string;
  readonly ownerSeat: number | null;
  readonly mine: boolean;
  readonly kind: ZoneKind;
  readonly title: string;
  readonly cards: readonly CardVM[];
  readonly count: number;
}

export interface MoveButtonVM {
  readonly move: PlayerMove;
  readonly label: string;
}

export interface ViewModel {
  readonly title: string;
  readonly statusLine: string;
  readonly yourTurn: boolean;
  readonly ended: boolean;
  readonly winnerText: string | null;
  readonly zones: readonly ZoneVM[];
  /** Untargeted legal moves render as buttons; targeted ones attach to their cards. */
  readonly buttons: readonly MoveButtonVM[];
}

const RANK_NAMES: Record<number, string> = { 11: "jack", 12: "queen", 13: "king", 14: "ace" };
const SUIT_GLYPHS: Record<string, string> = { clubs: "♣", diamonds: "♦", hearts: "♥", spades: "♠" };

export function cardShortText(props: Readonly<Record<string, string | number>>): { corner: string; center: string; tone: "red" | "dark" } {
  const suit = typeof props["suit"] === "string" ? (props["suit"] as string) : undefined;
  const rank = props["rank"] ?? props["value"];
  const rankText =
    typeof rank === "number" ? (RANK_NAMES[rank] !== undefined ? RANK_NAMES[rank]!.charAt(0).toUpperCase() : String(rank)) : undefined;
  if (suit !== undefined && SUIT_GLYPHS[suit] !== undefined) {
    const tone = suit === "hearts" || suit === "diamonds" ? "red" : "dark";
    return { corner: `${rankText ?? "?"}${SUIT_GLYPHS[suit]}`, center: SUIT_GLYPHS[suit]!, tone };
  }
  if (rankText !== undefined) return { corner: rankText, center: rankText, tone: "dark" };
  const first = Object.values(props)[0];
  return { corner: String(first ?? "?"), center: String(first ?? "?"), tone: "dark" };
}

export function cardLabel(props: Readonly<Record<string, string | number>>): string {
  const suit = typeof props["suit"] === "string" ? (props["suit"] as string) : undefined;
  const rank = props["rank"] ?? props["value"];
  const rankName = typeof rank === "number" ? (RANK_NAMES[rank] ?? String(rank)) : String(rank ?? "");
  if (suit !== undefined) return `${rankName} of ${suit}`;
  if (rank !== undefined) return `card ${rankName}`;
  const parts = Object.entries(props).map(([k, v]) => `${k} ${v}`);
  return parts.length > 0 ? parts.join(", ") : "card";
}

function zoneKind(owner: "shared" | "seat", visibility: "all" | "owner" | "none"): ZoneKind {
  if (owner === "seat") {
    if (visibility === "owner") return "hand";
    if (visibility === "none") return "stack";
    return "pile";
  }
  return "row";
}

function zoneTitle(zone: string, mine: boolean, ownerSeat: number | null, count: number): string {
  const who = ownerSeat === null ? "" : mine ? "your " : `seat ${ownerSeat}'s `;
  return `${who}${zone} (${count})`;
}

export function buildViewModel(doc: GameDocument, state: GameState, viewerSeat: number): ViewModel {
  const projected = projectState(state, doc.spec, viewerSeat);
  const yourTurn = state.status === "running" && state.activeSeat === viewerSeat;
  const moves = yourTurn ? legalMoves(state, doc.spec) : [];
  const moveByTarget = new Map<string, PlayerMove>();
  const buttons: MoveButtonVM[] = [];
  for (const move of moves) {
    if (move.target !== undefined) moveByTarget.set(move.target, move);
    else buttons.push({ move, label: move.action.replace(/-/g, " ") });
  }

  // Zones any flip-action targets: a face-up piece there is a live selection.
  const flipZones = new Set(doc.spec.actions.filter((a) => a.flip !== undefined).map((a) => a.flip!.zone.zone));

  const zones: ZoneVM[] = projected.zones.map((z) => {
    const decl = doc.spec.zones.find((d) => d.name === z.zone)!;
    const mine = z.ownerSeat === viewerSeat;
    const cards: CardVM[] = z.entries.map((entry) => {
      if ("hidden" in entry) {
        const move = moveByTarget.get(entry.handle);
        return {
          id: entry.handle,
          faceUp: false,
          lifted: false,
          label: move !== undefined ? "face-down card (playable)" : "face-down card",
          ...(move !== undefined ? { move } : {}),
        };
      }
      const move = moveByTarget.get(entry.pieceId);
      return {
        id: entry.pieceId,
        faceUp: true,
        lifted: entry.faceUp && flipZones.has(z.zone),
        label: cardLabel(entry.properties),
        properties: entry.properties,
        ...(move !== undefined ? { move } : {}),
      };
    });
    return {
      zone: z.zone,
      ownerSeat: z.ownerSeat,
      mine,
      kind: zoneKind(decl.owner, decl.visibility),
      title: zoneTitle(z.zone, mine, z.ownerSeat, z.count),
      cards,
      count: z.count,
    };
  });

  const ended = state.status === "ended";
  const winnerText = !ended
    ? null
    : state.winnerSeat === null
      ? "It's a draw!"
      : state.winnerSeat === viewerSeat
        ? "You win! 🏆"
        : `Seat ${state.winnerSeat} wins.`;

  const phase = doc.spec.turn.phases[state.phaseIndex]?.name ?? "";
  const statusLine = ended
    ? (winnerText ?? "Game over.")
    : yourTurn
      ? `Round ${state.round} — your turn (${phase.replace(/-/g, " ")})`
      : `Round ${state.round} — seat ${state.activeSeat} is thinking…`;

  return {
    title: doc.spec.meta.title,
    statusLine,
    yourTurn,
    ended,
    winnerText,
    zones,
    buttons,
  };
}
