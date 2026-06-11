// @junction/mcp — Integrin, the Junction MCP server (the agent-native front door).
export { buildIntegrinServer, type IntegrinDeps } from "./server.js";
export {
  runDescribeGrammar,
  runGetReference,
  runListReferences,
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
