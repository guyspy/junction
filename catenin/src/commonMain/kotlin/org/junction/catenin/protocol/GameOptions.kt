package org.junction.catenin.protocol

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * A single available action that a participant can take.
 * The server tells the client exactly what is valid — the client has zero game logic.
 */
@Serializable
@JsExport
data class AvailableAction(
    val type: String,
    val sourceId: String,
    val targets: List<String>,
    val metadata: Map<String, String>
)

/**
 * The set of actions available to a specific participant (analogous to Hearthstone's Options packet).
 * Sent by the server to tell the client what moves are valid.
 */
@Serializable
@JsExport
data class GameOptions(
    val participantId: String,
    val actions: List<AvailableAction>
)
