import makeTenMatch from "../../../games/make-ten-match.yaml";
import mathDuel from "../../../games/math-duel.yaml";
import memoryMatch from "../../../games/memory-match.yaml";
import war from "../../../games/war.yaml";

export const GAMES = {
  war,
  "memory-match": memoryMatch,
  "make-ten-match": makeTenMatch,
  "math-duel": mathDuel,
} as const;

export type GameName = keyof typeof GAMES;

export function isGameName(value: string): value is GameName {
  return Object.hasOwn(GAMES, value);
}
