package org.junction.catenin.core

/**
 * JVM actual implementation of GameWorld
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
    
    companion object {
        /**
         * Create an empty game world
         */
        fun empty(): GameWorld = GameWorld(GameWorldImpl.empty())
    }
}