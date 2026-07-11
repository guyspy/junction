import { cloudflareTest, readD1Migrations } from "@cloudflare/vitest-pool-workers";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig(async () => ({
  plugins: [
    cloudflareTest({
      wrangler: { configPath: "./wrangler.jsonc" },
      miniflare: {
        bindings: {
          TEST_MIGRATIONS: await readD1Migrations(fileURLToPath(new URL("./migrations", import.meta.url))),
          JUNCTION_MCP_OWNER_ID: "test-owner",
          JUNCTION_MCP_TOKEN_HMAC: "50c1a23833ae9b4c098c8bea205577dccea447e6a1bc2113787960e1632a0173",
        },
      },
    }),
  ],
  test: { setupFiles: ["./test/apply-migrations.ts"] },
}));
