// @junction/connexon — the online runtime: platform-agnostic room core + protocol.
// Transport adapters (Durable Objects, Node ws) wrap the Room; nothing here imports a platform.
export { Room, type RoomConfig, type RoomHooks } from "./core/room.js";
export { HandleCodec } from "./core/handles.js";
export { RoomManager, type ManagerHooks, type OpenRoomInput } from "./core/room-manager.js";
export { autoNickname, isValidJoinCode, makeJoinCode } from "./core/join-code.js";
export {
  encode,
  parseClientMessage,
  type ClientMessage,
  type ErrorMessage,
  type PatchMessage,
  type RoomInfoMessage,
  type ServerMessage,
  type WelcomeMessage,
} from "./protocol/messages.js";
