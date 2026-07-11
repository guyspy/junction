import { createPixiVisualAdapter } from "./pixi-adapter.js";

declare global {
  interface Window {
    JunctionPixi: { createVisualAdapter: typeof createPixiVisualAdapter };
  }
}

window.JunctionPixi = { createVisualAdapter: createPixiVisualAdapter };
