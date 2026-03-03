package org.junction.catenin.protocol

import org.junction.catenin.core.GameWorld
import org.junction.catenin.model.objects.GameObject
import org.junction.catenin.model.values.IntValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEventTest {

    private fun createTestWorld(): GameWorld {
        val player = GameObject("player_1", "player", properties = mapOf("health" to IntValue(10)))
        return GameWorld.empty().withObjects(listOf(player))
    }

    private fun createTestOptions(): GameOptions {
        return GameOptions(
            participantId = "player_1",
            actions = listOf(
                AvailableAction("end_turn", "player_1", emptyList(), emptyMap())
            )
        )
    }

    // --- GameResult tests ---

    @Test
    fun testGameResultWithWinner() {
        val result = GameResult(winnerId = "player_1", reason = "Health depleted")
        assertEquals("player_1", result.winnerId)
        assertEquals("Health depleted", result.reason)
    }

    @Test
    fun testGameResultDraw() {
        val result = GameResult(winnerId = null, reason = "Time limit reached")
        assertNull(result.winnerId)
        assertEquals("Time limit reached", result.reason)
    }

    @Test
    fun testGameResultEquality() {
        val r1 = GameResult("player_1", "win")
        val r2 = GameResult("player_1", "win")
        assertEquals(r1, r2)
    }

    // --- PlayerAction tests ---

    @Test
    fun testCreatePlayerAction() {
        val action = PlayerAction(
            type = "play_card",
            sourceId = "card_1",
            targetId = "player_2",
            metadata = mapOf("slot" to "1")
        )
        assertEquals("play_card", action.type)
        assertEquals("card_1", action.sourceId)
        assertEquals("player_2", action.targetId)
        assertEquals("1", action.metadata["slot"])
    }

    @Test
    fun testPlayerActionWithoutTarget() {
        val action = PlayerAction(
            type = "end_turn",
            sourceId = "player_1",
            targetId = null,
            metadata = emptyMap()
        )
        assertNull(action.targetId)
        assertTrue(action.metadata.isEmpty())
    }

    @Test
    fun testPlayerActionEquality() {
        val a1 = PlayerAction("play_card", "card_1", "p2", emptyMap())
        val a2 = PlayerAction("play_card", "card_1", "p2", emptyMap())
        assertEquals(a1, a2)
    }

    // --- GameEvent sealed class tests ---

    @Test
    fun testGameStartedEvent() {
        val world = createTestWorld()
        val options = createTestOptions()
        val event = GameEvent.GameStarted(world = world, options = options)

        assertTrue(event is GameEvent)
        assertEquals("player_1", event.options.participantId)
    }

    @Test
    fun testEffectBlockEvent() {
        val block = EffectBlock(
            type = BlockType.ACTION,
            sourceId = "player_1",
            updates = emptyList(),
            children = emptyList(),
            animationHints = emptyList()
        )
        val event = GameEvent.EffectBlockEvent(block = block)

        assertTrue(event is GameEvent)
        assertEquals(BlockType.ACTION, event.block.type)
    }

    @Test
    fun testOptionsUpdatedEvent() {
        val options = createTestOptions()
        val event = GameEvent.OptionsUpdated(options = options)

        assertTrue(event is GameEvent)
        assertEquals(1, event.options.actions.size)
    }

    @Test
    fun testGameEndedEvent() {
        val result = GameResult(winnerId = "player_1", reason = "Opponent surrendered")
        val event = GameEvent.GameEnded(result = result)

        assertTrue(event is GameEvent)
        assertEquals("player_1", event.result.winnerId)
    }

    @Test
    fun testGameEventIsSealed() {
        val world = createTestWorld()
        val options = createTestOptions()
        val events: List<GameEvent> = listOf(
            GameEvent.GameStarted(world, options),
            GameEvent.EffectBlockEvent(EffectBlock(BlockType.ACTION, "p1", emptyList(), emptyList(), emptyList())),
            GameEvent.OptionsUpdated(options),
            GameEvent.GameEnded(GameResult("p1", "win"))
        )
        assertEquals(4, events.size)
        assertTrue(events[0] is GameEvent.GameStarted)
        assertTrue(events[1] is GameEvent.EffectBlockEvent)
        assertTrue(events[2] is GameEvent.OptionsUpdated)
        assertTrue(events[3] is GameEvent.GameEnded)
    }

    // --- GameRenderer interface tests (using TestRenderer) ---

    @Test
    fun testGameRendererReceivesEvents() {
        val renderer = TestRenderer()
        val world = createTestWorld()
        val options = createTestOptions()
        val block = EffectBlock(BlockType.ACTION, "p1", emptyList(), emptyList(), emptyList())
        val result = GameResult("p1", "win")

        renderer.onGameStart(world, options)
        renderer.onEffectBlock(block)
        renderer.onOptionsUpdate(options)
        renderer.onGameEnd(result)

        assertEquals(4, renderer.events.size)
        assertEquals("gameStart", renderer.events[0])
        assertEquals("effectBlock", renderer.events[1])
        assertEquals("optionsUpdate", renderer.events[2])
        assertEquals("gameEnd", renderer.events[3])
    }

    // --- GameInput interface tests ---

    @Test
    fun testGameInputSubmitAction() {
        val input = TestGameInput()
        val action = PlayerAction("play_card", "card_1", "player_2", emptyMap())

        input.submitAction("player_1", action)

        assertEquals(1, input.submittedActions.size)
        assertEquals("player_1", input.submittedActions[0].first)
        assertEquals("play_card", input.submittedActions[0].second.type)
    }
}

/**
 * Test implementation of GameRenderer that records calls.
 */
class TestRenderer : GameRenderer {
    val events = mutableListOf<String>()

    override fun onGameStart(world: GameWorld, options: GameOptions) {
        events.add("gameStart")
    }

    override fun onEffectBlock(block: EffectBlock) {
        events.add("effectBlock")
    }

    override fun onOptionsUpdate(options: GameOptions) {
        events.add("optionsUpdate")
    }

    override fun onGameEnd(result: GameResult) {
        events.add("gameEnd")
    }
}

/**
 * Test implementation of GameInput that records submitted actions.
 */
class TestGameInput : GameInput {
    val submittedActions = mutableListOf<Pair<String, PlayerAction>>()

    override fun submitAction(participantId: String, action: PlayerAction) {
        submittedActions.add(participantId to action)
    }
}
