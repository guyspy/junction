package org.junction.catenin.core

/**
 * Game world that can only be modified through events
 */
expect class GameWorld {
    /**
     * Apply an update to the world, returning the new world state
     */
    suspend fun applyUpdate(update: WorldUpdate): GameWorld
}

/**
 * Sealed class for world update operations
 */
sealed class WorldUpdate