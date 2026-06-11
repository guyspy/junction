#!/usr/bin/env node
// The layering law, enforced (blueprint §7: "biology is the brand; layering is the law").
//   spec     → imports no @junction package, no node: builtins
//   runtime  → imports @junction/spec only, no node: builtins
//   cli      → imports spec + runtime, node: allowed
// spec and runtime must declare "sideEffects": false.
import { readFileSync, readdirSync, statSync } from "node:fs";
import { join } from "node:path";

const RULES = {
  spec: { allow: [], allowNode: false, sideEffectsFree: true },
  runtime: { allow: ["@junction/spec"], allowNode: false, sideEffectsFree: true },
  // Cadherin: the DOM renderer. Browser-only — no node imports, ever.
  renderer: { allow: ["@junction/spec", "@junction/runtime"], allowNode: false, sideEffectsFree: true },
  // Integrin: the MCP server. Imports spec+runtime; the SDK + stdio transport need node.
  mcp: { allow: ["@junction/spec", "@junction/runtime", "@junction/renderer"], allowNode: true, sideEffectsFree: false },
  cli: { allow: ["@junction/spec", "@junction/runtime", "@junction/renderer"], allowNode: true, sideEffectsFree: false },
};
const FORBIDDEN_EVERYWHERE = [/@cloudflare\//, /\bworkerd\b/];

function* walk(dir) {
  for (const entry of readdirSync(dir)) {
    const p = join(dir, entry);
    if (statSync(p).isDirectory()) yield* walk(p);
    else if (p.endsWith(".ts")) yield p;
  }
}

let violations = 0;
const fail = (msg) => {
  console.error(`BOUNDARY VIOLATION: ${msg}`);
  violations++;
};

for (const [pkg, rule] of Object.entries(RULES)) {
  const srcDir = join("packages", pkg, "src");
  for (const file of walk(srcDir)) {
    const text = readFileSync(file, "utf8");
    for (const match of text.matchAll(/(?:from|import)\s+["']([^"']+)["']/g)) {
      const dep = match[1];
      if (dep.startsWith("@junction/") && !rule.allow.includes(dep))
        fail(`${file} imports ${dep} (allowed: ${rule.allow.join(", ") || "none"})`);
      if (!rule.allowNode && dep.startsWith("node:"))
        fail(`${file} imports ${dep} — ${pkg}/src must stay platform-agnostic`);
      for (const re of FORBIDDEN_EVERYWHERE)
        if (re.test(dep)) fail(`${file} imports ${dep} — platform types are adapter-only`);
    }
  }
  const pkgJson = JSON.parse(readFileSync(join("packages", pkg, "package.json"), "utf8"));
  if (rule.sideEffectsFree && pkgJson.sideEffects !== false)
    fail(`packages/${pkg}/package.json must declare "sideEffects": false`);
}

if (violations > 0) {
  console.error(`\n${violations} boundary violation(s).`);
  process.exit(1);
}
console.log("check-boundaries: ok");
