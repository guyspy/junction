import { copyFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";

const packageDir = fileURLToPath(new URL("..", import.meta.url));
const rendererBundle = fileURLToPath(new URL("../../renderer/dist/standalone.js", import.meta.url));
const pixiBundle = fileURLToPath(new URL("../../renderer/dist/pixi-standalone.js", import.meta.url));

mkdirSync(`${packageDir}public`, { recursive: true });
copyFileSync(rendererBundle, `${packageDir}public/standalone.js`);
copyFileSync(rendererBundle, `${packageDir}public/standalone.txt`);
copyFileSync(pixiBundle, `${packageDir}public/pixi.js`);
