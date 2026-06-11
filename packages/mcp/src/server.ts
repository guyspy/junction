import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import {
  runDescribeGrammar,
  runGetReference,
  runListReferences,
  runScaffold,
  runSimulate,
  runValidate,
  type ReferenceGame,
  type ToolResult,
} from "./tools.js";

/**
 * Integrin — the Junction MCP server. The agent-native front door: every authoring
 * capability the kernel has, exposed as a tool. The studio's embedded agent and any
 * external agent (Claude, ChatGPT) consume the same tools (commitment #1: parity).
 */

export interface IntegrinDeps {
  /** The reference-game corpus, injected so tools stay pure and Workers-portable. */
  readonly referenceGames: readonly ReferenceGame[];
  readonly version?: string;
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

export function buildIntegrinServer(deps: IntegrinDeps): McpServer {
  const server = new McpServer({ name: "junction-integrin", version: deps.version ?? "0.0.1-alpha.0" });

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

  return server;
}
