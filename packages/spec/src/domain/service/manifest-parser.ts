import { parseAllDocuments } from "yaml";
import {
  DiagnosticCodes,
  hasErrors,
  suggestFrom,
  validateError,
  type Diagnostic,
} from "../../kernel/diagnostic.js";
import { err, ok, type Result } from "../../kernel/result.js";
import { collectPaths } from "../model/expression.js";
import {
  API_VERSION,
  SUPPORTED_KINDS,
  envelopeSchema,
  gameSpecSchema,
  type GameDocument,
  type GameSpec,
  type ZoneSel,
} from "../model/gamespec.js";
import { parseExpression } from "./expression-parser.js";

export interface ParseOptions {
  /** Used as the diagnostic path prefix, e.g. "games/war.yaml". */
  readonly file?: string;
}

/**
 * Parse + validate a (multi-doc) YAML string into a GameDocument.
 * Every failure mode returns structured diagnostics — never throws.
 */
export function parseGameDocument(
  yamlText: string,
  options: ParseOptions = {},
): Result<GameDocument> {
  const file = options.file ?? "<input>";
  const diagnostics: Diagnostic[] = [];

  const docs = parseAllDocuments(yamlText);
  for (const doc of docs) {
    for (const yamlErr of doc.errors) {
      const [line, col] = yamlErr.linePos?.[0] !== undefined
        ? [yamlErr.linePos[0].line, yamlErr.linePos[0].col]
        : [0, 0];
      diagnostics.push(
        validateError({
          code: DiagnosticCodes.INVALID_YAML,
          path: `${file}:${line}:${col}`,
          expected: yamlErr.message.split("\n")[0],
        }),
      );
    }
  }
  if (hasErrors(diagnostics)) return err(diagnostics);

  const games: GameDocument[] = [];
  docs.forEach((doc, index) => {
    const raw: unknown = doc.toJS();
    if (raw === null || raw === undefined) return;
    const docPath = `${file}#/doc/${index}`;

    const envelope = envelopeSchema.safeParse(raw);
    if (!envelope.success) {
      for (const issue of envelope.error.issues) {
        diagnostics.push(
          validateError({
            code: DiagnosticCodes.INVALID_MANIFEST_ENVELOPE,
            path: `${docPath}/${issue.path.join("/")}`,
            expected: issue.message,
          }),
        );
      }
      return;
    }

    const { apiVersion, kind, metadata, spec } = envelope.data;
    if (apiVersion !== API_VERSION) {
      diagnostics.push(
        validateError({
          code: DiagnosticCodes.UNSUPPORTED_API_VERSION,
          path: `${docPath}/apiVersion`,
          value: apiVersion,
          expected: `'${API_VERSION}'`,
          suggestion: API_VERSION,
        }),
      );
      return;
    }
    if (!(SUPPORTED_KINDS as readonly string[]).includes(kind)) {
      diagnostics.push(
        validateError({
          code: DiagnosticCodes.UNSUPPORTED_KIND,
          path: `${docPath}/kind`,
          value: kind,
          expected: "a supported kind",
          candidates: SUPPORTED_KINDS,
          suggestion: suggestFrom(kind, SUPPORTED_KINDS),
        }),
      );
      return;
    }

    const parsedSpec = gameSpecSchema.safeParse(spec);
    if (!parsedSpec.success) {
      for (const issue of parsedSpec.error.issues) {
        diagnostics.push(
          validateError({
            code: DiagnosticCodes.SCHEMA_VALIDATION_FAILED,
            path: `${file}#/spec/${issue.path.join("/")}`,
            expected: issue.message,
          }),
        );
      }
      return;
    }

    games.push({
      apiVersion: API_VERSION,
      kind: "Game",
      metadata: { name: metadata.name },
      spec: parsedSpec.data,
    });
  });

  if (hasErrors(diagnostics)) return err(diagnostics);
  if (games.length !== 1) {
    return err([
      validateError({
        code: DiagnosticCodes.INVALID_MANIFEST_ENVELOPE,
        path: file,
        value: games.length,
        expected: "exactly one Game document per file (v1alpha)",
      }),
    ]);
  }

  const game = games[0]!;
  diagnostics.push(...lintGameSpec(game.spec, file));
  if (hasErrors(diagnostics)) return err(diagnostics);
  return ok(game, diagnostics);
}

// ---- semantic lints ---------------------------------------------------------

