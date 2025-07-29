package org.junction.catenin

import org.junction.catenin.engine.GameEngine
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.*

/**
 * Display the current game state
 */
fun displayGameState(engine: GameEngine) {
    val world = engine.getWorld()
    val participants = world.getObjectsByType("participant")
    val units = world.getObjectsByType("unit")
    
    println("\n" + "=".repeat(60))
    println("GAME STATE")
    println("=".repeat(60))
    
    // Display participants
    println("\nParticipants:")
    participants.forEach { participant ->
        val wins = (participant.getProperty("wins") as? IntValue)?.value ?: 0
        val turn = (participant.getState("turn_count") as? IntValue)?.value ?: 0
        val active = (participant.getState("active") as? BoolValue)?.value ?: false
        
        println("  ${participant.id}:")
        println("    Wins: $wins")
        println("    Turn: $turn") 
        println("    Active: $active")
    }
    
    // Display units grouped by owner
    println("\nUnits on battlefield:")
    if (units.isEmpty()) {
        println("  (No units)")
    } else {
        val unitsByOwner = units.groupBy { unit ->
            val ownerRef = unit.getProperty("owner") as? ObjectRefValue
            ownerRef?.objectId ?: "Unowned"
        }
        
        unitsByOwner.forEach { (owner, ownerUnits) ->
            println("\n  $owner's units:")
            ownerUnits.forEach { unit ->
                displayUnit(unit, indent = "    ")
            }
        }
    }
}

/**
 * Display all objects in the world
 */
fun viewAllObjects(engine: GameEngine) {
    val world = engine.getWorld()
    val allObjects = world.getAllObjects()
    
    println("\nAll Game Objects (${allObjects.size} total):")
    
    val objectsByType = allObjects.groupBy { it.type }
    
    objectsByType.forEach { (type, objects) ->
        println("\n$type (${objects.size}):")
        objects.forEach { obj ->
            displayGameObject(obj, indent = "  ")
        }
    }
}

/**
 * Display a single unit with formatting
 */
fun displayUnit(unit: GameObject, indent: String = "") {
    val unitType = unit.getProperty("unit_type") ?: StringValue("Unknown")
    val health = (unit.getProperty("health") as? IntValue)?.value ?: 0
    val maxHealth = (unit.getProperty("max_health") as? IntValue)?.value ?: 10
    val attack = (unit.getProperty("attack") as? IntValue)?.value ?: 0
    val armor = (unit.getProperty("armor") as? IntValue)?.value ?: 0
    val tapped = (unit.getState("tapped") as? BoolValue)?.value ?: false
    
    val healthBar = createHealthBar(health, maxHealth)
    val status = if (tapped) " [TAPPED]" else ""
    
    println("$indent${unit.id} - $unitType$status")
    println("$indent  Health: $healthBar $health/$maxHealth")
    println("$indent  Attack: $attack, Armor: $armor")
}

/**
 * Display a list of units with indices
 */
fun displayUnits(units: List<GameObject>) {
    units.forEachIndexed { index, unit ->
        val unitType = unit.getProperty("unit_type") ?: StringValue("Unknown")
        val health = (unit.getProperty("health") as? IntValue)?.value ?: 0
        val maxHealth = (unit.getProperty("max_health") as? IntValue)?.value ?: 10
        val owner = (unit.getProperty("owner") as? ObjectRefValue)?.objectId ?: "Unowned"
        
        println("$index) $unitType (Owner: $owner, Health: $health/$maxHealth)")
    }
}

/**
 * Display any game object with all properties and states
 */
fun displayGameObject(obj: GameObject, indent: String = "") {
    println("$indent${obj.id} (${obj.type})")
    
    // Display properties
    if (obj.getAllPropertyNames().isNotEmpty()) {
        println("$indent  Properties:")
        obj.getAllPropertyNames().forEach { propName ->
            val value = obj.getProperty(propName)
            println("$indent    $propName: ${formatPropertyValue(value)}")
        }
    }
    
    // Display states
    if (obj.getAllStateNames().isNotEmpty()) {
        println("$indent  States:")
        obj.getAllStateNames().forEach { stateName ->
            val value = obj.getState(stateName)
            println("$indent    $stateName: ${formatPropertyValue(value)}")
        }
    }
}

/**
 * Format a property value for display
 */
fun formatPropertyValue(value: PropertyValue?): String {
    return when (value) {
        is IntValue -> value.value.toString()
        is StringValue -> "\"${value.value}\""
        is BoolValue -> value.value.toString()
        is ObjectRefValue -> "→ ${value.objectId}"
        null -> "null"
    }
}

/**
 * Create a visual health bar
 */
fun createHealthBar(current: Int, max: Int): String {
    val barLength = 10
    val filledLength = if (max > 0) (current * barLength / max) else 0
    val filled = "█".repeat(filledLength)
    val empty = "░".repeat(barLength - filledLength)
    
    return when {
        current <= max / 4 -> "[$filled$empty]" // Low health
        current <= max / 2 -> "[$filled$empty]" // Medium health
        else -> "[$filled$empty]" // High health
    }
}

/**
 * Wait for user to press enter
 */
fun waitForEnter() {
    print("\nPress Enter to continue...")
    readLine()
}