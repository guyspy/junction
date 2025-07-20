package org.junction.catenin.model.validation

import org.junction.catenin.model.definitions.UniversalGameDefinition
import org.junction.catenin.model.definitions.ObjectTypeDefinition
import org.junction.catenin.model.definitions.PropertyDefinition
import org.junction.catenin.model.definitions.PropertyType
import org.junction.catenin.model.definitions.GameMeta
import org.junction.catenin.model.values.PropertyValue
import org.junction.catenin.model.values.IntValue
import org.junction.catenin.model.values.StringValue
import org.junction.catenin.model.values.BoolValue
import org.junction.catenin.model.values.ObjectRefValue
import org.junction.catenin.model.objects.ObjectInstance
import org.junction.catenin.model.triggers.TriggerDefinition
import org.junction.catenin.model.triggers.EffectDefinition
import org.junction.catenin.model.triggers.LogEffect
import org.junction.catenin.model.triggers.ModifyPropertyEffect
import kotlin.js.JsExport

/**
 * Validates universal game definitions for correctness and consistency
 */
@JsExport
class SchemaValidator {
    
    /**
     * Validate a complete game definition
     */
    fun validate(definition: UniversalGameDefinition): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Validate meta section
        errors.addAll(validateMeta(definition.meta))
        
        // Validate object types
        errors.addAll(validateObjectTypes(definition.objectTypes))
        
        // Validate instances reference valid object types
        errors.addAll(validateInstances(definition.instances, definition.objectTypes))
        
        // Validate triggers reference valid properties and object types
        errors.addAll(validateTriggers(definition.triggers, definition.objectTypes))
        
