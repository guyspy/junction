import Phaser from 'phaser'
import Card from './Card';

export default class Player {
    private scene: Phaser.Scene;
    private hand: Phaser.GameObjects.Container;
    private score: number;

    // Variables to hold the calculated layout values
    private leftEdgeX!: number;
    private bottomEdgeY!: number;
    private handAvailableWidth!: number;
    private handMarginX!: number;
    private handMarginY!: number;

    constructor(scene: Phaser.Scene) {
        this.scene = scene;
        this.hand = new Phaser.GameObjects.Container(scene, scene.cameras.main.centerX, 0);
        scene.add.existing(this.hand);
        this.score = 0;

        // Calculate the initial layout
        this.calculateHandLayout();

        // Recalculate the layout whenever the scene is resized
        scene.scale.on('resize', this.calculateHandLayout, this);
    }

    private calculateHandLayout() {
        // TODO: read from game config
        const minXMarginPercentage = 0.05; // 5% minimum margin width
        const maxXMarginPercentage = 0.10; // 10% maximum margin width

        const minYMarginPercentage = 0.05; // 5% minimum margin height
        const maxYMarginPercentage = 0.10; // 10% maximum margin height

        // Calculate the actual margin based on the screen width, making sure it falls within min and max
        this.handMarginX = Math.max(Math.min(this.scene.scale.width * maxXMarginPercentage, this.scene.scale.width * minXMarginPercentage), maxXMarginPercentage * this.scene.scale.width);
        this.handMarginY = Math.max(Math.min(this.scene.scale.height * maxYMarginPercentage, this.scene.scale.height * minYMarginPercentage), maxYMarginPercentage * this.scene.scale.height);

        // Calculate the width available for the cards:
        this.handAvailableWidth = this.scene.scale.width - 2 * this.handMarginX;

        // Calculate the left edge
        this.leftEdgeX = -(this.scene.scale.width / 2);
        this.bottomEdgeY = this.scene.scale.height - this.handMarginY;
    }

    receiveCard(card: Card) {
        // Add the card to the hand first
        this.hand.add(card);
    }

    renderHandCards() {
        // Then position all the cards
        (this.hand.list as Card[]).forEach((card, index) => {
            // Calculate the x and y positions of each card:
            const newX = this.leftEdgeX + (this.handAvailableWidth / this.hand.list.length) * (index + 0.5) + this.handMarginX;
            const newY = this.bottomEdgeY;

            // Add a tween that animates the card's position to the new values over 500 milliseconds
            // Adding an index-based delay causes each animation to start slightly after the previous one
            this.scene.tweens.add({
                targets: card,
                x: newX,
                y: newY,
                duration: 500,
                ease: 'Power2',
                delay: index * 20, // delay each card's animation by 200ms * its index in the list
                onComplete: () => {
                    card.flip();
                }
            });
        });
    }

}