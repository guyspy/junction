#!/usr/bin/env node
// Bundle the renderer + engine into one browser IIFE for `junction render`.
import { build } from "esbuild";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("..", import.meta.url));

await build({
  entryPoints: [`${root}src/standalone.ts`],
  bundle: true,
  format: "iife",
  platform: "browser",
  target: "es2022",
  minify: true,
  outfile: `${root}dist/standalone.js`,
  logLevel: "warning",
});

console.log("cadherin: dist/standalone.js bundled");
