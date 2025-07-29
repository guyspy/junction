package org.junction.catenin

import org.junction.catenin.engine.InitializationConfig
import org.junction.catenin.engine.SingletonObjectConfig
import org.junction.catenin.model.definitions.*
import org.junction.catenin.model.objects.ObjectInstance
import org.junction.catenin.model.triggers.*
import org.junction.catenin.model.values.*
import org.junction.catenin.schema.UniversalGameSchema

/**
 * Creates a sample battle game schema demonstrating all engine features
 */
fun createSampleGameSchema(): UniversalGameSchema {
    // Create game metadata
    val meta = GameMeta(
        name = "Battle Arena",
        targetAge = intArrayOf(10, 99),
        participantCount = intArrayOf(2, 4)
    )
    
    // Define object types
    val objectTypes = mutableMapOf<String, ObjectTypeDefinition>()
    
    // Participant type
    objectTypes["participant"] = ObjectTypeDefinition(
        properties = mapOf(
            "wins" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(0)
            )
        ),
        states = mapOf(
            "turn_count" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(0)
            ),
            "active" to PropertyDefinition(
                type = PropertyType.BOOL,
                initial = BoolValue(true)
            )
        )
    )
    
    // Unit type
    objectTypes["unit"] = ObjectTypeDefinition(
        properties = mapOf(
            "health" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(10),
                min = IntValue(0),
                max = IntValue(20)
            ),
            "max_health" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(10)
            ),
            "attack" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(3),
                min = IntValue(1),
                max = IntValue(10)
            ),
            "armor" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(0),
                min = IntValue(0),
                max = IntValue(5)
            ),
            "owner" to PropertyDefinition(
                type = PropertyType.OBJECT_REF,
                initial = ObjectRefValue("")
            ),
            "unit_type" to PropertyDefinition(
                type = PropertyType.STRING,
                initial = StringValue("Soldier")
            )
        ),
        states = mapOf(
            "tapped" to PropertyDefinition(
                type = PropertyType.BOOL,
                initial = BoolValue(false)
            ),
            "summoning_sickness" to PropertyDefinition(
                type = PropertyType.BOOL,
                initial = BoolValue(true)
            )
        )
    )
    
    // Spell type
    objectTypes["spell"] = ObjectTypeDefinition(
        properties = mapOf(
            "damage" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(0)
            ),
            "healing" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(0)
            ),
            "target_type" to PropertyDefinition(
                type = PropertyType.STRING,
                initial = StringValue("unit")
            ),
            "spell_name" to PropertyDefinition(
                type = PropertyType.STRING,
                initial = StringValue("Unknown Spell")
            )
        )
    )
    
    // Game state type (singleton for tracking global game state)
    objectTypes["game_state"] = ObjectTypeDefinition(
        properties = mapOf(
            "turn_number" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(1)
            ),
            "phase" to PropertyDefinition(
                type = PropertyType.STRING,
                initial = StringValue("playing")
            ),
            "current_player_index" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(0)
            ),
            "total_players" to PropertyDefinition(
                type = PropertyType.INT,
                initial = IntValue(0)
            )
        )
    )
    
    // Define triggers
    val triggers = mutableListOf<TriggerDefinition>()
    
    // Trigger: Low health grants armor
    triggers.add(TriggerDefinition(
        name = "low_health_armor",
        `when` = TriggerCondition(
            objectType = "unit",
            propertyChanged = "health"
        ),
        effects = listOf(
            ModifyPropertyEffect(
                target = "self",
                property = "armor",
                delta = "2"
            ),
            LogEffect("Unit gained +2 armor due to low health!")
        )
    ))
    
    // Trigger: Log all property changes
    triggers.add(TriggerDefinition(
        name = "property_logger",
        `when` = TriggerCondition(),
        effects = listOf(
            LogEffect("Property {property} changed to {value} on {object}")
        )
    ))
    
    // Define instances
    val instances = mutableMapOf<String, ObjectInstance>()
    
    instances["fireball"] = ObjectInstance(
        objectType = "spell",
        properties = mapOf(
            "damage" to "8",
            "spell_name" to "Fireball"
        )
    )
    
    instances["healing_potion"] = ObjectInstance(
        objectType = "spell",
        properties = mapOf(
            "healing" to "5",
            "spell_name" to "Healing Potion"
        )
    )
    
    instances["champion"] = ObjectInstance(
        objectType = "unit",
        properties = mapOf(
            "health" to "15",
            "max_health" to "15",
            "attack" to "5",
            "armor" to "2",
            "unit_type" to "Champion"
        )
    )
    
    // Define initialization configuration for a turn-based battle game
    val initializationConfig = InitializationConfig(
        participantType = "participant",
        participantIdProperty = "player_id",
        singletonObjects = listOf(
            SingletonObjectConfig(
                objectType = "game_state",
                id = "game_state",
                propertyOverrides = mapOf(
                    "turn_number" to "1",
                    "phase" to "setup"
                )
            )
        ),
        autoCreateInstances = listOf("fireball", "healing_potion"),  // Pre-create some spell instances
        createAllInstances = false
    )
    
    return UniversalGameSchema(
        meta = meta,
        objectTypes = objectTypes,
        instances = instances,
        triggers = triggers,
        initialization = initializationConfig
    )
}