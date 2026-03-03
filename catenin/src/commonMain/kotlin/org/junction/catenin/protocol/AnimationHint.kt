package org.junction.catenin.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Non-state-changing hints for the renderer (analogous to Hearthstone's META_DATA).
 * These describe visual/audio effects that should accompany state changes.
 */
@Serializable
@JsExport
sealed class AnimationHint {

    @Serializable
    @SerialName("Projectile")
    data class Projectile(val from: String, val to: String, val effect: String) : AnimationHint()

    @Serializable
    @SerialName("FloatingText")
    data class FloatingText(val target: String, val text: String, val style: String) : AnimationHint()

    @Serializable
    @SerialName("Shake")
    data class Shake(val target: String, val intensity: Float) : AnimationHint()

    @Serializable
    @SerialName("Highlight")
    data class Highlight(val targets: List<String>, val color: String) : AnimationHint()

    @Serializable
    @SerialName("Delay")
    data class Delay(val ms: Int) : AnimationHint()

    @Serializable
    @SerialName("PlaySound")
    data class PlaySound(val soundId: String) : AnimationHint()
}
