import { buildMcpServer } from "@junction/mcp";
import { WebStandardStreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/webStandardStreamableHttp.js";
import rendererBundle from "../public/standalone.txt";
import { D1GameStore } from "./game-store.js";
import { REFERENCE_GAMES } from "./games.js";

const encoder = new TextEncoder();

export async function handleMcp(request: Request, env: Env): Promise<Response> {
  const origin = request.headers.get("Origin");
  if (origin !== null && origin !== new URL(request.url).origin)
    return Response.json({ error: "Origin is not allowed." }, { status: 403 });

  const token = bearerToken(request);
  if (token === null || !(await tokenMatches(token, env.JUNCTION_MCP_TOKEN_HMAC)))
    return Response.json(
      { error: "A valid Junction bearer token is required." },
      { status: 401, headers: { "WWW-Authenticate": 'Bearer realm="junction-mcp"' } },
    );

  const server = buildMcpServer({
    referenceGames: REFERENCE_GAMES,
    rendererBundle,
    authoring: {
      ownerId: env.JUNCTION_MCP_OWNER_ID,
      store: new D1GameStore(env.GAMES),
      playBaseUrl: new URL(request.url).origin,
    },
  });
  const transport = new WebStandardStreamableHTTPServerTransport({
    sessionIdGenerator: undefined,
    enableJsonResponse: true,
  });
  await server.connect(transport);
  return transport.handleRequest(request, {
    authInfo: {
      token,
      clientId: "junction-coding-agent",
      scopes: ["games:read", "games:write", "games:publish"],
      extra: { ownerId: env.JUNCTION_MCP_OWNER_ID },
    },
  });
}

function bearerToken(request: Request): string | null {
  const header = request.headers.get("Authorization");
  if (header === null || !header.startsWith("Bearer ")) return null;
  const token = header.slice(7).trim();
  return token.length === 0 ? null : token;
}

async function tokenMatches(token: string, expectedHex: string): Promise<boolean> {
  const keyBytes = hexBytes(token);
  const signature = hexBytes(expectedHex);
  if (keyBytes === null || signature === null) return false;
  const key = await crypto.subtle.importKey("raw", keyBytes, { name: "HMAC", hash: "SHA-256" }, false, ["verify"]);
  return crypto.subtle.verify("HMAC", key, signature, encoder.encode("junction-mcp"));
}

function hexBytes(value: string): Uint8Array<ArrayBuffer> | null {
  if (!/^[0-9a-f]+$/i.test(value) || value.length % 2 !== 0) return null;
  return Uint8Array.from(value.match(/.{2}/g)!, (byte) => Number.parseInt(byte, 16));
}
