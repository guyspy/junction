import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: {
    alias: {
      "@junction/spec": fileURLToPath(new URL("../spec/src/index.ts", import.meta.url)),
      "@junction/runtime": fileURLToPath(new URL("../runtime/src/index.ts", import.meta.url)),
      "@junction/rooms": fileURLToPath(new URL("../rooms/src/index.ts", import.meta.url)),
      "@junction/renderer": fileURLToPath(new URL("../renderer/src/index.ts", import.meta.url)),
    },
  },
  test: { include: ["test/**/*.test.ts"], testTimeout: 20000 },
});
