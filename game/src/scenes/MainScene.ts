import Phaser from 'phaser'
import { gameService } from '../service/gameService';

export default class MainScene extends Phaser.Scene {
    constructor() {
        super({ key: 'MainScene' })
    }

    preload() {
        // preload assets here
    }

    create() {
        const centerX = this.cameras.main.centerX;
        const centerY = this.cameras.main.centerY;
        const { prepareGame } = gameService(this);
        console.log('MainScene', centerX, centerY)


        // Add 'background' image at the center
        const background = this.add.image(centerX, centerY, 'background');
        prepareGame()

        // create game elements here
    }

    update() {
        // update game elements here
    }
}
