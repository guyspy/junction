import type { GameSpec } from "@junction/spec";
import type { GameEvent } from "../model/events.js";
import { zoneKey, type GameState } from "../model/state.js";

/**
 * Per-seat projection — the hidden-information contract (blueprint §7).
 * A seat sees a zone's piece identities only when the zone's visibility permits;
 * otherwise it sees anonymous face-down backs. The same projection runs in solo and
 * online modes, so local and networked behavior never diverge.
 */

export interface ProjectedPiece {
  readonly pieceId: string;
  readonly decl: string;
  readonly properties: Readonly<Record<string, string | number>>;
}

/** A face-down/hidden piece: present and countable, identity withheld. */
export interface HiddenPiece {
  readonly hidden: true;
}

export type ProjectedEntry = ProjectedPiece | HiddenPiece;

export interface ProjectedZone {
  readonly zone: string;
  readonly owner: "shared" | "seat";
  /** For owner zones, which seat's instance this is. */
  readonly ownerSeat: number | null;
  readonly entries: readonly ProjectedEntry[];
  readonly count: number;
}

export interface ProjectedState {
  readonly viewerSeat: number;
  readonly status: GameState["status"];
  readonly round: number;
  readonly activeSeat: number;
  readonly phaseIndex: number;
  readonly winnerSeat: number | null;
  readonly zones: readonly ProjectedZone[];
}

/**
 * Visibility rule: a viewer may see a piece's identity when the zone is `all`,
 * when the zone is `owner` and the viewer owns this instance, or — regardless of
 * zone visibility — when the individual piece is face-up (a public reveal).
 */
function viewerSeesIdentity(
  visibility: "all" | "owner" | "none",
  ownerSeat: number | null,
  viewerSeat: number,
  faceUp: boolean,
): boolean {
  if (faceUp) return true;
  if (visibility === "all") return true;
  if (visibility === "owner") return ownerSeat === viewerSeat;
  return false;
}

export function projectState(state: GameState, spec: GameSpec, viewerSeat: number): ProjectedState {
  const zones: ProjectedZone[] = [];
  for (const decl of spec.zones) {
    const instances: { key: string; ownerSeat: number | null }[] =
      decl.owner === "shared"
        ? [{ key: zoneKey(decl.name, null), ownerSeat: null }]
        : Array.from({ length: state.seats }, (_, seat) => ({ key: zoneKey(decl.name, seat), ownerSeat: seat }));

    for (const { key, ownerSeat } of instances) {
      const raw = state.zones[key] ?? [];
      const entries: ProjectedEntry[] = raw.map((entry) => {
        const piece = state.pieces[entry.pieceId]!;
        if (viewerSeesIdentity(decl.visibility, ownerSeat, viewerSeat, piece.faceUp))
          return { pieceId: piece.id, decl: piece.decl, properties: { ...piece.properties } };
        return { hidden: true };
      });
      zones.push({ zone: decl.name, owner: decl.owner, ownerSeat, entries, count: raw.length });
    }
  }

  return {
    viewerSeat,
    status: state.status,
    round: state.round,
    activeSeat: state.activeSeat,
    phaseIndex: state.phaseIndex,
    winnerSeat: state.winnerSeat,
    zones,
  };
}

/**
 * Project a single event for a viewer: a `revealed` payload is stripped unless the
 * viewer is entitled to it. (In v1alpha a reveal is public, so revealed payloads pass
 * through; this is the seam where private reveals will be filtered.)
 */
export function projectEvent(event: GameEvent, _viewerSeat: number): GameEvent {
  return event;
}
