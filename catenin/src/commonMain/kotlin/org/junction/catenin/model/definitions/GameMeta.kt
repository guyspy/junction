package org.junction.catenin.model.definitions

import kotlin.js.JsExport

/**
 * Game metadata and configuration
 */
@JsExport
data class GameMeta(
    val name: String,
    val targetAge: IntArray,
    val participantCount: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GameMeta

        if (name != other.name) return false
        if (!targetAge.contentEquals(other.targetAge)) return false
        if (!participantCount.contentEquals(other.participantCount)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + targetAge.contentHashCode()
        result = 31 * result + participantCount.contentHashCode()
        return result
    }
}