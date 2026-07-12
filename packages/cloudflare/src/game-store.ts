import type { GameStore, StoredGame, StoreResult } from "@junction/mcp";

interface GameRow {
  id: string;
  name: string;
  title: string;
  yaml: string;
  revision: number;
  status: "draft" | "published";
  published_revision: number | null;
  created_at: string;
  updated_at: string;
}

export interface PublishedGame {
  readonly id: string;
  readonly revision: number;
  readonly yaml: string;
}

export class D1GameStore implements GameStore {
  constructor(private readonly db: D1Database) {}

  async create(
    ownerId: string,
    input: { readonly name: string; readonly title: string; readonly yaml: string },
  ): Promise<StoredGame> {
    const id = crypto.randomUUID();
    const writeToken = crypto.randomUUID();
    const now = new Date().toISOString();
    await this.db.batch([
      this.db
        .prepare(
          `INSERT INTO games
             (id, owner_id, name, title, yaml, revision, status, published_revision, write_token, created_at, updated_at)
           VALUES (?, ?, ?, ?, ?, 1, 'draft', NULL, ?, ?, ?)`,
        )
        .bind(id, ownerId, input.name, input.title, input.yaml, writeToken, now, now),
      this.db
        .prepare("INSERT INTO game_revisions (game_id, revision, yaml, created_at) VALUES (?, 1, ?, ?)")
        .bind(id, input.yaml, now),
    ]);
    return (await this.get(ownerId, id))!;
  }

  async get(ownerId: string, id: string): Promise<StoredGame | undefined> {
    const row = await this.db
      .prepare(
        `SELECT id, name, title, yaml, revision, status, published_revision, created_at, updated_at
         FROM games WHERE id = ? AND owner_id = ?`,
      )
      .bind(id, ownerId)
      .first<GameRow>();
    return row === null ? undefined : mapGame(row);
  }

  async list(ownerId: string): Promise<readonly StoredGame[]> {
    const result = await this.db
      .prepare(
        `SELECT id, name, title, yaml, revision, status, published_revision, created_at, updated_at
         FROM games WHERE owner_id = ? ORDER BY updated_at DESC`,
      )
      .bind(ownerId)
      .all<GameRow>();
    return result.results.map(mapGame);
  }

  async update(
    ownerId: string,
    id: string,
    expectedRevision: number,
    input: { readonly name: string; readonly title: string; readonly yaml: string },
  ): Promise<StoreResult<StoredGame>> {
    const revision = expectedRevision + 1;
    const writeToken = crypto.randomUUID();
    const now = new Date().toISOString();
    const [updated] = await this.db.batch([
      this.db
        .prepare(
          `UPDATE games SET name = ?, title = ?, yaml = ?, revision = ?, status = 'draft', write_token = ?, updated_at = ?
           WHERE id = ? AND owner_id = ? AND revision = ?`,
        )
        .bind(input.name, input.title, input.yaml, revision, writeToken, now, id, ownerId, expectedRevision),
      this.db
        .prepare(
          `INSERT INTO game_revisions (game_id, revision, yaml, created_at)
           SELECT id, ?, ?, ? FROM games
           WHERE id = ? AND owner_id = ? AND revision = ? AND write_token = ?`,
        )
        .bind(revision, input.yaml, now, id, ownerId, revision, writeToken),
    ]);
    if (updated.meta.changes === 0) return this.missingOrConflict(ownerId, id, expectedRevision);
    return { ok: true, value: (await this.get(ownerId, id))! };
  }

  async publish(ownerId: string, id: string, expectedRevision: number): Promise<StoreResult<StoredGame>> {
    const writeToken = crypto.randomUUID();
    const now = new Date().toISOString();
    const [updated] = await this.db.batch([
      this.db
        .prepare(
          `UPDATE games SET status = 'published', published_revision = revision, write_token = ?, updated_at = ?
           WHERE id = ? AND owner_id = ? AND revision = ?`,
        )
        .bind(writeToken, now, id, ownerId, expectedRevision),
      this.db
        .prepare(
          `UPDATE game_revisions SET published_at = ?
           WHERE game_id = ? AND revision = ?
             AND EXISTS (
               SELECT 1 FROM games
               WHERE id = ? AND owner_id = ? AND revision = ? AND write_token = ?
             )`,
        )
        .bind(now, id, expectedRevision, id, ownerId, expectedRevision, writeToken),
    ]);
    if (updated.meta.changes === 0) return this.missingOrConflict(ownerId, id, expectedRevision);
    return { ok: true, value: (await this.get(ownerId, id))! };
  }

  async getPublished(id: string, revision: number): Promise<PublishedGame | undefined> {
    const row = await this.db
      .prepare(
        `SELECT game_id AS id, revision, yaml FROM game_revisions
         WHERE game_id = ? AND revision = ? AND published_at IS NOT NULL`,
      )
      .bind(id, revision)
      .first<PublishedGame>();
    return row === null ? undefined : row;
  }

  private async missingOrConflict(
    ownerId: string,
    id: string,
    expectedRevision: number,
  ): Promise<StoreResult<never>> {
    const current = await this.get(ownerId, id);
    return current === undefined
      ? { ok: false, code: "NOT_FOUND", message: `Game '${id}' was not found.` }
      : {
          ok: false,
          code: "REVISION_CONFLICT",
          message: `Expected revision ${expectedRevision}, but '${current.title}' is at revision ${current.revision}.`,
        };
  }
}

function mapGame(row: GameRow): StoredGame {
  return {
    id: row.id,
    name: row.name,
    title: row.title,
    yaml: row.yaml,
    revision: row.revision,
    status: row.status,
    publishedRevision: row.published_revision,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}
