// @junction/mcp — the Junction MCP server.
export {
  buildMcpServer,
  type AuthoringDeps,
  type GameStore,
  type McpServerDeps,
  type StoredGame,
  type StoreResult,
} from "./server.js";
export {
  runDescribeGrammar,
  runGetReference,
  runListReferences,
  runRenderGame,
  runScaffold,
  runSimulate,
  runValidate,
  type ReferenceGame,
  type ScaffoldOutput,
  type SimulateInput,
  type SimulateOutput,
  type ToolResult,
  type ValidateOutput,
} from "./tools.js";
export { scaffoldGame, type ScaffoldInput, type ScaffoldResult } from "./scaffold.js";
export { describeGrammar, GRAMMAR_REFERENCE, type GrammarReference } from "./grammar-reference.js";
