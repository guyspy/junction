import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  runDescribeGrammar,
  runGetReference,
  runListReferences,
  runRenderGame,
  runScaffold,
  runSimulate,
  runValidate,
  type ReferenceGame,
} from "../src/index.js";

function loadRefs(): ReferenceGame[] {
  const names = ["war", "memory-match"];
  return names.map((name) => {
    const yaml = readFileSync(fileURLToPath(new URL(`../../../games/${name}.yaml`, import.meta.url)), "utf8");
    return { name, title: name, description: "", yaml };
  });
}

describe("Junction MCP tools (pure layer)", () => {
  it("describe_grammar returns the closed vocabulary", () => {
    const r = runDescribeGrammar();
    expect(r.ok).toBe(true);
    expect(r.structured.apiVersion).toBe("games.junction.aotter.net/v1alpha1");
    expect(r.structured.zones.owner).toEqual(["shared", "seat"]);
    expect(r.structured.effects.map((e) => e.name)).toContain("resolveEqualPair");
  });

  it("validate_game accepts a real game and reports its name", () => {
    const refs = loadRefs();
    const r = runValidate(refs[0]!.yaml);
    expect(r.ok).toBe(true);
    expect(r.structured.game).toBe("war");
  });

  it("validate_game returns actionable diagnostics for a broken game", () => {
    const broken = loadRefs()[0]!.yaml.replace("zone: deck, owner: actor", "zone: dekk, owner: actor");
    const r = runValidate(broken);
    expect(r.ok).toBe(false);
    expect(r.structured.diagnostics.some((d) => d.code === "ZONE_REF_UNKNOWN" && d.suggestion === "deck")).toBe(true);
    expect(r.summary).toContain("Invalid");
  });

  it("simulate_game refuses an invalid game (validate first)", () => {
    const r = runSimulate({ yaml: "not: a game" });
    expect(r.ok).toBe(false);
    expect(r.summary).toContain("invalid");
  });

  it("simulate_game reports termination and balance for a valid game", () => {
    const r = runSimulate({ yaml: loadRefs()[0]!.yaml, games: 100, seed: "t" });
    expect(r.ok).toBe(true);
    expect(r.structured.report?.capped).toBe(0);
    expect(r.structured.report?.completed).toBe(100);
    expect(r.summary).toContain("Simulated 100 games");
  });

  it("scaffold_game produces a valid, terminating card game", () => {
    const scaffold = runScaffold({ genre: "card_game", title: "Sum Sprint" });
    expect(scaffold.ok).toBe(true);
    expect(scaffold.structured.name).toBe("sum-sprint");

    const validated = runValidate(scaffold.structured.yaml);
    expect(validated.ok).toBe(true);

    const sim = runSimulate({ yaml: scaffold.structured.yaml, games: 50, seed: "s" });
    expect(sim.structured.report?.capped).toBe(0);
  });

  it("reference tools list and fetch by name", () => {
    const refs = loadRefs();
    const list = runListReferences(refs);
    expect(list.structured.games.map((g) => g.name)).toEqual(["war", "memory-match"]);

    const got = runGetReference(refs, "memory-match");
    expect(got.ok).toBe(true);
    expect(got.structured.yaml).toContain("kind: Game");

    const missing = runGetReference(refs, "nope");
    expect(missing.ok).toBe(false);
  });
});

describe("render_game (the MCP Apps spike)", () => {
  const FAKE_BUNDLE = "window.JunctionGame={boot(){}};";

  it("renders a reference game into a ui:// playable page", () => {
    const refs = loadRefs();
    const page = runRenderGame(refs, FAKE_BUNDLE, { game: "war" });
    expect(page.result.ok).toBe(true);
    expect(page.uri).toBe("ui://junction/games/war");
    expect(page.html).toContain("<!doctype html>");
    expect(page.html).toContain("JunctionGame.boot");
    expect(page.html).toContain(FAKE_BUNDLE);
    expect(page.result.structured.badges?.some((b) => b.includes("always ends"))).toBe(true);
  });

  it("refuses invalid YAML with diagnostics and degrades gracefully without a bundle", () => {
    const refs = loadRefs();
    const bad = runRenderGame(refs, FAKE_BUNDLE, { yaml: "not: [valid" });
    expect(bad.result.ok).toBe(false);
    expect(bad.html).toBeUndefined();

    const disabled = runRenderGame(refs, undefined, { game: "war" });
    expect(disabled.result.ok).toBe(false);
    expect(disabled.result.summary).toContain("not available");
  });
});
