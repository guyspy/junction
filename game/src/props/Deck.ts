import Card from "./Card";

export default class Deck extends Phaser.GameObjects.Container {

    constructor(scene: Phaser.Scene, cards: Card[]) {
        super(scene, 0, 0);

        // Adding the deck to the scene.
        this.scene.add.existing(this);

        // Add the provided cards to the container
        cards.forEach(card => this.add(card));
    }

    draw(): Card | null {
        const lastChild = this.getAt(this.length - 1);
        if (lastChild instanceof Card) {
            this.remove(lastChild);
            return lastChild;
        }
        return null;
    }

    count() {
        return this.list.length;
    }


}