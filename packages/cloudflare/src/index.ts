import { isValidJoinCode } from "@junction/rooms";
import { GAMES, isGameName } from "./games.js";
import { GameRoom } from "./game-room.js";

export { GameRoom };

const CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
const CODE_LENGTH = 5;

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    try {
      if (request.method === "POST" && url.pathname === "/api/rooms")
        return await createRoom(url, env);

      if (request.method === "GET" && url.pathname === "/ws") {
        const code = (url.searchParams.get("code") ?? "").toUpperCase();
        if (!isValidJoinCode(code))
          return Response.json({ error: "A valid five-character room code is required." }, { status: 400 });
        return env.ROOMS.getByName(code).fetch(request);
      }

      if (request.method === "GET" && url.pathname === "/check")
        return Response.json({ ok: true, websocket: "/ws?code=ABCDE", games: Object.keys(GAMES) });

      return env.ASSETS.fetch(request);
    } catch (error) {
      console.error(
        JSON.stringify({
          message: "request failed",
          method: request.method,
          path: url.pathname,
          error: error instanceof Error ? error.message : String(error),
        }),
      );
      return Response.json({ error: "Internal server error." }, { status: 500 });
    }
  },
} satisfies ExportedHandler<Env>;

async function createRoom(url: URL, env: Env): Promise<Response> {
  const game = url.searchParams.get("game") ?? "war";
  if (!isGameName(game))
    return Response.json({ error: `Unknown game '${game}'.` }, { status: 400 });

  const seatsParam = url.searchParams.get("seats");
  const seats = seatsParam === null ? null : Number(seatsParam);
  if (seats !== null && !Number.isInteger(seats))
    return Response.json({ error: "Seats must be an integer." }, { status: 400 });

  for (let attempt = 0; attempt < 8; attempt++) {
    const code = makeRoomCode();
    const result = await env.ROOMS.getByName(code).initialize(GAMES[game], seats, crypto.randomUUID());
    if (result.error !== undefined) return Response.json({ error: result.error }, { status: 400 });
    if (result.created)
      return Response.json(
        { code, game, title: result.title, seats: result.seats, url: `${url.origin}/?code=${code}` },
        { status: 201 },
      );
  }
  return Response.json({ error: "Could not allocate a unique room code." }, { status: 503 });
}

function makeRoomCode(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(CODE_LENGTH));
  return Array.from(bytes, (byte) => CODE_ALPHABET[byte % CODE_ALPHABET.length]!).join("");
}
