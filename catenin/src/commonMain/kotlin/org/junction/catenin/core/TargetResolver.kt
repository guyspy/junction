package org.junction.catenin.core

import org.junction.catenin.model.GameObject
import org.junction.catenin.model.TargetDefinition
import org.junction.catenin.model.PropertyValue
import kotlin.js.JsExport

/**
 * Resolves target definitions to actual game objects
 */
@JsExport
class TargetResolver {
    
    /**
     * Resolve a target definition to a list of matching objects
     */
    fun resolveTargets(
        target: TargetDefinition,
        gameWorld: GameWorld,
        actingParticipantId: String
    ): List<GameObject> {
        return when {
            // Direct ID targeting
            target.id != null -> {
                if (target.id == "this") {
                    // "this" requires context from trigger firing - will be handled by caller
                    emptyList()
                } else {
                    listOfNotNull(gameWorld.objects[target.id])
                }
            }
            
            // Type-based targeting with relations
            target.type != null -> {
                val candidateObjects = gameWorld.getObjectsByType(target.type)
                
                when (target.relation) {
                    "self" -> {
                        // Find objects belonging to the acting participant
                        candidateObjects.filter { obj ->
                            obj.properties["participant_id"] == PropertyValue.IntValue(actingParticipantId.toInt())
                        }
                    }
                    
                    "opponent" -> {
                        // Find objects belonging to other participants
                        candidateObjects.filter { obj ->
                            obj.properties["participant_id"] != PropertyValue.IntValue(actingParticipantId.toInt())
                        }
                    }
                    
                    "all_opponents" -> {
                        // Same as opponent for now - could be extended for multiplayer
                        candidateObjects.filter { obj ->
                            obj.properties["participant_id"] != PropertyValue.IntValue(actingParticipantId.toInt())
                        }
                    }
                    
                    else -> candidateObjects
                }.let { filteredObjects ->
                    // Apply property matching if specified
                    if (target.property_match != null) {
                        filteredObjects.filter { obj ->
                            target.property_match.all { (key, value) ->
                                obj.properties[key] == value
                            }
                        }
                    } else {
                        filteredObjects
                    }
                }
            }
            
            else -> emptyList()
        }
    }
    
    /**
     * Resolve target with context object (for "this" references)
     */
    fun resolveTargetsWithContext(
        target: TargetDefinition,
        gameWorld: GameWorld,
        actingParticipantId: String,
        contextObject: GameObject
    ): List<GameObject> {
        return when {
            target.id == "this" -> listOf(contextObject)
            
            else -> resolveTargets(target, gameWorld, actingParticipantId)
        }
    }
    
    /**
     * Find objects matching a container by name (for parent matching)
     */
    fun findContainerByName(name: String, gameWorld: GameWorld): GameObject? {
        return gameWorld.objects.values.find { obj ->
            obj.type == "container" && obj.properties["name"] == PropertyValue.StringValue(name)
        }
    }
    
    /**
     * Get participant objects by participant ID
     */
    fun getParticipantObjects(participantId: String, gameWorld: GameWorld): List<GameObject> {
        return gameWorld.objects.values.filter { obj ->
            obj.properties["participant_id"] == PropertyValue.IntValue(participantId.toInt())
        }
    }
    
    /**
     * Get opponent participant IDs
     */
    fun getOpponentParticipantIds(actingParticipantId: String, gameWorld: GameWorld): List<String> {
        val allParticipantIds = gameWorld.objects.values
            .mapNotNull { it.properties["participant_id"] }
            .filterIsInstance<PropertyValue.IntValue>()
            .map { it.value.toString() }
            .toSet()
        
        return allParticipantIds.filter { it != actingParticipantId }
    }
}