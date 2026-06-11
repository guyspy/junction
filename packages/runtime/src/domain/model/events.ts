/**
 * The ordered event stream — the wire protocol's seed (blueprint §7; Hearthstone lineage).
 * Every event carries a sequence number; the event log is the source of truth for
 * replays, spectating, and learning analytics.
 */

export interface PieceView {
  readonly pieceId: string;
  readonly decl: string;
  readonly properties: Readonly<Record<string, string | number>>;
}

export type GameEvent = { readonly seq: number } & (
  | { readonly type: "gameStarted"; readonly seats: number; readonly game: string }
  | { readonly type: "roundStarted"; readonly round: number }
  | { readonly type: "turnStarted"; readonly seat: number; readonly round: number }
  | { readonly type: "actionTaken"; readonly seat: number; readonly action: string }
  | {
      readonly type: "pieceMoved";
      readonly pieceId: string;
      readonly from: string;
      readonly to: string;
      readonly bySeat: number | null;
      /** Present when the move reveals the piece (visibility rules). */
      readonly revealed?: PieceView;
    }
  | { readonly type: "triggerFired"; readonly trigger: string }
  | {
      readonly type: "pieceFlipped";
      readonly pieceId: string;
      readonly zone: string;
      readonly faceUp: boolean;
      readonly bySeat: number | null;
      /** Present when the flip turns the piece face-up (a public reveal). */
      readonly revealed?: PieceView;
    }
  | {
      readonly type: "zoneResolved";
      readonly zone: string;
      readonly property: string;
      readonly winnerSeat: number | null;
    }
  | {
      readonly type: "pairResolved";
      readonly zone: string;
      readonly property: string;
      readonly matched: boolean;
      readonly bySeat: number;
    }
  | { readonly type: "turnSkipped"; readonly seat: number; readonly phase: string }
  | {
      readonly type: "gameEnded";
      readonly winnerSeat: number | null;
      readonly reason: "endCondition" | "stalled";
    }
);

export type GameEventType = GameEvent["type"];

/** Distributive Omit — event initializers keep their variant fields, the reducer stamps `seq`. */
export type GameEventInit = {
  [K in GameEventType]: Omit<Extract<GameEvent, { type: K }>, "seq">;
}[GameEventType];
