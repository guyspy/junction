package org.junction.catenin.core.initialization

import kotlin.js.JsExport

/**
 * Information about the setup process
 */
@JsExport
data class SetupInfo(
    val participantCount: Int,
    val availableObjectTypes: List<String>,
    val availableInstances: List<String>,
    val hasParticipantType: Boolean,
    val hasGameController: Boolean,
    val hasBoard: Boolean
)