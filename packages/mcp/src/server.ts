import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import {
  runDescribeGrammar,
  runGetReference,
  runListReferences,
  runRenderGame,
  runScaffold,
  runSimulate,
  runValidate,
  type ReferenceGame,
  type ToolResult,
} from "./tools.js";

/**
 * The Junction MCP server exposes the engine's authoring capabilities as tools.
 */

export interface McpServerDeps {
  /** The reference-game corpus, injected so tools stay pure and Workers-portable. */
  readonly referenceGames: readonly ReferenceGame[];
  /** The standalone renderer+engine IIFE (contents of @junction/renderer/standalone.js), injected the same way. Enables render_game. */
  readonly rendererBundle?: string;
  readonly version?: string;
  readonly authoring?: AuthoringDeps;
}

export interface StoredGame {
  readonly id: string;
  readonly name: string;
  readonly title: string;
  readonly yaml: string;
  readonly revision: number;
  readonly status: "draft" | "published";
  readonly publishedRevision: number | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export type StoreResult<T> =
  | { readonly ok: true; readonly value: T }
  | { readonly ok: false; readonly code: "NOT_FOUND" | "REVISION_CONFLICT"; readonly message: string };

export interface GameStore {
  readonly create: (
    ownerId: string,
    game: { readonly name: string; readonly title: string; readonly yaml: string },
  ) => Promise<StoredGame>;
  readonly get: (ownerId: string, id: string) => Promise<StoredGame | undefined>;
  readonly list: (ownerId: string) => Promise<readonly StoredGame[]>;
  readonly update: (
    ownerId: string,
    id: string,
    expectedRevision: number,
    game: { readonly name: string; readonly title: string; readonly yaml: string },
  ) => Promise<StoreResult<StoredGame>>;
  readonly publish: (
    ownerId: string,
    id: string,
    expectedRevision: number,
  ) => Promise<StoreResult<StoredGame>>;
}

export interface AuthoringDeps {
  readonly ownerId: string;
  readonly store: GameStore;
  readonly playBaseUrl: string;
}

/** Wrap a pure ToolResult as an MCP tool response (text summary + structured content). */
function reply<T>(result: ToolResult<T>): {
  content: { type: "text"; text: string }[];
  structuredContent: Record<string, unknown>;
  isError?: boolean;
} {
  return {
    content: [{ type: "text", text: result.summary }],
    structuredContent: result.structured as Record<string, unknown>,
    ...(result.ok ? {} : { isError: true }),
  };
}

export function buildMcpServer(deps: McpServerDeps): McpServer {
  const server = new McpServer({ name: "junction-mcp", version: deps.version ?? "0.0.1-alpha.0" });

  server.registerTool(
    "describe_grammar",
    {
      title: "Describe the GameSpec grammar",
      description:
        "Return the complete closed vocabulary of GameSpec v1alpha: zone kinds, piece properties, setup ops, action/effect/trigger types, and the expression language. Call this first when authoring a game so you never guess at keys or enum values.",
      inputSchema: {},
    },
    () => reply(runDescribeGrammar()),
  );

  server.registerTool(
    "list_reference_games",
    {
      title: "List reference games",
      description: "List the built-in, fully-worked reference games you can study and adapt.",
      inputSchema: {},
    },
    () => reply(runListReferences(deps.referenceGames)),
  );

  server.registerTool(
    "get_reference_game",
    {
      title: "Get a reference game",
      description: "Fetch the full YAML of a reference game by name (use list_reference_games to see names). Study these before writing your own.",
      inputSchema: { name: z.string().describe("Reference game name, e.g. 'war' or 'memory-match'.") },
    },
    ({ name }) => reply(runGetReference(deps.referenceGames, name)),
  );

  server.registerTool(
    "scaffold_game",
    {
      title: "Scaffold a new game",
      description:
        "Produce a valid, playable, guaranteed-terminating skeleton GameSpec to start from. Returns YAML you then customize. Always begin a new game here rather than writing from scratch.",
      inputSchema: {
        genre: z.literal("card_game").describe("The game genre. v1alpha supports: card_game."),
        title: z.string().describe("Human title, e.g. 'Fraction Duel'."),
        description: z.string().optional().describe("One or two sentences on the game and its learning goal."),
        seatsMin: z.number().int().min(1).optional().describe("Minimum players (default 2)."),
        seatsMax: z.number().int().min(1).optional().describe("Maximum players (default max(min,4))."),
        handSize: z.number().int().min(1).optional().describe("Cards dealt per seat; the game lasts this many rounds (default 4)."),
        valueMax: z.number().int().min(2).optional().describe("Card values run 1..valueMax (default 10)."),
      },
    },
    (args) => reply(runScaffold(args)),
  );

  server.registerTool(
    "validate_game",
    {
      title: "Validate a game",
      description:
        "Check a GameSpec (YAML) against the grammar and semantic lints. Returns structured diagnostics with a path, what was expected, candidates, and a suggested fix for each problem. Call after every edit.",
      inputSchema: { yaml: z.string().describe("The full GameSpec YAML document.") },
    },
    ({ yaml }) => reply(runValidate(yaml)),
  );

  server.registerTool(
    "simulate_game",
    {
      title: "Simulate a game",
      description:
        "Play a GameSpec headlessly many times with random players and report termination, seat balance (first-player fairness), turn length, action usage, and draw rate. This is the playtest gate — a game should terminate 100% and be roughly balanced before you publish.",
      inputSchema: {
        yaml: z.string().describe("The full GameSpec YAML document."),
        games: z.number().int().min(1).optional().describe("How many games to play (default 200, max 5000)."),
        seats: z.number().int().min(1).optional().describe("Seat count to simulate (default the game's minimum)."),
        seed: z.string().optional().describe("Seed for reproducibility."),
      },
    },
    ({ yaml, games, seats, seed }) => reply(runSimulate({ yaml, games, seats, seed })),
  );

  server.registerTool(
    "render_game",
    {
      title: "Render a playable game page",
      description:
        "Turn a valid GameSpec into a complete, self-contained playable HTML page (engine in-browser, themed UI, synth sound, QA badges). Returns it as a ui:// text/html resource — hosts with MCP Apps support can open it in a sandboxed iframe so the game is playable right in the conversation. Pass `yaml`, or `game` to render a reference game.",
      inputSchema: {
        yaml: z.string().optional().describe("A full GameSpec YAML document (validate first)."),
        game: z.string().optional().describe("Or: a reference game name (see list_reference_games)."),
        seed: z.string().optional().describe("Badge-simulation seed."),
      },
    },
    ({ yaml, game, seed }) => {
      const page = runRenderGame(deps.referenceGames, deps.rendererBundle, { yaml, game, seed });
      const base = reply(page.result);
      if (page.html === undefined || page.uri === undefined) return base;
      return {
        ...base,
        content: [
          ...base.content,
          {
            type: "resource" as const,
            resource: { uri: page.uri, mimeType: "text/html", text: page.html },
          },
        ],
      };
    },
  );

  if (deps.authoring !== undefined) registerAuthoringTools(server, deps.authoring);

  return server;
}

function registerAuthoringTools(server: McpServer, deps: AuthoringDeps): void {
  server.registerTool(
    "create_game",
    {
      title: "Create an owned game draft",
      description:
        "Validate and save a full GameSpec YAML document as a new private draft. Returns its stable game ID and revision. Use scaffold_game first when starting from an idea.",
      inputSchema: { yaml: z.string().describe("The complete GameSpec YAML document.") },
    },
    async ({ yaml }) => {
      const validated = runValidate(yaml);
      if (!validated.ok || validated.structured.game === undefined || validated.structured.title === undefined)
        return reply(validated);
      const game = await deps.store.create(deps.ownerId, {
        name: validated.structured.game,
        title: validated.structured.title,
        yaml,
      });
      return reply({
        ok: true,
        summary: `Created draft '${game.title}' at revision ${game.revision}.`,
        structured: { game },
      });
    },
  );

  server.registerTool(
    "get_game",
    {
      title: "Get an owned game",
      description: "Fetch the current YAML and metadata for one of your games by its stable ID.",
      inputSchema: { id: z.string().uuid().describe("Stable game ID returned by create_game or list_my_games.") },
    },
    async ({ id }) => {
      const game = await deps.store.get(deps.ownerId, id);
      return game === undefined
        ? reply(toolError("NOT_FOUND", `Game '${id}' was not found.`))
        : reply({ ok: true, summary: `Fetched '${game.title}' revision ${game.revision}.`, structured: { game } });
    },
  );

  server.registerTool(
    "list_my_games",
    {
      title: "List owned games",
      description: "List your saved Junction game drafts and published revisions.",
      inputSchema: {},
    },
    async () => {
      const games = await deps.store.list(deps.ownerId);
      return reply({
        ok: true,
        summary: `${games.length} saved game(s).`,
        structured: { games },
      });
    },
  );

  server.registerTool(
    "update_game",
    {
      title: "Revise an owned game draft",
      description:
        "Validate and replace a game's complete YAML. expectedRevision prevents an agent from overwriting a newer edit; fetch the game again if it conflicts.",
      inputSchema: {
        id: z.string().uuid().describe("Stable game ID."),
        expectedRevision: z.number().int().min(1).describe("Current revision from get_game."),
        yaml: z.string().describe("Complete replacement GameSpec YAML."),
      },
    },
    async ({ id, expectedRevision, yaml }) => {
      const validated = runValidate(yaml);
      if (!validated.ok || validated.structured.game === undefined || validated.structured.title === undefined)
        return reply(validated);
      const result = await deps.store.update(deps.ownerId, id, expectedRevision, {
        name: validated.structured.game,
        title: validated.structured.title,
        yaml,
      });
      return result.ok
        ? reply({
            ok: true,
            summary: `Updated '${result.value.title}' to revision ${result.value.revision}.`,
            structured: { game: result.value },
          })
        : reply(toolError(result.code, result.message));
    },
  );

  server.registerTool(
    "publish_game",
    {
      title: "Publish a validated game revision",
      description:
        "Run the simulation termination gate and publish the current revision. Returns an immutable hosted-play URL for that revision.",
      inputSchema: {
        id: z.string().uuid().describe("Stable game ID."),
        expectedRevision: z.number().int().min(1).describe("Revision to publish."),
        games: z.number().int().min(1).max(5000).optional().describe("Simulation runs (default 200)."),
        seed: z.string().optional().describe("Reproducible simulation seed."),
      },
    },
    async ({ id, expectedRevision, games, seed }) => {
      const game = await deps.store.get(deps.ownerId, id);
      if (game === undefined) return reply(toolError("NOT_FOUND", `Game '${id}' was not found.`));
      if (game.revision !== expectedRevision)
        return reply(
          toolError(
            "REVISION_CONFLICT",
            `Expected revision ${expectedRevision}, but '${game.title}' is at revision ${game.revision}.`,
          ),
        );

      const simulation = runSimulate({ yaml: game.yaml, games, seed });
      if (!simulation.ok || simulation.structured.report === undefined) return reply(simulation);
      if (simulation.structured.report.capped > 0 || simulation.structured.report.stalled > 0)
        return reply({
          ok: false,
          summary: `Publish blocked: ${simulation.structured.report.capped} simulation(s) hit the turn cap and ${simulation.structured.report.stalled} ended via the stall guard.`,
          structured: { code: "SIMULATION_DID_NOT_TERMINATE_CLEANLY", report: simulation.structured.report },
        });

      const result = await deps.store.publish(deps.ownerId, id, expectedRevision);
      if (!result.ok) return reply(toolError(result.code, result.message));
      const playUrl = `${deps.playBaseUrl}/?gameId=${encodeURIComponent(id)}&revision=${result.value.revision}`;
      return reply({
        ok: true,
        summary: `Published '${result.value.title}' revision ${result.value.revision}: ${playUrl}`,
        structured: { game: result.value, report: simulation.structured.report, playUrl },
      });
    },
  );
}

function toolError(code: string, message: string): ToolResult<{ code: string }> {
  return { ok: false, summary: message, structured: { code } };
}
