package org.junction.catenin

import org.junction.catenin.schema.dsl.universalGameSchema

/**
 * Creates a sample battle game schema using the DSL and demonstrating
 * the new condition evaluator features
 */
fun createSampleGameSchema() = universalGameSchema {
    meta {
        name = "Battle Arena"
        targetAge = intArrayOf(10, 99)
        participantCount = intArrayOf(2, 4)
    }
    
    objectTypes {
        objectType("participant") {
            properties {
                int("wins", initial = 0)
            }
            states {
                int("turn_count", initial = 0)
                bool("active", initial = true)
            }
        }
        
        objectType("unit") {
            properties {
                int("health", initial = 10, min = 0, max = 20)
                int("max_health", initial = 10)
                int("attack", initial = 3, min = 1, max = 10)
                int("armor", initial = 0, min = 0, max = 5)
                objectRef("owner", initial = "")
                string("unit_type", initial = "Soldier")
            }
            states {
                bool("tapped", initial = false)
                bool("summoning_sickness", initial = true)
                bool("berserk", initial = false)
            }
        }
        
        objectType("spell") {
            properties {
                int("damage", initial = 0)
                int("healing", initial = 0)
                string("target_type", initial = "unit")
                string("spell_name", initial = "Unknown Spell")
            }
        }
        
        objectType("game_state") {
            properties {
                int("turn_number", initial = 1)
                string("phase", initial = "playing")
                int("current_player_index", initial = 0)
                int("total_players", initial = 0)
            }
        }
    }
    
    instances {
        instance("fireball", "spell") {
            properties {
                "damage" to 8
                "spell_name" to "Fireball"
            }
        }
        
        instance("healing_potion", "spell") {
            properties {
                "healing" to 5
                "spell_name" to "Healing Potion"
            }
        }
        
        instance("champion", "unit") {
            properties {
                "health" to 15
                "max_health" to 15
                "attack" to 5
                "armor" to 2
                "unit_type" to "Champion"
            }
        }
    }
    
    triggers {
        // Trigger: Critical armor boost when health is low and armor is weak
        trigger("critical_armor_boost") {
            `when` {
                objectType = "unit"
                propertyChanged = "health"
                condition = "source.health < 5 && source.armor < 3"
            }
            effects {
                modifyProperty(
                    target = "self",
                    property = "armor",
                    delta = "2"
                )
            }
        }
        
        // Trigger: Berserk mode activates on very low health or high attack
        trigger("berserk_activation") {
            `when` {
                objectType = "unit"
                propertyChanged = "health"
                condition = "source.health <= 3 || source.attack > 7"
            }
            effects {
                // Note: Would use setProperty here if it existed
                // For now, we'll let the berserk state be tracked externally
                modifyProperty(
                    target = "self",
                    property = "attack",
                    delta = "2"
                )
            }
        }
        
        // Trigger: Heal when armor changes and health is low
        trigger("armor_heal_synergy") {
            `when` {
                objectType = "unit"
                propertyChanged = "armor"
                condition = "source.health < source.max_health / 2 && source.armor > 0"
            }
            effects {
                modifyProperty(
                    target = "self",
                    property = "health",
                    delta = "1"
                )
            }
        }
        
        // Trigger: Death prevention at exactly 1 health with armor
        trigger("death_prevention") {
            `when` {
                objectType = "unit"
                propertyChanged = "health"
                newValue = "1"
                condition = "source.armor > 0"
            }
            effects {
                modifyProperty(
                    target = "self",
                    property = "armor",
                    delta = "-1"
                )
                modifyProperty(
                    target = "self",
                    property = "health",
                    delta = "2"
                )
            }
        }
        
        // Note: Removed LogEffect triggers as logging is now handled by the application layer
        // The CLI demo can observe state changes and log as needed
    }
    
    initialization {
        participantType = "participant"
        participantIdProperty = "player_id"
        singleton("game_state", "game_state") {
            properties {
                "turn_number" to 1
                "phase" to "setup"
            }
        }
        autoCreate("fireball", "healing_potion")
        createAllInstances = false
    }
}