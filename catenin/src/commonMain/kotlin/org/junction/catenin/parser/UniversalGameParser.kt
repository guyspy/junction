package org.junction.catenin.parser

import org.junction.catenin.model.UniversalGameDefinition
import kotlin.js.JsExport

/**
 * Parser for the universal object-based game definitions
 */
@JsExport
class UniversalGameParser {
    
    private val yamlParser = YamlParser()
    
    /**
     * Parse YAML string into UniversalGameDefinition
     */
    fun parseFromString(yamlContent: String): UniversalGameDefinition {
        return yamlParser.parseFromString(yamlContent)
    }
    
    /**
     * Validate that a game definition is well-formed
     */
    fun validate(definition: UniversalGameDefinition): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate instance references
        definition.instances.forEach { (instanceName, instanceDef) ->
            if (instanceDef.template !in definition.objectTypes) {
                errors.add("Instance '$instanceName' references unknown object type '${instanceDef.template}'")
            }
        }
        
        // Validate trigger references
        definition.triggers.forEach { trigger ->
            trigger.condition.objectType?.let { objectType ->
                if (objectType !in definition.objectTypes) {
                    errors.add("Trigger '${trigger.name}' references unknown object type '$objectType'")
                }
            }
        }
        
        // Validate effect target types
        definition.triggers.forEach { trigger ->
            trigger.effects.forEach { effect ->
                effect.modifyProperty?.target?.type?.let { targetType ->
                    if (targetType !in definition.objectTypes) {
                        errors.add("Effect in trigger '${trigger.name}' references unknown object type '$targetType'")
                    }
                }
                effect.changeParent?.target?.type?.let { targetType ->
                    if (targetType !in definition.objectTypes) {
                        errors.add("Effect in trigger '${trigger.name}' references unknown object type '$targetType'")
                    }
                }
                effect.createObject?.template?.let { template ->
                    if (template !in definition.objectTypes) {
                        errors.add("Effect in trigger '${trigger.name}' references unknown object type '$template'")
                    }
                }
            }
        }
        
        return errors
    }
}