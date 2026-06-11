import { describe, expect, it } from "vitest";
import {
  formatDiagnosticText,
  hasErrors,
  makeDiagnostic,
  runtimeError,
  suggestFrom,
  validateError,
} from "../src/index.js";

describe("diagnostic kernel", () => {
  it("composes the message from structured fields", () => {
    const d = makeDiagnostic({
      code: "ZONE_REF_UNKNOWN",
      phase: "validate",
      severity: "error",
      path: "war.yaml#/spec/actions/0/move/from/zone",
      value: "dekk",
      expected: "a declared zone",
      candidates: ["deck", "pot", "won"],
      suggestion: "deck",
    });
    expect(d.message).toContain("war.yaml#/spec/actions/0/move/from/zone");
    expect(d.message).toContain("'dekk'");
    expect(d.message).toContain("Did you mean 'deck'?");
  });

  it("lists candidates when no suggestion is confident", () => {
    const d = validateError({
      code: "UNSUPPORTED_KIND",
      path: "x#/kind",
      value: "Quiz",
      expected: "a supported kind",
      candidates: ["Game"],
    });
    expect(d.message).toContain("Known: [Game]");
  });

  it("strips candidates from runtime diagnostics (wire security rule)", () => {
    const d = runtimeError({
      code: "ACTION_NOT_LEGAL",
      path: "action:cheat",
      value: "cheat",
      candidates: ["play-card"],
    });
    expect(d.candidates).toBeUndefined();
    expect(d.phase).toBe("runtime");
  });

  it("suggestFrom only fires within edit distance 2", () => {
    expect(suggestFrom("dekc", ["deck", "pot"])).toBe("deck");
    expect(suggestFrom("battlefield", ["deck", "pot"])).toBeUndefined();
  });

  it("hasErrors distinguishes warnings", () => {
    const warning = makeDiagnostic({
      code: "SCHEMA_VALIDATION_FAILED",
      phase: "validate",
      severity: "warning",
      path: "x",
    });
    expect(hasErrors([warning])).toBe(false);
  });

  it("formats one line with a severity badge", () => {
    const d = validateError({ code: "INVALID_YAML", path: "x.yaml:3:1" });
    expect(formatDiagnosticText(d)).toMatch(/^✖ \[INVALID_YAML\]/);
  });
});
