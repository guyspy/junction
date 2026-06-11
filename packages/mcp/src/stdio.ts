#!/usr/bin/env node
import { readdirSync, readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { parseGameDocument } from "@junction/spec";
import { buildIntegrinServer } from "./server.js";
import type { ReferenceGame } from "./tools.js";

/**
 * Integrin over stdio — connect from Claude Code/Desktop with:
 *   { "command": "node", "args": ["<repo>/packages/mcp/dist/stdio.js"] }
 *
 * Loads the reference-game corpus from <repo>/games and injects it. NOTE: stdout is the
 * JSON-RPC channel — all logging goes to stderr.
 */

function loadReferenceGames(): ReferenceGame[] {
  const dir = fileURLToPath(new URL("../../../games/", import.meta.url));
  const out: ReferenceGame[] = [];
  let files: string[] = [];
  try {
    files = readdirSync(dir).filter((f) => f.endsWith(".yaml") || f.endsWith(".yml"));
  } catch {
    console.error(`integrin: no games dir at ${dir} — starting with no reference games.`);
    return out;
  }
  for (const file of files.sort()) {
    const yaml = readFileSync(dir + file, "utf8");
    const parsed = parseGameDocument(yaml, { file });
    if (!parsed.ok) {
      console.error(`integrin: skipping ${file} — does not validate.`);
      continue;
    }
    out.push({
      name: parsed.data.metadata.name,
      title: parsed.data.spec.meta.title,
      description: parsed.data.spec.meta.description ?? "",
      yaml,
    });
  }
  return out;
}

function loadRendererBundle(): string | undefined {
  try {
    const require = createRequire(import.meta.url);
    return readFileSync(require.resolve("@junction/renderer/standalone.js"), "utf8");
  } catch {
    console.error("integrin: renderer bundle unavailable — render_game disabled.");
    return undefined;
  }
}

async function main(): Promise<void> {
  const referenceGames = loadReferenceGames();
  const rendererBundle = loadRendererBundle();
  const server = buildIntegrinServer({ referenceGames, rendererBundle });
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error(
    `integrin: ready over stdio — ${referenceGames.length} reference game(s): ${referenceGames.map((r) => r.name).join(", ") || "none"}${rendererBundle !== undefined ? " · render_game enabled" : ""}`,
  );
}

main().catch((error: unknown) => {
  console.error(`integrin: fatal — ${error instanceof Error ? error.message : String(error)}`);
  process.exit(1);
});
