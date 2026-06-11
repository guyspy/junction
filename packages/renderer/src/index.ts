// @junction/renderer — Cadherin: the accessible DOM renderer.
export { announce, announceAll } from "./announcer.js";
export { cardBackSVG, cardFaceSVG } from "./art.js";
export { confettiBurst, scorePop } from "./celebrate.js";
export { mountGame, type GameController, type MountOptions } from "./dom-renderer.js";
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
  cardLabel,
  cardShortText,
  type CardVM,
  type MoveButtonVM,
  type ViewModel,
  type ZoneKind,
  type ZoneVM,
} from "./view-model.js";
