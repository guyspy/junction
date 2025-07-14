package org.junction.catenin.core

import org.junction.catenin.model.*
import kotlin.js.JsExport

/**
 * Engine for executing effects and applying changes to the game world
 */
@JsExport
class EffectEngine(
    private val targetResolver: TargetResolver,
    private val objectFactory: ObjectFactory
) {
    
    /**
     * Execute an effect and return the updated game world
     */
    fun executeEffect(
        effect: EffectDefinition,
        gameWorld: GameWorld,
        actingParticipantId: String,
        contextObject: GameObject? = null
    ): GameWorld {
        var updatedWorld = gameWorld
        
        // Handle different effect types
        effect.log?.let { logMessage ->
            // Log effects - in practice would add to game log
            // For now, just continue with no changes
        }
        
        effect.modifyProperty?.let { modifyEffect ->
            updatedWorld = executeModifyPropertyEffect(modifyEffect, updatedWorld, actingParticipantId, contextObject)
        }
        
        effect.changeParent?.let { changeParentEffect ->
            updatedWorld = executeChangeParentEffect(changeParentEffect, updatedWorld, actingParticipantId, contextObject)
        }
        
        effect.createObject?.let { createObjectEffect ->
            updatedWorld = executeCreateObjectEffect(createObjectEffect, updatedWorld, actingParticipantId, contextObject)
        }
        
        effect.destroyObject?.let { destroyObjectEffect ->
            updatedWorld = executeDestroyObjectEffect(destroyObjectEffect, updatedWorld, actingParticipantId, contextObject)
        }
        
        return updatedWorld
    }
    
    /**
     * Execute a modify property effect
     */
    private fun executeModifyPropertyEffect(
        effect: ModifyPropertyEffect,
        gameWorld: GameWorld,
        actingParticipantId: String,
        contextObject: GameObject?
    ): GameWorld {
        val targets = if (contextObject != null) {
            targetResolver.resolveTargetsWithContext(effect.target, gameWorld, actingParticipantId, contextObject)
        } else {
            targetResolver.resolveTargets(effect.target, gameWorld, actingParticipantId)
        }
        
        var updatedWorld = gameWorld
        
        targets.forEach { target ->
            val currentValue = target.properties[effect.property]
            
            val newValue = when {
                effect.value != null -> effect.value
                effect.delta != null -> {
                    calculateDeltaValue(currentValue, effect.delta)
                }
                else -> currentValue
            }
            
            if (newValue != null) {
                val updatedProperties = target.properties.toMutableMap()
                updatedProperties[effect.property] = newValue
                updatedWorld = updatedWorld.updateObjectProperties(target.id, updatedProperties)
            }
        }
        
        return updatedWorld
    }
    
    /**
     * Execute a change parent effect
     */
    private fun executeChangeParentEffect(
        effect: ChangeParentEffect,
        gameWorld: GameWorld,
        actingParticipantId: String,
        contextObject: GameObject?
    ): GameWorld {
        val targets = if (contextObject != null) {
            targetResolver.resolveTargetsWithContext(effect.target, gameWorld, actingParticipantId, contextObject)
        } else {
            targetResolver.resolveTargets(effect.target, gameWorld, actingParticipantId)
        }
        
        var updatedWorld = gameWorld
        
        targets.forEach { target ->
            val newParentId = when {
                effect.new_parent == null -> null
                else -> {
                    val newParents = if (contextObject != null) {
                        targetResolver.resolveTargetsWithContext(effect.new_parent, updatedWorld, actingParticipantId, contextObject)
                    } else {
                        targetResolver.resolveTargets(effect.new_parent, updatedWorld, actingParticipantId)
                    }
                    newParents.firstOrNull()?.id
                }
            }
            
            updatedWorld = updatedWorld.changeObjectParent(target.id, newParentId)
        }
        
        return updatedWorld
    }
    
    /**
     * Execute a create object effect
     */
    private fun executeCreateObjectEffect(
        effect: CreateObjectEffect,
        gameWorld: GameWorld,
        actingParticipantId: String,
        contextObject: GameObject?
    ): GameWorld {
        val objectId = effect.id ?: run {
            val result = gameWorld.generateObjectId(effect.template)
            return executeCreateObjectEffect(
                effect.copy(id = result.id),
                result.updatedWorld,
                actingParticipantId,
                contextObject
            )
        }
        
        val newObject = objectFactory.createObject(effect.template, effect.properties)
            .copy(id = objectId)
        
        var updatedWorld = gameWorld.withObject(newObject)
        
        // Set parent if specified
        effect.parent?.let { parentTarget ->
            val parents = if (contextObject != null) {
                targetResolver.resolveTargetsWithContext(parentTarget, updatedWorld, actingParticipantId, contextObject)
            } else {
                targetResolver.resolveTargets(parentTarget, updatedWorld, actingParticipantId)
            }
            
            parents.firstOrNull()?.let { parent ->
                updatedWorld = updatedWorld.changeObjectParent(objectId, parent.id)
            }
        }
        
        return updatedWorld
    }
    
    /**
     * Execute a destroy object effect
     */
    private fun executeDestroyObjectEffect(
        effect: DestroyObjectEffect,
        gameWorld: GameWorld,
        actingParticipantId: String,
        contextObject: GameObject?
    ): GameWorld {
        val targets = if (contextObject != null) {
            targetResolver.resolveTargetsWithContext(effect.target, gameWorld, actingParticipantId, contextObject)
        } else {
            targetResolver.resolveTargets(effect.target, gameWorld, actingParticipantId)
        }
        
        var updatedWorld = gameWorld
        
        targets.forEach { target ->
            updatedWorld = updatedWorld.withoutObject(target.id)
        }
        
        return updatedWorld
    }
    
    /**
     * Calculate new value based on delta expression
     */
    private fun calculateDeltaValue(
        currentValue: PropertyValue?,
        delta: String
    ): PropertyValue? {
        return when (currentValue) {
            is PropertyValue.IntValue -> {
                val deltaAmount = when {
                    delta.startsWith("+") -> delta.substring(1).toIntOrNull() ?: 0
                    delta.startsWith("-") -> -(delta.substring(1).toIntOrNull() ?: 0)
                    else -> delta.toIntOrNull() ?: 0
                }
                PropertyValue.IntValue(currentValue.value + deltaAmount)
            }
            
            is PropertyValue.StringValue -> {
                // For strings, delta could be concatenation
                if (delta.startsWith("+")) {
                    PropertyValue.StringValue(currentValue.value + delta.substring(1))
                } else {
                    PropertyValue.StringValue(delta)
                }
            }
            
            is PropertyValue.BoolValue -> {
                // For booleans, delta could toggle
                when (delta.lowercase()) {
                    "toggle" -> PropertyValue.BoolValue(!currentValue.value)
                    "true" -> PropertyValue.BoolValue(true)
                    "false" -> PropertyValue.BoolValue(false)
                    else -> currentValue
                }
            }
            
            else -> currentValue
        }
    }
    
    /**
     * Execute multiple effects in sequence
     */
    fun executeEffects(
        effects: List<EffectDefinition>,
        gameWorld: GameWorld,
        actingParticipantId: String,
        contextObject: GameObject? = null
    ): GameWorld {
        return effects.fold(gameWorld) { world, effect ->
            executeEffect(effect, world, actingParticipantId, contextObject)
        }
    }
}