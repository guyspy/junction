/**
 * Classroom join codes (the Kahoot-proven pattern): short, unambiguous, no student
 * accounts. Excludes look-alikes (0/O, 1/I/L) so a 7-year-old reads it off a projector.
 * Deterministic given an rng-like int source — the caller owns randomness (DO storage,
 * a seeded test, etc.).
 */

const ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"; // no 0 O 1 I L
const CODE_LENGTH = 5;

export function makeJoinCode(randomInt: (maxExclusive: number) => number): string {
  let code = "";
  for (let i = 0; i < CODE_LENGTH; i++) code += ALPHABET[randomInt(ALPHABET.length)];
  return code;
}

export function isValidJoinCode(code: string): boolean {
  return code.length === CODE_LENGTH && [...code].every((c) => ALPHABET.includes(c));
}

/** Curated, classroom-safe nickname pool for auto-assignment. */
const NICK_ADJECTIVES = ["Swift", "Brave", "Clever", "Sunny", "Lucky", "Mighty", "Jolly", "Cosmic", "Turbo", "Mega"];
const NICK_ANIMALS = ["Fox", "Owl", "Otter", "Panda", "Hawk", "Lynx", "Whale", "Gecko", "Bear", "Moth"];

export function autoNickname(randomInt: (maxExclusive: number) => number): string {
  const adj = NICK_ADJECTIVES[randomInt(NICK_ADJECTIVES.length)]!;
  const animal = NICK_ANIMALS[randomInt(NICK_ANIMALS.length)]!;
  return `${adj} ${animal}`;
}
