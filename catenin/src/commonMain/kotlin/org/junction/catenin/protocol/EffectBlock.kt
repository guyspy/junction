package org.junction.catenin.protocol

import kotlinx.serialization.Serializable
import org.junction.catenin.core.WorldUpdate
import kotlin.js.JsExport

/**
 * Type of causal block (analogous to Hearthstone's BLOCK_START type).
 */
@Serializable
@JsExport
enum class BlockType {
    ACTION,
    EFFECT,
    TRIGGER,
    CASCADE
}

/**
 * A causal block wrapping WorldUpdate events into a tree structure
 * (analogous to Hearthstone's BLOCK_START/BLOCK_END).
 *
 * Each block represents a cause-and-effect chain: an action triggers effects,
 * which may trigger further effects (children), forming a tree.
 */
@JsExport
data class EffectBlock(
    val type: BlockType,
    val sourceId: String,
    val updates: List<WorldUpdate>,
    val children: List<EffectBlock>,
    val animationHints: List<AnimationHint>
) {
    /**
     * Flatten the block tree into an ordered list of all updates (depth-first).
     * Own updates come first, then children's updates in order.
     */
    fun flattenUpdates(): List<WorldUpdate> {
        val result = mutableListOf<WorldUpdate>()
        result.addAll(updates)
        for (child in children) {
            result.addAll(child.flattenUpdates())
        }
        return result
    }
}
