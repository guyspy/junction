import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { parseGameDocument, type Diagnostic } from "../src/index.js";

const warYaml = readFileSync(
  fileURLToPath(new URL("../../../games/war.yaml", import.meta.url)),
  "utf8",
);

function codes(diagnostics: readonly Diagnostic[]): string[] {
  return diagnostics.map((d) => d.code);
}

/** Minimal valid document used as the mutation base for lint tests. */
const base = `
apiVersion: games.junction.aotter.net/v1alpha1
kind: Game
metadata: { name: mini }
spec:
  meta:
    title: Mini
    seats: { min: 2, max: 2 }
  zones:
    - { name: deck, owner: seat, visibility: none }
    - { name: pot, owner: shared, visibility: all }
    - { name: won, owner: seat, visibility: all }
  pieces:
    - name: card
      properties:
        rank: { type: int }
      generate: { cartesian: { rank: [2, 3] } }
  setup:
    - { op: create, pieces: card, into: pot }
    - { op: deal, from: pot, to: deck, count: all }
  turn:
    order: roundRobin
    phases: [{ name: battle, actions: [play] }]
  actions:
    - name: play
      move: { from: { zone: deck, owner: actor }, to: { zone: pot }, reveal: true }
  end:
    when: "zones.deck.allEmpty"
    winner: { mostPiecesIn: won }
`;

describe("manifest parser", () => {
  it("accepts games/war.yaml (the first GameSpec)", () => {
    const result = parseGameDocument(warYaml, { file: "games/war.yaml" });
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.data.metadata.name).toBe("war");
    expect(result.data.spec.zones).toHaveLength(3);
    expect(result.data.spec.triggers).toHaveLength(1);
  });

  it("accepts the minimal base document", () => {
    const result = parseGameDocument(base);
    expect(result.ok).toBe(true);
  });

  it("rejects a wrong apiVersion with a suggestion", () => {
    const result = parseGameDocument(base.replace("v1alpha1", "v1"));
    expect(result.ok).toBe(false);
    if (result.ok) return;
    expect(codes(result.diagnostics)).toContain("UNSUPPORTED_API_VERSION");
    expect(result.diagnostics[0]!.suggestion).toBe("games.junction.aotter.net/v1alpha1");
  });

  it("rejects an unsupported kind with candidates", () => {
    const result = parseGameDocument(base.replace("kind: Game", "kind: Quiz"));
    expect(result.ok).toBe(false);
    if (result.ok) return;
    const d = result.diagnostics.find((x) => x.code === "UNSUPPORTED_KIND")!;
    expect(d.candidates).toEqual(["Game"]);
  });

  it("rejects duplicate zone names", () => {
    const result = parseGameDocument(
      base.replace(
        "- { name: pot, owner: shared, visibility: all }",
        "- { name: pot, owner: shared, visibility: all }\n    - { name: pot, owner: shared, visibility: all }",
      ),
    );
    expect(result.ok).toBe(false);
    if (result.ok) return;
    expect(codes(result.diagnostics)).toContain("DUPLICATE_NAME");
  });

  it("rejects an unknown zone reference with a typo suggestion", () => {
    const result = parseGameDocument(base.replace("from: { zone: deck, owner: actor }", "from: { zone: dekk, owner: actor }"));
    expect(result.ok).toBe(false);
    if (result.ok) return;
    const d = result.diagnostics.find((x) => x.code === "ZONE_REF_UNKNOWN")!;
    expect(d.suggestion).toBe("deck");
    expect(d.path).toContain("actions/0/move/from/zone");
  });

  it("rejects an unknown action in a phase", () => {
    const result = parseGameDocument(base.replace("actions: [play]", "actions: [playy]"));
    expect(result.ok).toBe(false);
    if (result.ok) return;
    const d = result.diagnostics.find((x) => x.code === "ACTION_REF_UNKNOWN")!;
    expect(d.suggestion).toBe("play");
  });

  it("rejects expression syntax errors", () => {
    const result = parseGameDocument(base.replace('when: "zones.deck.allEmpty"', 'when: "zones.deck.allEmpty &&"'));
    expect(result.ok).toBe(false);
    if (result.ok) return;
    expect(codes(result.diagnostics)).toContain("EXPRESSION_SYNTAX_ERROR");
  });

  it("rejects 'count' on an owner zone (use totalCount/allEmpty)", () => {
    const result = parseGameDocument(base.replace('when: "zones.deck.allEmpty"', 'when: "zones.deck.count == 0"'));
    expect(result.ok).toBe(false);
    if (result.ok) return;
    const d = result.diagnostics.find((x) => x.code === "EXPRESSION_REF_INVALID")!;
    expect(d.expected).toContain("shared zones");
  });

  it("rejects a winner zone that is not owner: seat", () => {
    const result = parseGameDocument(base.replace("winner: { mostPiecesIn: won }", "winner: { mostPiecesIn: pot }"));
    expect(result.ok).toBe(false);
    if (result.ok) return;
    expect(codes(result.diagnostics)).toContain("SCHEMA_VALIDATION_FAILED");
  });

  it("rejects non-YAML input with INVALID_YAML", () => {
    const result = parseGameDocument("{ not: [valid", { file: "x.yaml" });
    expect(result.ok).toBe(false);
    if (result.ok) return;
    expect(codes(result.diagnostics)).toContain("INVALID_YAML");
  });

  it("requires exactly one Game document per file (v1alpha)", () => {
    const result = parseGameDocument(`${base}\n---\n${base}`);
    expect(result.ok).toBe(false);
    if (result.ok) return;
    const d = result.diagnostics.find((x) => x.code === "INVALID_MANIFEST_ENVELOPE")!;
    expect(d.expected).toContain("exactly one Game document");
  });
});
