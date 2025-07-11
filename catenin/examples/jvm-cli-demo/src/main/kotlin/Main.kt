package org.junction.catenin

import org.junction.catenin.core.GameEngine
import org.junction.catenin.model.CardFactory
import org.junction.catenin.parser.GameDefinitionParser

fun main() {
    println("=== Catenin DSL Day 1 Demo ===")
    
    try {
        // Test YAML parsing
        val gameYaml = """
            meta:
              name: "Day 1 測試遊戲"
              target_age: [8, 12]
            
            cards:
              number_card:
                count: 8
                properties:
                  value:
                    type: int
                    min: 1
                    max: 5
                  color:
                    type: enum
                    values: [red, blue, green]
        """.trimIndent()
        
        // Parse game definition
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(gameYaml)
        
        println("✅ 成功解析遊戲: ${definition.meta.name}")
        
        // Generate cards
        val cardFactory = CardFactory(definition)
        val cards = cardFactory.generateCards()
        
        println("✅ 成功生成 ${cards.size} 張卡牌")
        
        // Display cards
        cards.take(5).forEach { card ->
            val value = card.getIntProperty("value")
            val color = card.getStringProperty("color")
            println("   ${card.id}: ${color}色數字${value}")
        }
        
        // Test GameEngine creation
        val engine = GameEngine.fromYaml(gameYaml, listOf("Alice", "Bob"))
        println("✅ 成功創建遊戲引擎")
        println("   玩家: ${engine.getPlayers().map { it.name }}")
        
        println("\n🎉 Day 1 目標達成！")
        
    } catch (e: Exception) {
        println("❌ 執行失敗: ${e.message}")
        e.printStackTrace()
    }
}