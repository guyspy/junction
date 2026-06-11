import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { buildIntegrinServer, type ReferenceGame } from "../src/index.js";

/** Drive the real MCP server in-process over a linked in-memory transport. */
function loadRefs(): ReferenceGame[] {
  return ["war", "memory-match"].map((name) => ({
    name,
    title: name,
    description: "ref",
    yaml: readFileSync(fileURLToPath(new URL(`../../../games/${name}.yaml`, import.meta.url)), "utf8"),
  }));
}

interface ToolReply {
  content: { type: string; text: string }[];
  structuredContent?: Record<string, unknown>;
  isError?: boolean;
}

describe("Integrin MCP server (end-to-end over a real client)", () => {
  let client: Client;

  beforeEach(async () => {
    const server = buildIntegrinServer({ referenceGames: loadRefs() });
    client = new Client({ name: "test-agent", version: "0.0.0" });
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    await server.connect(serverTransport);
    await client.connect(clientTransport);
  });

  afterEach(async () => {
    await client.close();
  });

  const call = (name: string, args: Record<string, unknown> = {}): Promise<ToolReply> =>
    client.callTool({ name, arguments: args }) as Promise<ToolReply>;

  it("advertises the six authoring tools", async () => {
    const { tools } = await client.listTools();
    expect(tools.map((t) => t.name).sort()).toEqual(
      ["describe_grammar", "get_reference_game", "list_reference_games", "scaffold_game", "simulate_game", "validate_game"].sort(),
    );
  });

  it("the full agent authoring loop: grammar → scaffold → validate → simulate", async () => {
    // 1. Learn the vocabulary.
    const grammar = await call("describe_grammar");
    expect(grammar.structuredContent?.["apiVersion"]).toBe("games.junction.aotter.net/v1alpha1");

    // 2. Scaffold a new game.
    const scaffold = await call("scaffold_game", { genre: "card_game", title: "Fraction Duel" });
    const yaml = scaffold.structuredContent?.["yaml"] as string;
    expect(yaml).toContain("kind: Game");

    // 3. Validate it.
    const validated = await call("validate_game", { yaml });
    expect(validated.isError).toBeFalsy();
    expect(validated.structuredContent?.["game"]).toBe("fraction-duel");

    // 4. Simulate it — the playtest gate.
    const sim = (await call("simulate_game", { yaml, games: 80, seed: "e2e" })).structuredContent;
    const report = sim?.["report"] as { capped: number; completed: number };
    expect(report.capped).toBe(0);
    expect(report.completed).toBe(80);
  });

  it("surfaces a tool error (isError) with actionable diagnostics", async () => {
    const broken = "apiVersion: games.junction.aotter.net/v1alpha1\nkind: Game\nmetadata: { name: x }\nspec: {}";
    const result = await call("validate_game", { yaml: broken });
    expect(result.isError).toBe(true);
    expect(result.content[0]?.text).toContain("Invalid");
  });

  it("reference games are reachable for few-shot grounding", async () => {
    const list = (await call("list_reference_games")).structuredContent as { games: { name: string }[] };
    expect(list.games.map((g) => g.name)).toContain("war");
    const got = await call("get_reference_game", { name: "war" });
    expect(got.structuredContent?.["yaml"]).toContain("resolveHighest");
  });
});
