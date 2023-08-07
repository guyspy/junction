export default class Card extends Phaser.GameObjects.Sprite {
    private flipped = false;
    private bWidth = 0;
    private scalePercentOfCamera = 0.2;
    
    constructor({
        scene,
        x,
        y,
        front,
        back,
        meta
    }: { scene: Phaser.Scene, x: number, y: number, front: string, back: string, meta: any }) {
        super(scene, x, y, back);

        // Adding the card to the scene.
        this.scene.add.existing(this);

        // Store the front and back texture keys.
        this.setData('front', front);
        this.setData('back', back);
        this.setData('meta', meta);

        // Set the display origin to the center of the card
        this.setOrigin(0.5, 0.5);

        // Determine the desired width and height as a percentage of the screen size
        this.bWidth = scene.cameras.main.width * this.scalePercentOfCamera; // 20% of screen width

        // Calculate the scale factors
        const scaleX = this.bWidth / this.width;

        // Set the scale
        this.setScale(scaleX);
    }

    flip() {
        this.flipped = !this.flipped;
        this.setTexture(this.flipped ? this.getData('front') : this.getData('back'));
    }

    move(x: number, y: number) {
        this.x = x;
        this.y = y;
    }
}