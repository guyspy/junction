package org.junction.catenin.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameOptionsTest {

    @Test
    fun testCreateAvailableAction() {
        val action = AvailableAction(
            type = "play_card",
            sourceId = "card_1",
            targets = listOf("player_2", "minion_1"),
            metadata = mapOf("card_name" to "Fireball", "mana_cost" to "4")
        )

        assertEquals("play_card", action.type)
        assertEquals("card_1", action.sourceId)
        assertEquals(listOf("player_2", "minion_1"), action.targets)
        assertEquals("Fireball", action.metadata["card_name"])
        assertEquals("4", action.metadata["mana_cost"])
    }

    @Test
    fun testAvailableActionWithEmptyTargets() {
        val action = AvailableAction(
            type = "end_turn",
            sourceId = "player_1",
            targets = emptyList(),
            metadata = emptyMap()
        )

        assertTrue(action.targets.isEmpty())
        assertTrue(action.metadata.isEmpty())
    }

    @Test
    fun testCreateGameOptions() {
        val actions = listOf(
            AvailableAction("play_card", "card_1", listOf("player_2"), emptyMap()),
            AvailableAction("play_card", "card_2", listOf("minion_1", "minion_2"), emptyMap()),
            AvailableAction("end_turn", "player_1", emptyList(), emptyMap())
        )
        val options = GameOptions(
            participantId = "player_1",
            actions = actions
        )

        assertEquals("player_1", options.participantId)
        assertEquals(3, options.actions.size)
        assertEquals("play_card", options.actions[0].type)
        assertEquals("end_turn", options.actions[2].type)
    }

    @Test
    fun testGameOptionsWithNoActions() {
        val options = GameOptions(
            participantId = "player_2",
            actions = emptyList()
        )

        assertEquals("player_2", options.participantId)
        assertTrue(options.actions.isEmpty())
    }

    @Test
    fun testAvailableActionDataClassEquality() {
        val action1 = AvailableAction("play_card", "card_1", listOf("player_2"), mapOf("cost" to "3"))
        val action2 = AvailableAction("play_card", "card_1", listOf("player_2"), mapOf("cost" to "3"))
        val action3 = AvailableAction("play_card", "card_1", listOf("player_2"), mapOf("cost" to "5"))

        assertEquals(action1, action2)
        assertTrue(action1 != action3)
    }

    @Test
    fun testGameOptionsDataClassEquality() {
        val actions = listOf(AvailableAction("end_turn", "p1", emptyList(), emptyMap()))
        val options1 = GameOptions("player_1", actions)
        val options2 = GameOptions("player_1", actions)

        assertEquals(options1, options2)
    }

    @Test
    fun testMultipleActionTypes() {
        val options = GameOptions(
            participantId = "player_1",
            actions = listOf(
                AvailableAction("play_card", "card_1", listOf("player_2"), mapOf("card_name" to "Heal")),
                AvailableAction("move_piece", "piece_1", listOf("tile_a", "tile_b", "tile_c"), emptyMap()),
                AvailableAction("use_ability", "hero_1", listOf("minion_1"), mapOf("ability" to "shield_bash")),
                AvailableAction("end_turn", "player_1", emptyList(), emptyMap())
            )
        )

        assertEquals(4, options.actions.size)
        // Verify each action type
        assertEquals("play_card", options.actions[0].type)
        assertEquals("move_piece", options.actions[1].type)
        assertEquals("use_ability", options.actions[2].type)
        assertEquals("end_turn", options.actions[3].type)
        // Move piece has 3 valid targets
        assertEquals(3, options.actions[1].targets.size)
    }
}
