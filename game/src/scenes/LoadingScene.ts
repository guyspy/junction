import Phaser from 'phaser'
import EventBus from '@vertx/eventbus-bridge-client.js'
import { gameService } from '../service/gameService';

export default class LoadingScene extends Phaser.Scene {
    private progressBar!: Phaser.GameObjects.Graphics;
    private progressBox!: Phaser.GameObjects.Graphics;
    private assetsLoaded: boolean = false;
    private connected: boolean = false;

    constructor() {
        super({ key: 'LoadingScene' })
    }

    preload() {
        this.createProgressBar();
        this.load.image('background', 'src/assets/background.png')
        const { preload } = gameService(this);
        preload()
        this.load.on('progress', this.updateProgressBar, this);
        this.load.on('complete', () => {
            this.assetsLoaded = true;
            console.log('Assets loaded')
            this.checkStartCondition();
        });
    }

    create() {
        // Create EventBus
        const eventBus = new EventBus('http://localhost:8080/eventbus');
        eventBus.onopen = () => {
            console.log('Connected to Vert.x Event Bus');
            this.connected = true;
            this.checkStartCondition();
        }
    }

    createProgressBar() {
        this.progressBox = this.add.graphics();
        this.progressBar = this.add.graphics();

        this.progressBox.fillStyle(0x222222, 0.8);
        this.progressBox.fillRect(240, 270, 320, 50);
    }

    updateProgressBar(value: number) {
        this.progressBar.clear();
        this.progressBar.fillStyle(0xffffff, 1);
        this.progressBar.fillRect(250, 280, 300 * value, 30);
    }

    checkStartCondition() {
        if (this.assetsLoaded && this.connected) {
            this.scene.start('MainScene');
        }
    }
}
