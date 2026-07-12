import { Application, Assets, Sprite, Text, Texture } from "pixi.js";
import { cardBackSVG, cardFaceSVG } from "./art.js";
import type { VisualAdapter } from "./dom-renderer.js";
import type { CardVM, ViewModel, ZoneVM } from "./view-model.js";

const CARD_W = 64;
const CARD_H = 90;
const GAP = 10;

/** Default visual layer: the DOM remains the accessible controller and fallback. */
export function createPixiVisualAdapter(): VisualAdapter {
  const app = new Application();
  let host: HTMLElement | undefined;
  let ready: Promise<void> | undefined;
  let generation = 0;
  let disposed = false;

  return {
    mount(container) {
      host = document.createElement("div");
      host.className = "jx-pixi-stage";
      host.setAttribute("aria-hidden", "true");
      container.append(host);
      ready = app
        .init({ resizeTo: host, preference: "webgl", antialias: true, backgroundAlpha: 0 })
        .then(() => host?.append(app.canvas));
    },
    render(view, move) {
      const current = ++generation;
      void draw(view, move, current);
    },
    dispose() {
      disposed = true;
      generation++;
      app.destroy(true, { children: true });
      host?.remove();
    },
  };

  async function draw(
    view: ViewModel,
    move: (move: { action: string; target?: string }) => void,
    current: number,
  ): Promise<void> {
    await ready;
    if (disposed || current !== generation) return;
    for (const child of app.stage.removeChildren()) child.destroy({ children: true });

    const width = app.renderer.width;
    let y = 12;
    for (const zone of view.zones) {
      app.stage.addChild(
        new Text({
          text: zone.title.toUpperCase(),
          style: { fill: 0xf4f7f4, fontFamily: "system-ui", fontSize: 13, fontWeight: "700" },
          x: 12,
          y,
        }),
      );
      y += 24;
      const cards = cardsToDraw(zone);
      let x = 12;
      let rowHeight = CARD_H;
      for (const card of cards) {
        if (x + CARD_W > width - 12) {
          x = 12;
          y += CARD_H + GAP;
          rowHeight += CARD_H + GAP;
        }
        const sprite = await cardSprite(card);
        if (disposed || current !== generation) return;
        sprite.position.set(x, y);
        if (card.move !== undefined) {
          sprite.eventMode = "static";
          sprite.cursor = "pointer";
          sprite.on("pointertap", () => move(card.move!));
        }
        app.stage.addChild(sprite);
        x += zone.kind === "stack" || zone.kind === "pile" ? 12 : CARD_W + GAP;
      }
      y += rowHeight + 20;
    }
  }
}

function cardsToDraw(zone: ZoneVM): readonly CardVM[] {
  if (zone.kind === "stack") return zone.cards.slice(0, 1);
  if (zone.kind === "pile") return zone.cards.slice(0, 8);
  return zone.cards;
}

async function cardSprite(card: CardVM): Promise<Sprite> {
  const svg = card.faceUp && card.properties !== undefined ? cardFaceSVG(card.properties) : cardBackSVG("junction-pixi");
  const texture = await Assets.load<Texture>(`data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`);
  const sprite = new Sprite(texture);
  sprite.width = CARD_W;
  sprite.height = CARD_H;
  sprite.alpha = card.move === undefined ? 0.82 : 1;
  return sprite;
}
