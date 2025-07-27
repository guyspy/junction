package org.junction.catenin.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * JavaScript actual implementation of GameWorld
 */
actual class GameWorld internal constructor(
    private val impl: GameWorldImpl
) {
    
    /**
     * Apply an update to the world, returning the new world state
     */
    actual suspend fun applyUpdate(update: WorldUpdate): GameWorld {
        val newImpl = impl.applyUpdateInternal(update)
        return GameWorld(newImpl)
    }
    
    /**
     * Wrapper function to expose applyUpdate as a Promise to JavaScript
     */
    fun applyUpdateAsPromise(update: WorldUpdate): Promise<GameWorld> = GlobalScope.promise {
        applyUpdate(update)
    }
    
    companion object {
        /**
         * Create an empty game world
         */
        fun empty(): GameWorld = GameWorld(GameWorldImpl.empty())
    }
}