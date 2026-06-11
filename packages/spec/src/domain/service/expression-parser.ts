import type { BinOp, Expr } from "../model/expression.js";

/**
 * Recursive-descent parser for the total expression grammar:
 *   or := and ("||" and)* ; and := unary ("&&" unary)* ;
 *   comparison := additive (op additive)? ; additive/multiplicative as usual;
 *   primary := number | true | false | path | "(" or ")"
 */

export type ParseExprResult =
  | { readonly ok: true; readonly expr: Expr }
  | { readonly ok: false; readonly pos: number; readonly reason: string };

interface Token {
  readonly kind: "num" | "ident" | "op" | "lparen" | "rparen" | "dot";
  readonly text: string;
  readonly pos: number;
}

const OPS = ["||", "&&", "==", "!=", ">=", "<=", ">", "<", "+", "-", "*", "/", "!"] as const;

function tokenize(src: string): Token[] | { pos: number; reason: string } {
  const tokens: Token[] = [];
  let i = 0;
  while (i < src.length) {
    const ch = src[i]!;
    if (ch === " " || ch === "\t" || ch === "\n") {
      i++;
      continue;
    }
    if (ch === "(") {
      tokens.push({ kind: "lparen", text: ch, pos: i });
      i++;
      continue;
    }
    if (ch === ")") {
      tokens.push({ kind: "rparen", text: ch, pos: i });
      i++;
      continue;
    }
    if (ch === ".") {
      tokens.push({ kind: "dot", text: ch, pos: i });
      i++;
      continue;
    }
    const op = OPS.find((o) => src.startsWith(o, i));
    if (op !== undefined) {
      tokens.push({ kind: "op", text: op, pos: i });
      i += op.length;
      continue;
    }
    if (/[0-9]/.test(ch)) {
      let j = i;
      while (j < src.length && /[0-9]/.test(src[j]!)) j++;
      tokens.push({ kind: "num", text: src.slice(i, j), pos: i });
      i = j;
      continue;
    }
    if (/[A-Za-z_]/.test(ch)) {
      let j = i;
      while (j < src.length && /[A-Za-z0-9_-]/.test(src[j]!)) j++;
      tokens.push({ kind: "ident", text: src.slice(i, j), pos: i });
      i = j;
      continue;
    }
    return { pos: i, reason: `unexpected character '${ch}'` };
  }
  return tokens;
}

export function parseExpression(src: string): ParseExprResult {
  const tokens = tokenize(src);
  if (!Array.isArray(tokens)) return { ok: false, pos: tokens.pos, reason: tokens.reason };

  let cursor = 0;
  const peek = (): Token | undefined => tokens[cursor];
  const fail = (reason: string, pos?: number): never => {
    throw { pos: pos ?? peek()?.pos ?? src.length, reason };
  };
  const eat = (): Token => tokens[cursor++] ?? fail("unexpected end of expression");

  function parseOr(): Expr {
    let left = parseAnd();
    while (peek()?.text === "||") {
      eat();
      left = { t: "bin", op: "||", l: left, r: parseAnd() };
    }
    return left;
  }
  function parseAnd(): Expr {
    let left = parseNot();
    while (peek()?.text === "&&") {
      eat();
      left = { t: "bin", op: "&&", l: left, r: parseNot() };
    }
    return left;
  }
  function parseNot(): Expr {
    if (peek()?.text === "!") {
      eat();
      return { t: "un", op: "!", e: parseNot() };
    }
    return parseComparison();
  }
  function parseComparison(): Expr {
    const left = parseAdditive();
    const op = peek();
    if (op?.kind === "op" && ["==", "!=", ">=", "<=", ">", "<"].includes(op.text)) {
      eat();
      return { t: "bin", op: op.text as BinOp, l: left, r: parseAdditive() };
    }
    return left;
  }
  function parseAdditive(): Expr {
    let left = parseMultiplicative();
    while (peek()?.kind === "op" && (peek()!.text === "+" || peek()!.text === "-")) {
      const op = eat().text as BinOp;
      left = { t: "bin", op, l: left, r: parseMultiplicative() };
    }
    return left;
  }
  function parseMultiplicative(): Expr {
    let left = parseUnary();
    while (peek()?.kind === "op" && (peek()!.text === "*" || peek()!.text === "/")) {
      const op = eat().text as BinOp;
      left = { t: "bin", op, l: left, r: parseUnary() };
    }
    return left;
  }
  function parseUnary(): Expr {
    if (peek()?.text === "-") {
      eat();
      return { t: "un", op: "-", e: parseUnary() };
    }
    return parsePrimary();
  }
  function parsePrimary(): Expr {
    const tok = eat();
    if (tok.kind === "num") return { t: "num", v: Number(tok.text) };
    if (tok.kind === "ident") {
      if (tok.text === "true") return { t: "bool", v: true };
      if (tok.text === "false") return { t: "bool", v: false };
      const segs = [tok.text];
      while (peek()?.kind === "dot") {
        eat();
        const seg = eat();
        if (seg.kind !== "ident" && seg.kind !== "num")
          fail("expected identifier after '.'", seg.pos);
        segs.push(seg.text);
      }
      return { t: "path", segs };
    }
    if (tok.kind === "lparen") {
      const inner = parseOr();
      const close = eat();
      if (close.kind !== "rparen") fail("expected ')'", close.pos);
      return inner;
    }
    return fail(`unexpected token '${tok.text}'`, tok.pos);
  }

  try {
    const expr = parseOr();
    if (cursor < tokens.length)
      return { ok: false, pos: tokens[cursor]!.pos, reason: `unexpected trailing '${tokens[cursor]!.text}'` };
    return { ok: true, expr };
  } catch (e) {
    const { pos, reason } = e as { pos: number; reason: string };
    return { ok: false, pos, reason };
  }
}
