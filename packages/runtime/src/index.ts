// @junction/runtime — the deterministic game kernel (Catenin's reducer half).
export { createRng, shuffled, type Rng } from "./kernel/rng.js";
export { zoneKey, type GameState, type GameStatus, type PieceInstance, type ZoneEntry } from "./domain/model/state.js";
export { type GameEvent, type GameEventType, type PieceView } from "./domain/model/events.js";
export { EvalError, evaluate, evaluateBoolean } from "./domain/service/expression-evaluator.js";
export { SetupError, buildInitialState, type SetupResult } from "./domain/service/setup.js";
export {
  applyAction,
  applySkip,
  legalActions,
  type PlayerAction,
  type StepResult,
} from "./domain/service/reducer.js";
export { randomChooser, runGame, type Chooser, type RunGameOptions, type RunGameResult } from "./usecase/run-game.js";
export { simulate, type SimulateOptions, type SimulateReport } from "./usecase/simulate.js";
