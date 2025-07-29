package org.junction.catenin

import org.junction.catenin.core.*
import org.junction.catenin.engine.GameEngine
import org.junction.catenin.model.values.*

fun main() {
    println("=== Catenin Universal Game Engine - Interactive Demo ===")
    println()
    
    try {
        // Create game schema
        val schema = createSampleGameSchema()
        
        println("Welcome to ${schema.meta.name}!")
        println("A game for ${schema.meta.targetAge[0]}-${schema.meta.targetAge[1]} year olds")
        println()
        
        // Get player names
        print("Enter number of players (${schema.meta.participantCount[0]}-${schema.meta.participantCount[1]}): ")
        val playerCount = readLine()?.toIntOrNull() ?: 2
        
        if (playerCount < schema.meta.participantCount[0] || playerCount > schema.meta.participantCount[1]) {
            println("Invalid player count. Using 2 players.")
        }
        
        val playerIds = mutableListOf<String>()
        repeat(minOf(playerCount, schema.meta.participantCount[1])) { index ->
            print("Enter name for player ${index + 1}: ")
            val name = readLine()?.trim() ?: "Player${index + 1}"
            playerIds.add(name)
        }
        
        // Initialize game with schema's initialization configuration
        val engine = GameEngine.fromSchema(schema, schema.initialization).initializeGame(playerIds)
        
        println("\n✅ Game initialized!")
        println("Players: ${playerIds.joinToString(", ")}")
        println()
        
        // Start game loop
        gameLoop(engine)
        
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}

fun gameLoop(engine: GameEngine) {
    var running = true
    
    while (running) {
        displayGameState(engine)
        
        println("\nActions:")
        println("1) Create a unit")
        println("2) Cast a spell")
        println("3) Attack with unit")
        println("4) View all objects")
        println("5) View triggers")
        println("6) End turn")
        println("0) Exit game")
        
        print("Choose action: ")
        
        when (readLine()) {
            "1" -> createUnit(engine)
            "2" -> castSpell(engine)
            "3" -> attackWithUnit(engine)
            "4" -> viewAllObjects(engine)
            "5" -> viewTriggers(engine)
            "6" -> endTurn(engine)
            "0" -> {
                println("\nThanks for playing!")
                running = false
            }
            else -> println("Invalid choice")
        }
        
        println()
    }
}

fun createUnit(engine: GameEngine) {
    val world = engine.getWorld()
    val participants = world.getObjectsByType("participant")
    
    if (participants.isEmpty()) {
        println("No participants found!")
        return
    }
    
    println("\nCreate unit for which participant?")
    participants.forEachIndexed { index, participant ->
        println("$index) ${participant.id}")
    }
    
    print("Choose participant: ")
    val participantIndex = readLine()?.toIntOrNull() ?: return
    
    if (participantIndex !in participants.indices) {
        println("Invalid choice")
        return
    }
    
    val owner = participants[participantIndex]
    
    println("\nUnit types:")
    println("1) Warrior (10 health, 3 attack)")
    println("2) Archer (8 health, 4 attack)")
    println("3) Mage (6 health, 5 attack)")
    
    print("Choose unit type: ")
    
    val unit = when (readLine()) {
        "1" -> engine.createObject(
            "unit",
            propertyOverrides = mapOf(
                "health" to "10",
                "max_health" to "10",
                "attack" to "3",
                "owner" to owner.id,
                "unit_type" to "Warrior"
            )
        )
        "2" -> engine.createObject(
            "unit",
            propertyOverrides = mapOf(
                "health" to "8",
                "max_health" to "8",
                "attack" to "4",
                "owner" to owner.id,
                "unit_type" to "Archer"
            )
        )
        "3" -> engine.createObject(
            "unit",
            propertyOverrides = mapOf(
                "health" to "6",
                "max_health" to "6",
                "attack" to "5",
                "owner" to owner.id,
                "unit_type" to "Mage"
            )
        )
        else -> {
            println("Invalid choice")
            return
        }
    }
    
    engine.addObject(unit)
    println("✅ Created ${unit.getProperty("unit_type")} for ${owner.id}")
}

fun castSpell(engine: GameEngine) {
    val world = engine.getWorld()
    val units = world.getObjectsByType("unit")
    
    if (units.isEmpty()) {
        println("No units to target!")
        return
    }
    
    println("\nAvailable spells:")
    println("1) Lightning Bolt (5 damage to one unit)")
    println("2) Heal (restore 4 health to one unit)")
    println("3) Armor Boost (+3 armor to one unit)")
    
    print("Choose spell: ")
    val spellChoice = readLine()
    
    println("\nTarget which unit?")
    displayUnits(units)
    
    print("Choose target: ")
    val targetIndex = readLine()?.toIntOrNull() ?: return
    
    if (targetIndex !in units.indices) {
        println("Invalid target")
        return
    }
    
    val target = units[targetIndex]
    
    when (spellChoice) {
        "1" -> {
            // Lightning Bolt
            val currentHealth = (target.getProperty("health") as? IntValue)?.value ?: 0
            val newHealth = maxOf(0, currentHealth - 5)
            engine.updateProperty(target.id, "health", IntValue(newHealth))
            println("⚡ Lightning Bolt deals 5 damage to ${target.getProperty("unit_type")}")
            if (newHealth <= 0) {
                engine.removeObject(target.id)
                println("💀 ${target.getProperty("unit_type")} was destroyed!")
            }
        }
        "2" -> {
            // Heal
            val currentHealth = (target.getProperty("health") as? IntValue)?.value ?: 0
            val maxHealth = (target.getProperty("max_health") as? IntValue)?.value ?: 10
            val newHealth = minOf(maxHealth, currentHealth + 4)
            engine.updateProperty(target.id, "health", IntValue(newHealth))
            println("✨ Heal restores 4 health to ${target.getProperty("unit_type")}")
        }
        "3" -> {
            // Armor Boost
            val currentArmor = (target.getProperty("armor") as? IntValue)?.value ?: 0
            engine.updateProperty(target.id, "armor", IntValue(currentArmor + 3))
            println("🛡️ Armor Boost grants +3 armor to ${target.getProperty("unit_type")}")
        }
        else -> println("Invalid spell")
    }
}

fun attackWithUnit(engine: GameEngine) {
    val world = engine.getWorld()
    val units = world.getObjectsByType("unit")
    
    if (units.size < 2) {
        println("Need at least 2 units for combat!")
        return
    }
    
    println("\nAttack with which unit?")
    displayUnits(units)
    
    print("Choose attacker: ")
    val attackerIndex = readLine()?.toIntOrNull() ?: return
    
    if (attackerIndex !in units.indices) {
        println("Invalid attacker")
        return
    }
    
    val attacker = units[attackerIndex]
    
    println("\nAttack which unit?")
    val targets = units.filterIndexed { index, _ -> index != attackerIndex }
    displayUnits(targets)
    
    print("Choose target: ")
    val targetIndex = readLine()?.toIntOrNull() ?: return
    
    if (targetIndex !in targets.indices) {
        println("Invalid target")
        return
    }
    
    val target = targets[targetIndex]
    val attackPower = (attacker.getProperty("attack") as? IntValue)?.value ?: 0
    val targetArmor = (target.getProperty("armor") as? IntValue)?.value ?: 0
    val damage = maxOf(1, attackPower - targetArmor)
    
    val currentHealth = (target.getProperty("health") as? IntValue)?.value ?: 0
    val newHealth = maxOf(0, currentHealth - damage)
    
    engine.updateProperty(target.id, "health", IntValue(newHealth))
    
    println("⚔️ ${attacker.getProperty("unit_type")} attacks ${target.getProperty("unit_type")} for $damage damage!")
    
    if (newHealth <= 0) {
        engine.removeObject(target.id)
        println("💀 ${target.getProperty("unit_type")} was destroyed!")
    }
}

fun endTurn(engine: GameEngine) {
    val world = engine.getWorld()
    val participants = world.getObjectsByType("participant")
    
    if (participants.isNotEmpty()) {
        val currentTurn = (participants[0].getState("turn_count") as? IntValue)?.value ?: 0
        participants.forEach { participant ->
            engine.updateState(participant.id, "turn_count", IntValue(currentTurn + 1))
        }
        println("Turn ${currentTurn + 1} ended. Starting turn ${currentTurn + 2}.")
    }
}

fun viewTriggers(engine: GameEngine) {
    val schema = engine.getSchema()
    
    println("\nActive Triggers:")
    schema.triggers.forEach { trigger ->
        println("\n${trigger.name ?: "Unnamed Trigger"}:")
        println("  When: ${trigger.`when`.objectType ?: "any"} object")
        trigger.`when`.propertyChanged?.let {
            println("        property '$it' changes")
        }
        trigger.`when`.newValue?.let {
            println("        to value '$it'")
        }
        println("  Effects:")
        trigger.effects.forEach { effect ->
            println("    - $effect")
        }
    }
}