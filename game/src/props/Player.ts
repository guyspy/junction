import Phaser from 'phaser'
import Card from './Card';

export default class Player {
    private scene: Phaser.Scene;
    private hand: Phaser.GameObjects.Container;
    private score: number;

    // Variables to hold the calculated layout values
    private handLeftEdgeX!: number;
    private handAvailableWidth!: number;
    private handMargin!: number;

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
        const minMarginPercentage = 0.05; // 5% minimum margin width
        const maxMarginPercentage = 0.10; // 10% maximum margin width

        // Calculate the actual margin based on the screen width, making sure it falls within min and max
        this.handMargin = Math.max(Math.min(this.scene.scale.width * maxMarginPercentage, this.scene.scale.width * minMarginPercentage), maxMarginPercentage * this.scene.scale.width);

        // Calculate the width available for the cards:
        this.handAvailableWidth = this.scene.scale.width - 2 * this.handMargin;

        // Calculate the left edge
        this.handLeftEdgeX = -(this.scene.scale.width / 2);
    }

    receiveCard(card: Card) {
        // Add the card to the hand first
        this.hand.add(card);

        // Then position all the cards
        (this.hand.list as Card[]).forEach((card, index) => {
            // Calculate the x position of each card:
            // - (this.handAvailableWidth / this.hand.list.length) calculates the space each card should take up.
            // - (index + 0.5) positions each card at its correct position within that space.
            // - Finally, add the this.handMargin to shift the x position from the left side of the screen.
            card.x = this.handLeftEdgeX + (this.handAvailableWidth / this.hand.list.length) * (index + 0.5) + this.handMargin;
            card.flip()
        });
    }

}