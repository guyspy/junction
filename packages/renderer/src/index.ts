// @junction/renderer — Cadherin: Pixi visuals with accessible DOM controls.
export { announce, announceAll } from "./announcer.js";
export { cardBackSVG, cardFaceSVG } from "./art.js";
export { confettiBurst, scorePop } from "./celebrate.js";
export {
  mountGame,
  mountOnlineGame,
  type GameController,
  type MountOptions,
  type OnlineMountOptions,
  type VisualAdapter,
} from "./dom-renderer.js";
export { buildGamePageHtml, buildOnlinePageHtml, computeQaBadges, type GamePageInput, type OnlinePageInput, type QaBadgeOptions } from "./page.js";
export { createSoundBank, soundForEvent, type SoundBank, type SoundName, type SoundSetName } from "./sound.js";
export { CADHERIN_CSS } from "./styles.js";
export {
  applyThemeTokens,
  celebrationColors,
  motionParams,
  resolveTheme,
  type MotionParams,
  type Theme,
} from "./theme.js";
export {
  buildViewModel,
  buildViewModelFromProjection,
  cardLabel,
  cardShortText,
  type CardVM,
  type MoveButtonVM,
  type ViewModel,
  type ZoneKind,
  type ZoneVM,
} from "./view-model.js";
