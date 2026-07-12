import war from "../../../games/war.yaml";
import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import worker from "../src/index.js";

const TOKEN = "00".repeat(32);

interface McpReply {
  result?: {
    tools?: { name: string }[];
    structuredContent?: Record<string, unknown>;
    isError?: boolean;
  };
}

describe("remote Junction MCP", () => {
  it("requires a bearer token and rejects foreign browser origins", async () => {
    const missing = await mcp("tools/list", {}, null);
    expect(missing.status).toBe(401);

    const foreign = await worker.fetch(
      new Request("https://example.com/mcp", {
        method: "POST",
        headers: mcpHeaders(TOKEN, "https://evil.example"),
        body: JSON.stringify(message("tools/list", {})),
      }),
      env,
    );
    expect(foreign.status).toBe(403);
  });

  it("creates, revises, publishes, and hosts an owned YAML game", async () => {
    const listed = await mcp("tools/list");
    expect(listed.status).toBe(200);
    const advertised = ((await listed.json<McpReply>()).result?.tools ?? []).map((tool) => tool.name);
    expect(advertised).toEqual(
      expect.arrayContaining(["describe_grammar", "create_game", "get_game", "list_my_games", "update_game", "publish_game"]),
    );

    const created = await callTool("create_game", { yaml: war });
    const game = created.structuredContent?.["game"] as { id: string; revision: number; status: string };
    expect(game).toMatchObject({ revision: 1, status: "draft" });

    const revisedYaml = war.replace('title: "War (Battle Variant)"', 'title: "Agent War"');
    const updated = await callTool("update_game", { id: game.id, expectedRevision: 1, yaml: revisedYaml });
    expect(updated.structuredContent?.["game"]).toMatchObject({ revision: 2, title: "Agent War", status: "draft" });

    const stale = await callTool("update_game", { id: game.id, expectedRevision: 1, yaml: revisedYaml });
    expect(stale.isError).toBe(true);
    expect(stale.structuredContent).toMatchObject({ code: "REVISION_CONFLICT" });

    const published = await callTool("publish_game", { id: game.id, expectedRevision: 2, games: 10, seed: "mcp-test" });
    expect(published.isError).toBeFalsy();
    expect(published.structuredContent?.["game"]).toMatchObject({ status: "published", publishedRevision: 2 });
    expect(published.structuredContent?.["playUrl"]).toBe(`https://example.com/?gameId=${game.id}&revision=2`);

    const room = await worker.fetch(
      new Request(`https://example.com/api/rooms?gameId=${game.id}&revision=2`, { method: "POST" }),
      env,
    );
    expect(room.status).toBe(201);
    await expect(room.json()).resolves.toMatchObject({ title: "Agent War", game: game.id });
  });
});

async function callTool(name: string, args: Record<string, unknown>): Promise<NonNullable<McpReply["result"]>> {
  const response = await mcp("tools/call", { name, arguments: args });
  expect(response.status).toBe(200);
  const body = await response.json<McpReply>();
  expect(body.result).toBeDefined();
  return body.result!;
}

function mcp(method: string, params: Record<string, unknown> = {}, token: string | null = TOKEN): Promise<Response> {
  return worker.fetch(
    new Request("https://example.com/mcp", {
      method: "POST",
      headers: mcpHeaders(token),
      body: JSON.stringify(message(method, params)),
    }),
    env,
  );
}

function message(method: string, params: Record<string, unknown>): Record<string, unknown> {
  return { jsonrpc: "2.0", id: crypto.randomUUID(), method, params };
}

function mcpHeaders(token: string | null, origin?: string): Headers {
  const headers = new Headers({
    Accept: "application/json, text/event-stream",
    "Content-Type": "application/json",
    "MCP-Protocol-Version": "2025-11-25",
  });
  if (token !== null) headers.set("Authorization", `Bearer ${token}`);
  if (origin !== undefined) headers.set("Origin", origin);
  return headers;
}
