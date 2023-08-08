import { cards, game } from '../game';
import { Player } from '../props';
import { cardService } from './cardService';

export const gameService = (scene: Phaser.Scene) => {

    const { dealSpecificCardsToEachPlayer, shuffleAndDealEvenlyToAllPayers } = cardService(scene);

    const preload = () => {
        cards.forEach(card => {
            scene.load.image(card.back.name, card.back.path);
            scene.load.image(card.front.name, card.front.path);
        });
    }

    const prepareGame = () => {
        const player = new Player(scene);
        game.prepare.forEach((step) => {
            if (step.action === 'dealCards') {
                if (step.mode === 'dealSpecificCardsToEachPlayer') {
                    dealSpecificCardsToEachPlayer(step.cards, [player]);
                } else if (step.mode === 'shuffleAndDealEvenlyToAllPayers') {
                    shuffleAndDealEvenlyToAllPayers(step.cards, [player]);
                }
            } else if (step.action === 'render') {
                if (step.mode === 'renderMyHandCards') {
                    player.renderHandCards();
                }
            }
        });
    }

    return { preload, prepareGame }
}

