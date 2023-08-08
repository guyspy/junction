import { cards } from '../game';
import { Player, Card } from '../props';

export const cardService = (scene: Phaser.Scene) => {

    const findCardByName = (name: string): CardSetting | undefined => {
        return cards.find(card => card.meta.name === name);
    };

    const createSpecificCard = (scene: Phaser.Scene, cardSetting: CardSetting): Card => {
        return new Card({ scene, x: 0, y: scene.cameras.main.centerY, front: cardSetting.front.name, back: cardSetting.back.name, meta: cardSetting.meta });
    };

    const dealCardsToPlayer = (player: Player, cardNamesToDeal: string[]): void => {
        cardNamesToDeal.forEach((cardName) => {
            const cardSetting = findCardByName(cardName);
            if (!cardSetting) {
                throw new Error(`Card with name ${cardName} not found`);
            }
            const c = createSpecificCard(scene, cardSetting);
            player.receiveCard(c);
        });
    };


    const flattenToCardNames = (cardsToDeal: InventoryItem[]): string[] => {
        return cardsToDeal
            .map(item => Array(item.count).fill(item.name))
            .flat();
    }


    const dealSpecificCardsToEachPlayer = (cardsToDeal: InventoryItem[], players: Player[]) => {
        players.forEach((player) => {
            const cardNames = flattenToCardNames(cardsToDeal)
            dealCardsToPlayer(player, cardNames);
        });
    }

    const shuffleAndDealEvenlyToAllPayers = (cardsToDeal: InventoryItem[], players: Player[]) => {
        const cardNames = flattenToCardNames(cardsToDeal)
        // shuffle cardNames
        const shuffledCardNames = cardNames.sort(() => Math.random() - 0.5);
        // deal cards evenly to players
        for (let i = 0; i < shuffledCardNames.length; i++) {
            const playerIndex = i % players.length;
            const cardName = shuffledCardNames[i];
            const p = players[playerIndex]
            dealCardsToPlayer(p, [cardName]);
        }
    }

    return { dealSpecificCardsToEachPlayer, shuffleAndDealEvenlyToAllPayers }
}