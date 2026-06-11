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

/** Which expression roots are legal at a given site. */
interface ExprContext {
  /** seat.* (the acting seat's variables) */
  readonly actor: boolean;
  /** this.* (the piece that fired the trigger) */
  readonly eventPiece: boolean;
  /** target.* (the chosen piece of the action) */
  readonly targetPiece: boolean;
}

const NO_CONTEXT: ExprContext = { actor: false, eventPiece: false, targetPiece: false };

function lintGameSpec(spec: GameSpec, file: string): Diagnostic[] {
  const out: Diagnostic[] = [];
  const at = (ptr: string) => `${file}#/spec/${ptr}`;

  const zoneNames = spec.zones.map((z) => z.name);
  const pieceNames = spec.pieces.map((p) => p.name);
  const actionNames = spec.actions.map((a) => a.name);
  const zoneByName = new Map(spec.zones.map((z) => [z.name, z]));
  const globalVars = Object.keys(spec.variables.global);
  const seatVars = Object.keys(spec.variables.perSeat);
  const intProps = new Set(
    spec.pieces.flatMap((p) => Object.entries(p.properties).filter(([, d]) => d.type === "int").map(([k]) => k)),
  );
  const allPieceProps = new Set(spec.pieces.flatMap((p) => Object.keys(p.properties)));
  const twoSeater = spec.meta.seats.min === 2 && spec.meta.seats.max === 2;

  const fail = (
    code: (typeof DiagnosticCodes)[keyof typeof DiagnosticCodes],
    ptr: string,
    value: unknown,
    expected: string,
    candidates?: readonly string[],
  ): void => {
    out.push(
      validateError({
        code,
        path: at(ptr),
        value,
        expected,
        candidates,
        suggestion:
          candidates !== undefined && typeof value === "string" ? suggestFrom(value, candidates) : undefined,
      }),
    );
  };

  // Duplicate names per namespace.
  for (const [ns, names] of [
    ["zones", zoneNames],
    ["pieces", pieceNames],
    ["actions", actionNames],
    ["triggers", spec.triggers.map((t) => t.name)],
  ] as const) {
    const seen = new Set<string>();
    names.forEach((n, i) => {
      if (seen.has(n)) fail(DiagnosticCodes.DUPLICATE_NAME, `${ns}/${i}/name`, n, `a unique name within '${ns}'`);
      seen.add(n);
    });
  }

  const zoneRef = (zone: string, ptr: string): void => {
    if (!zoneByName.has(zone))
      fail(DiagnosticCodes.ZONE_REF_UNKNOWN, ptr, zone, "a declared zone", zoneNames);
  };
  const seatZoneRef = (zone: string, ptr: string): void => {
    zoneRef(zone, ptr);
    const decl = zoneByName.get(zone);
    if (decl !== undefined && decl.owner !== "seat")
      fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, ptr, zone, "a zone declared with owner: seat");
  };
  const zoneSelRef = (sel: ZoneSel, ptr: string): void => {
    zoneRef(sel.zone, `${ptr}/zone`);
    const decl = zoneByName.get(sel.zone);
    if (decl !== undefined && sel.owner === "actor" && decl.owner !== "seat")
      fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, `${ptr}/owner`, sel.owner, "owner is only valid when the zone is declared owner: seat");
  };
  const seatVarRef = (name: string, ptr: string): void => {
    if (!seatVars.includes(name))
      fail(DiagnosticCodes.VAR_REF_UNKNOWN, ptr, name, "a declared perSeat variable", seatVars);
  };
  const scopedVarRef = (scope: "global" | "actor" | "opponent", name: string, ptr: string): void => {
    if (scope === "global") {
      if (!globalVars.includes(name))
        fail(DiagnosticCodes.VAR_REF_UNKNOWN, ptr, name, "a declared global variable", globalVars);
    } else {
      seatVarRef(name, ptr);
      if (scope === "opponent" && !twoSeater)
        fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, ptr, scope, "scope 'opponent' requires seats fixed at exactly 2");
    }
  };
  const intPropRef = (property: string, ptr: string): void => {
    if (!intProps.has(property))
      fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, ptr, property, "an int property declared on some piece set", [...intProps]);
  };

  const lintAmount = (value: number | string, ptr: string, ctx: ExprContext): void => {
    if (typeof value === "string") lintExpression(value, ptr, ctx);
  };

  // Setup references.
  spec.setup.forEach((op, i) => {
    if (op.op === "create") {
      if (!pieceNames.includes(op.pieces))
        fail(DiagnosticCodes.PIECE_REF_UNKNOWN, `setup/${i}/pieces`, op.pieces, "a declared piece set", pieceNames);
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
    const hasTarget = action.move?.take === "chosen" || action.flip !== undefined;
    const ctx: ExprContext = { actor: true, eventPiece: false, targetPiece: hasTarget };
    if (action.move !== undefined) {
      zoneSelRef(action.move.from, `actions/${i}/move/from`);
      zoneSelRef(action.move.to, `actions/${i}/move/to`);
    }
    if (action.flip !== undefined) zoneSelRef(action.flip.zone, `actions/${i}/flip/zone`);
    if (action.requires !== undefined) lintExpression(action.requires, `actions/${i}/requires`, ctx);
    if (action.cost !== undefined) {
      seatVarRef(action.cost.var, `actions/${i}/cost/var`);
      lintAmount(action.cost.amount, `actions/${i}/cost/amount`, ctx);
    }
  });

  // Turn phases reference actions.
  spec.turn.phases.forEach((phase, i) => {
    phase.actions.forEach((a, j) => {
      if (!actionNames.includes(a))
        fail(DiagnosticCodes.ACTION_REF_UNKNOWN, `turn/phases/${i}/actions/${j}`, a, "a declared action", actionNames);
    });
  });

  // Triggers.
  const PIECE_EVENTS = ["pieceMoved", "pieceFlipped", "propertyChanged"];
  spec.triggers.forEach((trigger, i) => {
    const on = trigger.on;
    const isPieceEvent = PIECE_EVENTS.includes(on.event);
    const ctx: ExprContext = { actor: true, eventPiece: isPieceEvent, targetPiece: false };

    if (on.intoZone !== undefined) {
      zoneRef(on.intoZone, `triggers/${i}/on/intoZone`);
      if (on.event !== "pieceMoved")
        fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, `triggers/${i}/on/intoZone`, on.event, "intoZone is a pieceMoved filter; use inZone for pieceFlipped");
    }
    if (on.inZone !== undefined) {
      zoneRef(on.inZone, `triggers/${i}/on/inZone`);
      if (on.event !== "pieceFlipped")
        fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, `triggers/${i}/on/inZone`, on.event, "inZone is a pieceFlipped filter; use intoZone for pieceMoved");
    }
    if (on.pieceSet !== undefined) {
      if (!pieceNames.includes(on.pieceSet))
        fail(DiagnosticCodes.PIECE_REF_UNKNOWN, `triggers/${i}/on/pieceSet`, on.pieceSet, "a declared piece set", pieceNames);
      if (!isPieceEvent)
        fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, `triggers/${i}/on/pieceSet`, on.event, "pieceSet filters piece events (pieceMoved/pieceFlipped/propertyChanged)");
    }
    if (on.property !== undefined) {
      if (on.event !== "propertyChanged")
        fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, `triggers/${i}/on/property`, on.event, "property is a propertyChanged filter");
      else intPropRef(on.property, `triggers/${i}/on/property`);
    }
    if (on.var !== undefined) {
      if (on.event !== "varChanged")
        fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, `triggers/${i}/on/var`, on.event, "var is a varChanged filter");
      else if (![...globalVars, ...seatVars].includes(on.var))
        fail(DiagnosticCodes.VAR_REF_UNKNOWN, `triggers/${i}/on/var`, on.var, "a declared variable", [...globalVars, ...seatVars]);
    }
    if (trigger.when !== undefined) lintExpression(trigger.when, `triggers/${i}/when`, ctx);

    trigger.effects.forEach((effect, j) => {
      const eptr = `triggers/${i}/effects/${j}`;
      if ("moveAll" in effect) {
        zoneSelRef(effect.moveAll.from, `${eptr}/moveAll/from`);
        zoneSelRef(effect.moveAll.to, `${eptr}/moveAll/to`);
      } else if ("modifyProperty" in effect) {
        intPropRef(effect.modifyProperty.property, `${eptr}/modifyProperty/property`);
        if (!isPieceEvent)
          fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, `${eptr}/modifyProperty/target`, on.event, "'this' exists only for piece events (pieceMoved/pieceFlipped/propertyChanged)");
        const amountValue = effect.modifyProperty.add ?? effect.modifyProperty.set!;
        lintAmount(amountValue, `${eptr}/modifyProperty`, ctx);
      } else if ("setVar" in effect) {
        scopedVarRef(effect.setVar.scope, effect.setVar.var, `${eptr}/setVar/var`);
        lintAmount(effect.setVar.value, `${eptr}/setVar/value`, ctx);
      } else if ("addVar" in effect) {
        scopedVarRef(effect.addVar.scope, effect.addVar.var, `${eptr}/addVar/var`);
        lintAmount(effect.addVar.amount, `${eptr}/addVar/amount`, ctx);
      } else if ("roll" in effect) {
        scopedVarRef(effect.roll.scope, effect.roll.var, `${eptr}/roll/var`);
      } else if ("resolveHighest" in effect) {
        zoneRef(effect.resolveHighest.zone, `${eptr}/resolveHighest/zone`);
        seatZoneRef(effect.resolveHighest.toWinnerZone, `${eptr}/resolveHighest/toWinnerZone`);
        intPropRef(effect.resolveHighest.property, `${eptr}/resolveHighest/property`);
      } else {
        const resolve = effect.resolveEqualPair;
        zoneRef(resolve.zone, `${eptr}/resolveEqualPair/zone`);
        const zoneDecl = zoneByName.get(resolve.zone);
        if (zoneDecl !== undefined && zoneDecl.owner !== "shared")
          fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, `${eptr}/resolveEqualPair/zone`, resolve.zone, "a shared zone (the match grid is common to all seats)");
        seatZoneRef(resolve.toZone, `${eptr}/resolveEqualPair/toZone`);
        if (!allPieceProps.has(resolve.property))
          fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, `${eptr}/resolveEqualPair/property`, resolve.property, "a property declared on some piece set", [...allPieceProps]);
      }
    });
  });

  // End.
  lintExpression(spec.end.when, "end/when", NO_CONTEXT);
  if ("mostPiecesIn" in spec.end.winner) seatZoneRef(spec.end.winner.mostPiecesIn, "end/winner/mostPiecesIn");
  else seatVarRef(spec.end.winner.highestSeatVar, "end/winner/highestSeatVar");

  // Meta sanity.
  if (spec.meta.seats.min > spec.meta.seats.max)
    fail(DiagnosticCodes.SCHEMA_VALIDATION_FAILED, "meta/seats", spec.meta.seats, "min <= max");

  return out;

  /**
   * Expression context roots (the complete Wave-1 vocabulary):
   *   zones.<zone>.count|faceUpCount   (shared zones only)
   *   zones.<zone>.totalCount|allEmpty|anyEmpty
   *   seats.count
   *   turn.round | turn.seatIndex
   *   vars.<global>                          — global variable
   *   seat.<perSeat>                         — the acting seat's variable (actor context)
   *   seatVars.<perSeat>.min|max|sum         — aggregates across seats (valid anywhere)
   *   this.<intProp>                         — the trigger's piece (piece-event context)
   *   target.<intProp>                       — the action's chosen piece (chosen-action context)
   */
  function lintExpression(src: string, ptr: string, ctx: ExprContext): void {
    const parsed = parseExpression(src);
    if (!parsed.ok) {
      fail(DiagnosticCodes.EXPRESSION_SYNTAX_ERROR, ptr, src, `${parsed.reason} (at offset ${parsed.pos})`);
      return;
    }
    for (const segs of collectPaths(parsed.expr)) {
      const joined = segs.join(".");
      const invalid = (expected: string, candidates?: readonly string[]): void => {
        out.push(
          validateError({
            code: DiagnosticCodes.EXPRESSION_REF_INVALID,
            path: at(ptr),
            value: joined,
            expected,
            candidates,
            suggestion: candidates !== undefined ? suggestFrom(segs[1] ?? joined, candidates) : undefined,
          }),
        );
      };
      const root = segs[0];
      if (root === "zones") {
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
      } else if (root === "seats") {
        if (segs.length !== 2 || segs[1] !== "count") invalid("seats.count");
      } else if (root === "turn") {
        if (segs.length !== 2 || !["round", "seatIndex"].includes(segs[1] ?? "")) invalid("turn.round or turn.seatIndex");
      } else if (root === "vars") {
        if (segs.length !== 2 || !globalVars.includes(segs[1] ?? "")) invalid("vars.<declared-global-variable>", globalVars);
      } else if (root === "seat") {
        if (!ctx.actor) invalid("seat.* needs an acting seat — not available in end conditions");
        else if (segs.length !== 2 || !seatVars.includes(segs[1] ?? "")) invalid("seat.<declared-perSeat-variable>", seatVars);
      } else if (root === "seatVars") {
        const aggregates = ["min", "max", "sum"];
        if (segs.length !== 3 || !seatVars.includes(segs[1] ?? "") || !aggregates.includes(segs[2] ?? ""))
          invalid("seatVars.<perSeat-variable>.{min|max|sum}", seatVars);
      } else if (root === "this") {
        if (!ctx.eventPiece) invalid("this.* is only available in piece-event triggers");
        else if (segs.length !== 2 || !intProps.has(segs[1] ?? "")) invalid("this.<int-property>", [...intProps]);
      } else if (root === "target") {
        if (!ctx.targetPiece) invalid("target.* is only available on chosen-target actions");
        else if (segs.length !== 2 || !intProps.has(segs[1] ?? "")) invalid("target.<int-property>", [...intProps]);
      } else {
        invalid("a path rooted at zones/seats/turn/vars/seat/seatVars/this/target", [
          "zones", "seats", "turn", "vars", "seat", "seatVars", "this", "target",
        ]);
      }
    }
  }
}
