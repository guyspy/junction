import Phaser from 'phaser'
import LoadingScene from './scenes/LoadingScene'
import MainScene from './scenes/MainScene'

const config: Phaser.Types.Core.GameConfig = {
	type: Phaser.AUTO,
	width: window.innerWidth,
	height: window.innerHeight,
	scale: {
		mode: Phaser.Scale.RESIZE,
		autoCenter: Phaser.Scale.CENTER_BOTH
	},
	scene: [LoadingScene, MainScene],
}

export default new Phaser.Game(config)
