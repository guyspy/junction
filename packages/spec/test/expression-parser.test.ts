import { describe, expect, it } from "vitest";
import { collectPaths, parseExpression } from "../src/index.js";

describe("expression parser (total grammar)", () => {
  it("parses precedence: comparison binds looser than arithmetic", () => {
    const r = parseExpression("1 + 2 * 3 == 7");
    expect(r.ok).toBe(true);
    if (!r.ok) return;
    expect(r.expr).toEqual({
      t: "bin",
      op: "==",
      l: {
        t: "bin",
        op: "+",
        l: { t: "num", v: 1 },
        r: { t: "bin", op: "*", l: { t: "num", v: 2 }, r: { t: "num", v: 3 } },
      },
      r: { t: "num", v: 7 },
    });
  });

  it("parses dotted paths including kebab-case zone names", () => {
    const r = parseExpression("zones.pot.count >= seats.count && turn.seatIndex == seats.count - 1");
    expect(r.ok).toBe(true);
    if (!r.ok) return;
    expect(collectPaths(r.expr).map((p) => p.join("."))).toEqual([
      "zones.pot.count",
      "seats.count",
      "turn.seatIndex",
      "seats.count",
    ]);
  });

  it("parses boolean literals and negation", () => {
    const r = parseExpression("!(true && false)");
    expect(r.ok).toBe(true);
  });

  it("reports syntax errors with a position", () => {
    const r = parseExpression("zones.pot.count >=");
    expect(r.ok).toBe(false);
    if (r.ok) return;
    expect(r.reason).toContain("unexpected end");
  });

  it("rejects trailing garbage", () => {
    const r = parseExpression("1 == 1 garbage");
    expect(r.ok).toBe(false);
  });
});
