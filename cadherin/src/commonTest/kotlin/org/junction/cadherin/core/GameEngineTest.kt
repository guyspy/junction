package org.junction.cadherin.core

import org.junction.cadherin.model.GameDefinition
import org.junction.cadherin.model.GameMeta
import org.junction.cadherin.model.CardTypeDefinition
import org.junction.cadherin.model.PropertyDefinition
import org.junction.cadherin.model.GameMechanics
import org.junction.cadherin.model.SetupMechanics
import org.junction.cadherin.model.PlayerSetup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameEngineTest {
    
    @Test
    fun testGameEngineCreation() {
        val yaml = """
            meta:
              name: "Test Game"
              target_age: [8, 12]
            
            cards:
              number_card:
                count: 5
                properties:
                  value:
                    type: int
                    min: 1
                    max: 5
            
            mechanics:
              setup:
                players:
                  health: 20
        """.trimIndent()
        
        val engine = GameEngine.fromYaml(yaml, listOf("Alice", "Bob"))
        
        val definition = engine.getGameDefinition()
        assertEquals("Test Game", definition.meta.name)
        
        val players = engine.getPlayers()
        assertEquals(2, players.size)
        assertEquals("Alice", players[0].name)
        assertEquals("Bob", players[1].name)
        assertEquals("player_0", players[0].id)
        assertEquals("player_1", players[1].id)
        assertEquals(20, players[0].health)
        assertEquals(20, players[1].health)
    }
    
    @Test
    fun testGameEngineWithDefaultHealth() {
        val yaml = """
            meta:
              name: "Simple Game"
              target_age: [8, 12]
            
            cards:
              basic_card:
                count: 2
                properties:
                  power:
                    type: int
                    min: 1
                    max: 3
        """.trimIndent()
        
        val engine = GameEngine.fromYaml(yaml, listOf("Player1"))
        val players = engine.getPlayers()
        
        assertEquals(1, players.size)
        assertEquals(10, players[0].health) // Default health
    }
}