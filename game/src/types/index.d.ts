declare module '@vertx/eventbus-bridge-client.js';

interface CardSetting {
    back: ImageAssets
    front: ImageAssets
    meta: CardMeta
}

interface ImageAssets {
    name: string
    path: string
}

interface CardMeta {
    type: 'gene'
    subtype: string
    name: string
}


interface InventoryItem {
    name: string;
    count: number;
}

type DealCardsAction = {
    action: 'dealCards';
    mode: 'dealSpecificCardsToEachPlayer' | 'shuffleAndDealEvenlyToAllPayers';
    cards: InventoryItem[];
};

type RenderAction = {
    action: 'render';
    mode: 'renderMyHandCards';
}

type Action = DealCardsAction | RenderAction;

interface GameDesign {
    players: number;
    prepare: Action[];
}
