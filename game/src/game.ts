export const game: GameDesign = {
    players: 2,
    prepare: [
        {
            action: 'dealCards',
            mode: 'dealSpecificCardsToEachPlayer',
            cards: [
                { name: '5-utr-3', count: 1 },
                { name: '5-utr-2', count: 1 },
                { name: '5-utr-1', count: 1 },
            ]
        },
        {
            action: 'dealCards',
            mode: 'shuffleAndDealEvenlyToAllPayers',
            cards: [
                { name: 'gene-mn', count: 4 },
                { name: 'gene-spike', count: 4 },
                { name: 'gene-polymerase', count: 4 },
            ]
        },
        {
            action: 'dealCards',
            mode: 'dealSpecificCardsToEachPlayer',
            cards: [
                { name: '3-utr-poly-a', count: 4 },
                { name: 'rnai', count: 1 },
                { name: 'recombination', count: 1 }
            ]
        },
    ]
}

export const cards: CardSetting[] = [
    {
        meta: { type: 'gene', subtype: '5-utr', name: '5-utr-3' },
        front: { name: 'pink_3', path: 'src/assets/cards/pink_3.png' },
        back: { name: 'pink_back', path: 'src/assets/cards/pink_back.png' },
    },
    {
        meta: { type: 'gene', subtype: '5-utr', name: '5-utr-2' },
        front: { name: 'pink_4', path: 'src/assets/cards/pink_4.png' },
        back: { name: 'pink_back', path: 'src/assets/cards/pink_back.png' },
    },
    {
        meta: { type: 'gene', subtype: '5-utr', name: '5-utr-1' },
        front: { name: 'pink_5', path: 'src/assets/cards/pink_5.png' },
        back: { name: 'pink_back', path: 'src/assets/cards/pink_back.png' },
    },
    {
        meta: { type: 'gene', subtype: 'structure', name: 'gene-mn' },
        front: { name: 'pink_6', path: 'src/assets/cards/pink_6.png' },
        back: { name: 'pink_back', path: 'src/assets/cards/pink_back.png' },
    },
    {
        meta: { type: 'gene', subtype: 'structure', name: 'gene-spike' },
        front: { name: 'pink_7', path: 'src/assets/cards/pink_7.png' },
        back: { name: 'pink_back', path: 'src/assets/cards/pink_back.png' },
    },
    {
        meta: { type: 'gene', subtype: 'structure', name: 'gene-polymerase' },
        front: { name: 'pink_8', path: 'src/assets/cards/pink_8.png' },
        back: { name: 'pink_back', path: 'src/assets/cards/pink_back.png' },
    },
    {
        meta: { type: 'gene', subtype: '3-utr', name: '3-utr-poly-a' },
        front: { name: 'pink_9', path: 'src/assets/cards/pink_9.png' },
        back: { name: 'pink_back', path: 'src/assets/cards/pink_back.png' },
    },
    {
        meta: { type: 'gene', subtype: 'event', name: 'rnai' },
        front: { name: 'pink_9', path: 'src/assets/cards/pink_1.png' },
        back: { name: 'pink_back', path: 'src/assets/cards/pink_back.png' },
    },
    {
        meta: { type: 'gene', subtype: 'event', name: 'recombination' },
        front: { name: 'pink_9', path: 'src/assets/cards/pink_2.png' },
        back: { name: 'pink_back', path: 'src/assets/cards/pink_back.png' },
    }
]