function lintGameSpec(spec: GameSpec, file: string): Diagnostic[] {
  const out: Diagnostic[] = [];
  const at = (ptr: string) => `${file}#/spec/${ptr}`;

  const zoneNames = spec.zones.map((z) => z.name);
  const pieceNames = spec.pieces.map((p) => p.name);
  const actionNames = spec.actions.map((a) => a.name);
  const zoneByName = new Map(spec.zones.map((z) => [z.name, z]));

  // Duplicate names per namespace.
  for (const [ns, names] of [
    ["zones", zoneNames],
    ["pieces", pieceNames],
    ["actions", actionNames],
    ["triggers", spec.triggers.map((t) => t.name)],
  ] as const) {
    const seen = new Set<string>();
    names.forEach((n, i) => {
      if (seen.has(n))
        out.push(
          validateError({
            code: DiagnosticCodes.DUPLICATE_NAME,
            path: at(`${ns}/${i}/name`),
            value: n,
            expected: `a unique name within '${ns}'`,
          }),
        );
      seen.add(n);
    });
  }

  const zoneRef = (zone: string, ptr: string): void => {
    if (!zoneByName.has(zone))
      out.push(
        validateError({
          code: DiagnosticCodes.ZONE_REF_UNKNOWN,
          path: at(ptr),
          value: zone,
          expected: "a declared zone",
          candidates: zoneNames,
          suggestion: suggestFrom(zone, zoneNames),
        }),
      );
  };
  const seatZoneRef = (zone: string, ptr: string): void => {
    zoneRef(zone, ptr);
    const decl = zoneByName.get(zone);
    if (decl !== undefined && decl.owner !== "seat")
      out.push(
        validateError({
          code: DiagnosticCodes.SCHEMA_VALIDATION_FAILED,
          path: at(ptr),
          value: zone,
          expected: "a zone declared with owner: seat",
        }),
      );
  };
  const zoneSelRef = (sel: ZoneSel, ptr: string): void => {
    zoneRef(sel.zone, `${ptr}/zone`);
    const decl = zoneByName.get(sel.zone);
    if (decl !== undefined && sel.owner === "actor" && decl.owner !== "seat")
      out.push(
        validateError({
          code: DiagnosticCodes.SCHEMA_VALIDATION_FAILED,
          path: at(`${ptr}/owner`),
          value: sel.owner,
          expected: "owner is only valid when the zone is declared owner: seat",
        }),
      );
  };

  // Setup references.
  spec.setup.forEach((op, i) => {
    if (op.op === "create") {
      if (!pieceNames.includes(op.pieces))
        out.push(
          validateError({
            code: DiagnosticCodes.PIECE_REF_UNKNOWN,
            path: at(`setup/${i}/pieces`),
            value: op.pieces,
            expected: "a declared piece set",
            candidates: pieceNames,
            suggestion: suggestFrom(op.pieces, pieceNames),
          }),
        );
      zoneRef(op.into, `setup/${i}/into`);
    } else if (op.op === "shuffle") {
      zoneRef(op.zone, `setup/${i}/zone`);
    } else {
      zoneRef(op.from, `setup/${i}/from`);
      zoneRef(op.to, `setup/${i}/to`);
    }
  });

  // Actions.
  spec.actions.forEach((action, i) => {
    if (action.move !== undefined) {
      zoneSelRef(action.move.from, `actions/${i}/move/from`);
      zoneSelRef(action.move.to, `actions/${i}/move/to`);
    }
    if (action.flip !== undefined) zoneSelRef(action.flip.zone, `actions/${i}/flip/zone`);
    if (action.requires !== undefined)
      lintExpression(action.requires, at(`actions/${i}/requires`), spec, out);
  });

  // Turn phases reference actions.
  spec.turn.phases.forEach((phase, i) => {
    phase.actions.forEach((a, j) => {
      if (!actionNames.includes(a))
        out.push(
          validateError({
            code: DiagnosticCodes.ACTION_REF_UNKNOWN,
            path: at(`turn/phases/${i}/actions/${j}`),
            value: a,
            expected: "a declared action",
            candidates: actionNames,
            suggestion: suggestFrom(a, actionNames),
          }),
        );
    });
  });

  // Triggers.
  const allPieceProps = new Set(spec.pieces.flatMap((p) => Object.keys(p.properties)));
  spec.triggers.forEach((trigger, i) => {
    if (trigger.on.intoZone !== undefined) {
      zoneRef(trigger.on.intoZone, `triggers/${i}/on/intoZone`);
      if (trigger.on.event !== "pieceMoved")
        out.push(
          validateError({
            code: DiagnosticCodes.SCHEMA_VALIDATION_FAILED,
            path: at(`triggers/${i}/on/intoZone`),
            value: trigger.on.event,
            expected: "intoZone is a pieceMoved filter; use inZone for pieceFlipped",
          }),
        );
    }
    if (trigger.on.inZone !== undefined) {
      zoneRef(trigger.on.inZone, `triggers/${i}/on/inZone`);
      if (trigger.on.event !== "pieceFlipped")
        out.push(
          validateError({
            code: DiagnosticCodes.SCHEMA_VALIDATION_FAILED,
            path: at(`triggers/${i}/on/inZone`),
            value: trigger.on.event,
            expected: "inZone is a pieceFlipped filter; use intoZone for pieceMoved",
          }),
        );
    }
    if (trigger.when !== undefined) lintExpression(trigger.when, at(`triggers/${i}/when`), spec, out);
    trigger.effects.forEach((effect, j) => {
      const propLint = (property: string, ptr: string): void => {
        if (!allPieceProps.has(property))
          out.push(
            validateError({
              code: DiagnosticCodes.SCHEMA_VALIDATION_FAILED,
              path: at(ptr),
              value: property,
              expected: "a property declared on some piece set",
              candidates: [...allPieceProps],
              suggestion: suggestFrom(property, [...allPieceProps]),
            }),
          );
      };
      if ("moveAll" in effect) {
        zoneSelRef(effect.moveAll.from, `triggers/${i}/effects/${j}/moveAll/from`);
        zoneSelRef(effect.moveAll.to, `triggers/${i}/effects/${j}/moveAll/to`);
      } else if ("resolveHighest" in effect) {
        zoneRef(effect.resolveHighest.zone, `triggers/${i}/effects/${j}/resolveHighest/zone`);
        seatZoneRef(effect.resolveHighest.toWinnerZone, `triggers/${i}/effects/${j}/resolveHighest/toWinnerZone`);
        propLint(effect.resolveHighest.property, `triggers/${i}/effects/${j}/resolveHighest/property`);
      } else {
        const resolve = effect.resolveEqualPair;
        zoneRef(resolve.zone, `triggers/${i}/effects/${j}/resolveEqualPair/zone`);
        const zoneDecl = zoneByName.get(resolve.zone);
        if (zoneDecl !== undefined && zoneDecl.owner !== "shared")
          out.push(
            validateError({
              code: DiagnosticCodes.SCHEMA_VALIDATION_FAILED,
              path: at(`triggers/${i}/effects/${j}/resolveEqualPair/zone`),
              value: resolve.zone,
              expected: "a shared zone (the match grid is common to all seats)",
            }),
          );
        seatZoneRef(resolve.toZone, `triggers/${i}/effects/${j}/resolveEqualPair/toZone`);
        propLint(resolve.property, `triggers/${i}/effects/${j}/resolveEqualPair/property`);
      }
    });
  });

  // End.
  lintExpression(spec.end.when, at("end/when"), spec, out);
  seatZoneRef(spec.end.winner.mostPiecesIn, "end/winner/mostPiecesIn");

  // Meta sanity.
  if (spec.meta.seats.min > spec.meta.seats.max)
    out.push(
      validateError({
        code: DiagnosticCodes.SCHEMA_VALIDATION_FAILED,
        path: at("meta/seats"),
        value: spec.meta.seats,
        expected: "min <= max",
      }),
    );

  return out;
}

