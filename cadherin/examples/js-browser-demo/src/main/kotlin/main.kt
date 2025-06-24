import kotlinx.browser.document
import kotlinx.browser.window
import org.junction.cadherin.core.GameEngine
import org.junction.cadherin.model.PlayerAction

fun main() {
    window.onload = {
        setupUI()
    }
}

fun setupUI() {
    val container = document.getElementById("game-container")
    if (container != null) {
        container.innerHTML = """
            <h1>Cadherin Browser Demo</h1>
            <div id="game-area">
                <p>Loading game...</p>
            </div>
            <div id="controls">
                <button id="start-game">Start Game</button>
            </div>
        """.trimIndent()
        
        document.getElementById("start-game")?.addEventListener("click", {
            startGame()
        })
    }
}

fun startGame() {
    val gameArea = document.getElementById("game-area")
    gameArea?.innerHTML = """
        <h2>Game Started!</h2>
        <p>This is a demonstration of the Cadherin game engine running in the browser.</p>
        <p>The game engine is loaded and ready to process YAML game definitions.</p>
    """.trimIndent()
}