// @junction/spec — GameSpec grammar, manifest parser, expression parser, diagnostics kernel.
export {
  DiagnosticCodes,
  formatDiagnosticText,
  hasErrors,
  makeDiagnostic,
  runtimeError,
  suggestFrom,
  validateError,
  validateWarning,
  type Diagnostic,
  type DiagnosticCode,
  type DiagnosticPhase,
  type DiagnosticSeverity,
} from "./kernel/diagnostic.js";
export { err, ok, type Result } from "./kernel/result.js";
export { collectPaths, type BinOp, type Expr } from "./domain/model/expression.js";
export {
  API_VERSION,
  SUPPORTED_KINDS,
  gameSpecSchema,
  type ActionDecl,
  type EffectDecl,
  type GameDocument,
  type GameSpec,
  type PieceDecl,
  type SetupOp,
  type ThemeDecl,
  type TriggerDecl,
  type ZoneDecl,
  type ZoneSel,
} from "./domain/model/gamespec.js";
export { parseExpression, type ParseExprResult } from "./domain/service/expression-parser.js";
export { parseGameDocument, type ParseOptions } from "./domain/service/manifest-parser.js";
