import type { GameEvent } from "@junction/runtime";
import { cardLabel } from "./view-model.js";

/**
 * Events → plain-language lines for the ticker and the ARIA live region.
 * The ordered event stream narrating itself is the accessibility story
 * (blueprint §8: the event log maps onto screen-reader announcements).
 */

function seatName(seat: number | null, viewerSeat: number): string {
  if (seat === null) return "the table";
  return seat === viewerSeat ? "you" : `seat ${seat}`;
}

function verb(seat: number | null, viewerSeat: number, you: string, they: string): string {
  return seat === viewerSeat ? you : they;
}

export function announce(event: GameEvent, viewerSeat: number): string | null {
  switch (event.type) {
    case "gameStarted":
      return `Game on — ${event.seats} seats.`;
    case "turnStarted":
      return event.seat === viewerSeat ? "Your turn." : null;
    case "pieceMoved": {
      if (event.revealed !== undefined) {
        const who = seatName(event.bySeat, viewerSeat);
        return `${capitalize(who)} ${verb(event.bySeat, viewerSeat, "play", "plays")} the ${cardLabel(event.revealed.properties)}.`;
      }
      return null;
    }
    case "pieceFlipped": {
      if (!event.faceUp) return null;
      const who = seatName(event.bySeat, viewerSeat);
      const what = event.revealed !== undefined ? `the ${cardLabel(event.revealed.properties)}` : "a card";
      return `${capitalize(who)} ${verb(event.bySeat, viewerSeat, "flip", "flips")} ${what}.`;
    }
    case "zoneResolved":
      if (event.winnerSeat === null) return `A tie — the ${event.zone} carries over.`;
      return `${capitalize(seatName(event.winnerSeat, viewerSeat))} ${verb(event.winnerSeat, viewerSeat, "take", "takes")} the ${event.zone}!`;
    case "pairResolved":
      if (event.matched)
        return `A match! ${capitalize(seatName(event.bySeat, viewerSeat))} ${verb(event.bySeat, viewerSeat, "keep", "keeps")} the pair and ${verb(event.bySeat, viewerSeat, "go", "goes")} again.`;
      return "No match — the cards flip back.";
    case "diceRolled":
      return `${capitalize(seatName(event.seat, viewerSeat))} ${verb(event.seat, viewerSeat, "roll", "rolls")} a ${event.value}.`;
    case "varChanged": {
      // Narrate per-seat swings (damage, score) — global bookkeeping stays quiet.
      if (event.scope !== "seat" || event.seat === null) return null;
      const delta = event.to - event.from;
      const who = capitalize(seatName(event.seat, viewerSeat));
      if (delta < 0) return `${who} ${verb(event.seat, viewerSeat, "lose", "loses")} ${-delta} ${event.var} (${event.to} left).`;
      return `${who} ${verb(event.seat, viewerSeat, "gain", "gains")} ${delta} ${event.var} (now ${event.to}).`;
    }
    case "propertyChanged":
      return null; // visual layers handle stat ticks; the ticker stays readable
    case "turnSkipped":
      return `${capitalize(seatName(event.seat, viewerSeat))} ${verb(event.seat, viewerSeat, "have", "has")} no move — skipped.`;
    case "gameEnded":
      if (event.winnerSeat === null) return "Game over — it's a draw.";
      return event.winnerSeat === viewerSeat ? "Game over — you win! 🏆" : `Game over — seat ${event.winnerSeat} wins.`;
    case "roundStarted":
    case "actionTaken":
    case "triggerFired":
      return null;
  }
}

export function announceAll(events: readonly GameEvent[], viewerSeat: number): string[] {
  const lines: string[] = [];
  for (const event of events) {
    const line = announce(event, viewerSeat);
    if (line !== null) lines.push(line);
  }
  return lines;
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
