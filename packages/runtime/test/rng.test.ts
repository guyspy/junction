import { describe, expect, it } from "vitest";
import { createRng, shuffled } from "../src/index.js";

describe("seeded rng", () => {
  it("is deterministic for equal seeds", () => {
    const a = createRng("junction");
    const b = createRng("junction");
    const seqA = Array.from({ length: 10 }, () => a.next());
    const seqB = Array.from({ length: 10 }, () => b.next());
    expect(seqA).toEqual(seqB);
  });

  it("diverges for different seeds", () => {
    const a = createRng("seed-1");
    const b = createRng("seed-2");
    expect(a.next()).not.toBe(b.next());
  });

  it("int stays within bounds", () => {
    const rng = createRng(7);
    for (let i = 0; i < 1000; i++) {
      const v = rng.int(13);
      expect(v).toBeGreaterThanOrEqual(0);
      expect(v).toBeLessThan(13);
    }
  });

  it("shuffles deterministically without mutating the input", () => {
    const input = [1, 2, 3, 4, 5, 6, 7, 8];
    const out1 = shuffled(input, createRng("s"));
    const out2 = shuffled(input, createRng("s"));
    expect(out1).toEqual(out2);
    expect(input).toEqual([1, 2, 3, 4, 5, 6, 7, 8]);
    expect([...out1].sort((a, b) => a - b)).toEqual(input);
  });
});
