import { applyD1Migrations } from "cloudflare:test";
import { env } from "cloudflare:workers";

await applyD1Migrations(env.GAMES, env.TEST_MIGRATIONS);