/**
 * Expression context roots (the complete v1alpha vocabulary):
 *   zones.<zone>.count        (shared zones only)
 *   zones.<zone>.faceUpCount  (shared zones only)
 *   zones.<zone>.totalCount   (any zone; owner zones sum across seats)
 *   zones.<zone>.allEmpty | anyEmpty
 *   seats.count
 *   turn.round | turn.seatIndex
 */
function lintExpression(src: string, path: string, spec: GameSpec, out: Diagnostic[]): void {
  const parsed = parseExpression(src);
  if (!parsed.ok) {
    out.push(
      validateError({
        code: DiagnosticCodes.EXPRESSION_SYNTAX_ERROR,
        path,
        value: src,
        expected: `${parsed.reason} (at offset ${parsed.pos})`,
      }),
    );
    return;
  }
  const zoneByName = new Map(spec.zones.map((z) => [z.name, z]));
  const zoneNames = spec.zones.map((z) => z.name);
  for (const segs of collectPaths(parsed.expr)) {
    const joined = segs.join(".");
    const invalid = (expected: string, candidates?: readonly string[]): void => {
      out.push(
        validateError({
          code: DiagnosticCodes.EXPRESSION_REF_INVALID,
          path,
          value: joined,
          expected,
          candidates,
          suggestion: candidates ? suggestFrom(segs[1] ?? joined, candidates) : undefined,
        }),
      );
    };
    if (segs[0] === "zones") {
      const zone = zoneByName.get(segs[1] ?? "");
      if (zone === undefined) {
        invalid("zones.<declared-zone>.<field>", zoneNames);
        continue;
      }
      const field = segs[2];
      const fields = ["count", "faceUpCount", "totalCount", "allEmpty", "anyEmpty"];
      if (segs.length !== 3 || !fields.includes(field ?? "")) {
        invalid(`one of zones.${zone.name}.{${fields.join("|")}}`);
        continue;
      }
      if ((field === "count" || field === "faceUpCount") && zone.owner !== "shared")
        invalid(`'${field}' is only valid on shared zones; use zones.${zone.name}.totalCount/allEmpty/anyEmpty for owner zones`);
    } else if (segs[0] === "seats") {
      if (segs.length !== 2 || segs[1] !== "count") invalid("seats.count");
    } else if (segs[0] === "turn") {
      if (segs.length !== 2 || !["round", "seatIndex"].includes(segs[1] ?? ""))
        invalid("turn.round or turn.seatIndex");
    } else {
      invalid("a path rooted at zones.*, seats.*, or turn.*", ["zones", "seats", "turn"]);
    }
  }
}
