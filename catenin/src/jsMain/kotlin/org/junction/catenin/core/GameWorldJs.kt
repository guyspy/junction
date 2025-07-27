package org.junction.catenin.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.Promise

/**
 * JavaScript-specific wrapper for GameWorld to provide exports
 */
@JsExport
class GameWorldJs internal constructor(
    private val gameWorld: GameWorld
) {
    /**
     * Apply an update and return a Promise
     */
    @JsName("applyUpdate")
    fun applyUpdate(update: WorldUpdate): Promise<GameWorldJs> = GlobalScope.promise {
        val newWorld = gameWorld.applyUpdate(update)
        GameWorldJs(newWorld)
    }
    
    companion object {
        /**
         * Create an empty game world
         */
        @JsName("create")
        fun create(): GameWorldJs = GameWorldJs(GameWorld.empty())
    }
}

/**
 * Factory function for JavaScript
 */
@JsExport
@JsName("createGameWorld")
fun createGameWorld(): GameWorldJs = GameWorldJs.create()