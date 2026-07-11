import { createRng, type GameEvent, type PlayerMove, type ProjectedState } from "@junction/runtime";

/**
 * Opaque wire handles. Piece ids are creation-ordered ("tile-0" is the first value),
 * so seeing any id+identity pair lets a client decode the whole deck. On the wire,
 * every pieceId becomes "p<n>" where n comes from a server-secret shuffle — stable for
 * the room's lifetime (FLIP animation + targeting keys survive reconnects), but
 * unlinkable to creation order without the seed.
 */
export class HandleCodec {
  private readonly toHandle = new Map<string, string>();
  private readonly toPiece = new Map<string, string>();

  constructor(pieceIds: readonly string[], seed: string) {
    const rng = createRng(seed);
    const indices = pieceIds.map((_, i) => i);
    for (let i = indices.length - 1; i > 0; i--) {
      const j = rng.int(i + 1);
      [indices[i], indices[j]] = [indices[j]!, indices[i]!];
    }
    pieceIds.forEach((id, i) => {
      const handle = `p${indices[i]!}`;
      this.toHandle.set(id, handle);
      this.toPiece.set(handle, id);
    });
  }

  toWire(pieceId: string): string {
    return this.toHandle.get(pieceId) ?? pieceId;
  }

  fromWire(handle: string): string {
    return this.toPiece.get(handle) ?? handle;
  }

  translateEvent(event: GameEvent): GameEvent {
    switch (event.type) {
      case "pieceMoved":
      case "pieceFlipped":
      case "propertyChanged":
        return {
          ...event,
          pieceId: this.toWire(event.pieceId),
          ...("revealed" in event && event.revealed !== undefined
            ? { revealed: { ...event.revealed, pieceId: this.toWire(event.revealed.pieceId) } }
            : {}),
        };
      default:
        return event;
    }
  }

  translateState(state: ProjectedState): ProjectedState {
    return {
      ...state,
      zones: state.zones.map((zone) => ({
        ...zone,
        entries: zone.entries.map((entry) =>
          "hidden" in entry
            ? { ...entry, handle: this.toWire(entry.handle) }
            : { ...entry, pieceId: this.toWire(entry.pieceId) },
        ),
      })),
    };
  }

  translateMoves(moves: readonly PlayerMove[]): PlayerMove[] {
    return moves.map((m) => (m.target === undefined ? m : { ...m, target: this.toWire(m.target) }));
  }
}
