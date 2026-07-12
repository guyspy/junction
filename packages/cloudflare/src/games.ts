import type { ReferenceGame } from "@junction/mcp";
import { parseGameDocument } from "@junction/spec";
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

export const REFERENCE_GAMES: readonly ReferenceGame[] = Object.entries(GAMES).map(([name, yaml]) => {
  const parsed = parseGameDocument(yaml, { file: `games/${name}.yaml` });
  if (!parsed.ok) throw new Error(`built-in game '${name}' is invalid`);
  return {
    name,
    title: parsed.data.spec.meta.title,
    description: parsed.data.spec.meta.description ?? "",
    yaml,
  };
});

export type GameName = keyof typeof GAMES;

export function isGameName(value: string): value is GameName {
  return Object.hasOwn(GAMES, value);
}
