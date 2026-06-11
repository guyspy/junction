import type { GameEvent, ProjectedState } from "@junction/runtime";

/**
 * The Connexon wire protocol — plain JSON over any ordered transport (WebSocket today,
 * SSE+POST as the school-proxy fallback). Server→client events carry sequence numbers;
 * a reconnecting client resumes from its last seen seq (cheap-Chromebook Wi-Fi is the
 * design case). This module is pure types + guards: no transport, no platform.
 */

// ---- client → server --------------------------------------------------------

export type ClientMessage =
  | {
      readonly t: "join";
      /** Reconnect token from a prior `welcome`; omit to take a fresh seat. */
      readonly token?: string;
      /** Preferred display name; the room may disambiguate. */
      readonly name?: string;
      /** Resume server events strictly after this seq (reconnect). */
      readonly lastSeq?: number;
    }
  | { readonly t: "move"; readonly action: string; readonly target?: string }
  | { readonly t: "ping" };

// ---- server → client --------------------------------------------------------

/** Sent once on join: who you are, the full projected state, and how to resume. */
export interface WelcomeMessage {
  readonly t: "welcome";
  readonly seat: number;
  readonly seats: number;
  readonly token: string;
  readonly game: string;
  readonly title: string;
  /** The GameSpec YAML, so the client can mount the renderer with no extra fetch. */
  readonly spec: string;
  readonly state: ProjectedState;
  /** Highest event seq already reflected in `state`. */
  readonly seq: number;
}

/** An ordered, per-seat-projected batch of game events. */
export interface PatchMessage {
  readonly t: "patch";
  readonly events: readonly GameEvent[];
  /** Refreshed projection after the batch (clients may diff or replace). */
  readonly state: ProjectedState;
  /** Seq of the last event in this batch. */
  readonly seq: number;
}

export interface RoomInfoMessage {
  readonly t: "room";
  readonly seatsFilled: number;
  readonly seats: number;
  readonly status: "waiting" | "playing" | "ended";
}

export interface ErrorMessage {
  readonly t: "error";
  readonly code: string;
  readonly message: string;
}

export type ServerMessage = WelcomeMessage | PatchMessage | RoomInfoMessage | ErrorMessage | { readonly t: "pong" };

// ---- guards (transports hand us untrusted strings) --------------------------

export function parseClientMessage(raw: string): ClientMessage | null {
  let value: unknown;
  try {
    value = JSON.parse(raw);
  } catch {
    return null;
  }
  if (typeof value !== "object" || value === null) return null;
  const m = value as Record<string, unknown>;
  switch (m["t"]) {
    case "join":
      return {
        t: "join",
        ...(typeof m["token"] === "string" ? { token: m["token"] } : {}),
        ...(typeof m["name"] === "string" ? { name: m["name"] } : {}),
        ...(typeof m["lastSeq"] === "number" ? { lastSeq: m["lastSeq"] } : {}),
      };
    case "move":
      if (typeof m["action"] !== "string") return null;
      return {
        t: "move",
        action: m["action"],
        ...(typeof m["target"] === "string" ? { target: m["target"] } : {}),
      };
    case "ping":
      return { t: "ping" };
    default:
      return null;
  }
}

export function encode(message: ServerMessage): string {
  return JSON.stringify(message);
}