        return ValidationResult(errors)
    }
    
    private fun validateMeta(meta: GameMeta): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        // Validate name
        if (meta.name.isBlank()) {
            errors.add(ValidationError("Game name cannot be empty", "meta.name"))
        }
        
        // Validate target age
        if (meta.targetAge.size != 2) {
            errors.add(ValidationError("Target age must be an array of exactly 2 integers [min, max]", "meta.target_age"))
        } else {
            val (min, max) = meta.targetAge
            if (min < 0 || max < 0) {
                errors.add(ValidationError("Target age values must be non-negative", "meta.target_age"))
            }
            if (min > max) {
                errors.add(ValidationError("Target age minimum ($min) cannot be greater than maximum ($max)", "meta.target_age"))
            }
        }
        
        // Validate participant count
        if (meta.participantCount.size != 2) {
            errors.add(ValidationError("Participant count must be an array of exactly 2 integers [min, max]", "meta.participant_count"))
        } else {
            val (min, max) = meta.participantCount
            if (min < 1 || max < 1) {
                errors.add(ValidationError("Participant count values must be positive", "meta.participant_count"))
            }
            if (min > max) {
                errors.add(ValidationError("Participant count minimum ($min) cannot be greater than maximum ($max)", "meta.participant_count"))
            }
        }
        
        return errors
    }
    
    private fun validateObjectTypes(objectTypes: Map<String, ObjectTypeDefinition>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        if (objectTypes.isEmpty()) {
            errors.add(ValidationError("Game must define at least one object type", "object_types"))
            return errors
        }
        
        for ((typeName, definition) in objectTypes) {
            // Validate object type name
            if (typeName.isBlank()) {
                errors.add(ValidationError("Object type name cannot be empty", "object_types"))
                continue
            }
            
            // Validate that object type has some properties or states
            if (definition.properties.isEmpty() && definition.states.isEmpty()) {
                errors.add(ValidationError("Object type '$typeName' must define at least one property or state", "object_types.$typeName"))
            }
            
            // Validate each property definition
            for ((propName, propDef) in definition.properties) {
                errors.addAll(validatePropertyDefinition(propName, propDef, "object_types.$typeName.properties"))
            }
            
            // Validate each state definition
            for ((stateName, stateDef) in definition.states) {
                errors.addAll(validatePropertyDefinition(stateName, stateDef, "object_types.$typeName.states"))
            }
        }
        
        return errors
    }
    
    private fun validatePropertyDefinition(name: String, definition: PropertyDefinition, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        if (name.isBlank()) {
            errors.add(ValidationError("Property/state name cannot be empty", "$path.$name"))
            return errors
        }
        
        // PropertyDefinition's constructor already validates type compatibility,
        // so we don't need to duplicate that validation here
        
        return errors
    }
    
    private fun validateInstances(instances: Map<String, ObjectInstance>, objectTypes: Map<String, ObjectTypeDefinition>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        for ((instanceName, instance) in instances) {
            if (instanceName.isBlank()) {
                errors.add(ValidationError("Instance name cannot be empty", "instances"))
                continue
            }
            
            // Validate template reference
            if (!objectTypes.containsKey(instance.objectType)) {
                errors.add(ValidationError("Instance '$instanceName' references unknown object type '${instance.objectType}'", "instances.$instanceName.objectType"))
                continue
            }
            
            val objectType = objectTypes[instance.objectType]
                ?: continue // Skip validation if object type not found (error already added above)
            
            // Validate property overrides
            for ((propName, propValue) in instance.properties) {
                if (!objectType.hasProperty(propName)) {
                    errors.add(ValidationError("Instance '$instanceName' overrides unknown property '$propName'", "instances.$instanceName.properties.$propName"))
                } else {
                    // Try to parse the value according to the property type
                    val propDef = objectType.getPropertyDefinition(propName)
                        ?: continue // Skip if property definition not found (shouldn't happen after hasProperty check)
                    val validationResult = validatePropertyValue(propValue, propDef)
                    if (!validationResult.isValid) {
                        errors.add(ValidationError("Instance '$instanceName' has invalid value for property '$propName': ${validationResult.issues.first().message}", "instances.$instanceName.properties.$propName"))
                    }
                }
            }
            
            // Validate state overrides
            for ((stateName, stateValue) in instance.states) {
                if (!objectType.hasState(stateName)) {
                    errors.add(ValidationError("Instance '$instanceName' overrides unknown state '$stateName'", "instances.$instanceName.states.$stateName"))
                } else {
                    // Try to parse the value according to the state type
                    val stateDef = objectType.getStateDefinition(stateName)
                        ?: continue // Skip if state definition not found (shouldn't happen after hasState check)
                    val validationResult = validatePropertyValue(stateValue, stateDef)
                    if (!validationResult.isValid) {
                        errors.add(ValidationError("Instance '$instanceName' has invalid value for state '$stateName': ${validationResult.issues.first().message}", "instances.$instanceName.states.$stateName"))
                    }
                }
            }
        }
        
        return errors
    }
    
    private fun validateTriggers(triggers: List<TriggerDefinition>, objectTypes: Map<String, ObjectTypeDefinition>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        for ((index, trigger) in triggers.withIndex()) {
            val path = "triggers[$index]"
            
            // Validate object type reference in trigger condition
            trigger.`when`.objectType?.let { objType ->
                if (!objectTypes.containsKey(objType)) {
                    errors.add(ValidationError("Trigger references unknown object type '$objType'", "$path.when.object_type"))
                }
            }
            
            // Validate property reference in trigger condition
            if (trigger.`when`.propertyChanged != null && trigger.`when`.objectType != null) {
                val objectType = objectTypes[trigger.`when`.objectType]
                if (objectType != null) {
                    val propName = trigger.`when`.propertyChanged
                    if (!objectType.hasProperty(propName) && !objectType.hasState(propName)) {
                        errors.add(ValidationError("Trigger references unknown property/state '$propName' on object type '${trigger.`when`.objectType}'", "$path.when.property_changed"))
                    }
                }
            }
            
            // Validate effects
            if (trigger.effects.isEmpty()) {
                errors.add(ValidationError("Trigger must define at least one effect", "$path.effects"))
            }
            
            for ((effectIndex, effect) in trigger.effects.withIndex()) {
                errors.addAll(validateEffect(effect, "$path.effects[$effectIndex]", objectTypes))
            }
        }
        
        return errors
    }
    
    private fun validateEffect(effect: EffectDefinition, path: String, objectTypes: Map<String, ObjectTypeDefinition>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        when (effect) {
            is LogEffect -> {
                if (effect.message.isBlank()) {
                    errors.add(ValidationError("Log effect message cannot be empty", "$path.message"))
                }
            }
            is ModifyPropertyEffect -> {
                if (effect.property.isBlank()) {
                    errors.add(ValidationError("Modify property effect must specify a property name", "$path.property"))
                }
                if (effect.delta == null && effect.value == null) {
                    errors.add(ValidationError("Modify property effect must specify either 'delta' or 'value'", path))
                }
                if (effect.delta != null && effect.value != null) {
                    errors.add(ValidationError("Modify property effect cannot specify both 'delta' and 'value'", path))
                }
                // Note: We can't validate target references without more context about runtime objects
            }
        }
        
        return errors
    }
    
    private fun validatePropertyValue(value: String, definition: PropertyDefinition): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        try {
            val propertyValue = when (definition.type) {
                PropertyType.INT -> {
                    val intValue = value.toInt()
                    IntValue(intValue)
                }
                PropertyType.STRING -> StringValue(value)
                PropertyType.BOOL -> {
                    val boolValue = when (value.lowercase()) {
                        "true" -> true
                        "false" -> false
                        else -> throw IllegalArgumentException("Invalid boolean value: $value")
                    }
                    BoolValue(boolValue)
                }
                PropertyType.OBJECT_REF -> ObjectRefValue(value)
            }
            
            // Validate constraints
            if (!definition.isValid(propertyValue)) {
                errors.add(ValidationError("Value '$value' does not meet constraints for ${definition.type} property"))
            }
            
        } catch (e: Exception) {
            errors.add(ValidationError("Cannot parse '$value' as ${definition.type}: ${e.message}"))
        }
        
        return ValidationResult(errors)
    }
}