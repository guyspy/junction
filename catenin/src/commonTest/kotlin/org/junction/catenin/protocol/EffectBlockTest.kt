package org.junction.catenin.protocol

import org.junction.catenin.core.UpdatePropertyUpdate
import org.junction.catenin.core.WorldUpdate
import org.junction.catenin.model.values.IntValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectBlockTest {

    @Test
    fun testBlockTypeValues() {
        val values = BlockType.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(BlockType.ACTION))
        assertTrue(values.contains(BlockType.EFFECT))
        assertTrue(values.contains(BlockType.TRIGGER))
        assertTrue(values.contains(BlockType.CASCADE))
    }

    @Test
    fun testCreateSimpleBlock() {
        val update = UpdatePropertyUpdate("player_1", "health", IntValue(7))
        val block = EffectBlock(
            type = BlockType.ACTION,
            sourceId = "player_1",
            updates = listOf(update),
            children = emptyList(),
            animationHints = emptyList()
        )

        assertEquals(BlockType.ACTION, block.type)
        assertEquals("player_1", block.sourceId)
        assertEquals(1, block.updates.size)
        assertEquals(0, block.children.size)
        assertEquals(0, block.animationHints.size)
    }

    @Test
    fun testCreateEmptyBlock() {
        val block = EffectBlock(
            type = BlockType.EFFECT,
            sourceId = "card_5",
            updates = emptyList(),
            children = emptyList(),
            animationHints = emptyList()
        )

        assertEquals(BlockType.EFFECT, block.type)
        assertEquals("card_5", block.sourceId)
        assertTrue(block.updates.isEmpty())
        assertTrue(block.children.isEmpty())
        assertTrue(block.animationHints.isEmpty())
    }

    @Test
    fun testNestedBlocks() {
        val innerUpdate = UpdatePropertyUpdate("player_2", "health", IntValue(5))
        val innerBlock = EffectBlock(
            type = BlockType.TRIGGER,
            sourceId = "trap_1",
            updates = listOf(innerUpdate),
            children = emptyList(),
            animationHints = listOf(AnimationHint.Shake("player_2", 0.8f))
        )

        val outerUpdate = UpdatePropertyUpdate("card_1", "used", IntValue(1))
        val outerBlock = EffectBlock(
            type = BlockType.ACTION,
            sourceId = "player_1",
            updates = listOf(outerUpdate),
            children = listOf(innerBlock),
            animationHints = listOf(
                AnimationHint.Projectile("card_1", "player_2", "fireball")
            )
        )

        assertEquals(BlockType.ACTION, outerBlock.type)
        assertEquals(1, outerBlock.children.size)
        assertEquals(BlockType.TRIGGER, outerBlock.children[0].type)
        assertEquals("trap_1", outerBlock.children[0].sourceId)
    }

    @Test
    fun testDeeplyNestedBlocks() {
        // CASCADE inside TRIGGER inside EFFECT inside ACTION
        val cascadeBlock = EffectBlock(
            type = BlockType.CASCADE,
            sourceId = "aura_1",
            updates = listOf(UpdatePropertyUpdate("minion_3", "attack", IntValue(2))),
            children = emptyList(),
            animationHints = emptyList()
        )

        val triggerBlock = EffectBlock(
            type = BlockType.TRIGGER,
            sourceId = "trap_1",
            updates = listOf(UpdatePropertyUpdate("player_2", "health", IntValue(5))),
            children = listOf(cascadeBlock),
            animationHints = emptyList()
        )

        val effectBlock = EffectBlock(
            type = BlockType.EFFECT,
            sourceId = "spell_1",
            updates = listOf(UpdatePropertyUpdate("player_2", "health", IntValue(7))),
            children = listOf(triggerBlock),
            animationHints = emptyList()
        )

        val actionBlock = EffectBlock(
            type = BlockType.ACTION,
            sourceId = "player_1",
            updates = emptyList(),
            children = listOf(effectBlock),
            animationHints = emptyList()
        )

        assertEquals(BlockType.ACTION, actionBlock.type)
        assertEquals(1, actionBlock.children.size)
        assertEquals(BlockType.EFFECT, actionBlock.children[0].type)
        assertEquals(1, actionBlock.children[0].children.size)
        assertEquals(BlockType.TRIGGER, actionBlock.children[0].children[0].type)
        assertEquals(1, actionBlock.children[0].children[0].children.size)
        assertEquals(BlockType.CASCADE, actionBlock.children[0].children[0].children[0].type)
    }

    @Test
    fun testFlattenUpdates() {
        val cascadeBlock = EffectBlock(
            type = BlockType.CASCADE,
            sourceId = "aura_1",
            updates = listOf(UpdatePropertyUpdate("minion_3", "attack", IntValue(2))),
            children = emptyList(),
            animationHints = emptyList()
        )

        val triggerBlock = EffectBlock(
            type = BlockType.TRIGGER,
            sourceId = "trap_1",
            updates = listOf(UpdatePropertyUpdate("player_2", "health", IntValue(5))),
            children = listOf(cascadeBlock),
            animationHints = emptyList()
        )

        val actionBlock = EffectBlock(
            type = BlockType.ACTION,
            sourceId = "player_1",
            updates = listOf(UpdatePropertyUpdate("card_1", "used", IntValue(1))),
            children = listOf(triggerBlock),
            animationHints = emptyList()
        )

        val allUpdates = actionBlock.flattenUpdates()

        // Should be depth-first: action's own updates, then trigger's, then cascade's
        assertEquals(3, allUpdates.size)
        assertEquals("card_1", (allUpdates[0] as UpdatePropertyUpdate).objectId)
        assertEquals("player_2", (allUpdates[1] as UpdatePropertyUpdate).objectId)
        assertEquals("minion_3", (allUpdates[2] as UpdatePropertyUpdate).objectId)
    }

    @Test
    fun testFlattenUpdatesEmptyTree() {
        val block = EffectBlock(
            type = BlockType.ACTION,
            sourceId = "player_1",
            updates = emptyList(),
            children = emptyList(),
            animationHints = emptyList()
        )

        val allUpdates = block.flattenUpdates()
        assertTrue(allUpdates.isEmpty())
    }

    @Test
    fun testBlockWithAnimationHints() {
        val block = EffectBlock(
            type = BlockType.EFFECT,
            sourceId = "spell_1",
            updates = listOf(UpdatePropertyUpdate("player_2", "health", IntValue(3))),
            children = emptyList(),
            animationHints = listOf(
                AnimationHint.Projectile("spell_1", "player_2", "lightning"),
                AnimationHint.FloatingText("player_2", "-7 HP", "damage"),
                AnimationHint.Shake("player_2", 1.0f)
            )
        )

        assertEquals(3, block.animationHints.size)
        assertTrue(block.animationHints[0] is AnimationHint.Projectile)
        assertTrue(block.animationHints[1] is AnimationHint.FloatingText)
        assertTrue(block.animationHints[2] is AnimationHint.Shake)
    }

    @Test
    fun testDataClassEquality() {
        val block1 = EffectBlock(
            type = BlockType.ACTION,
            sourceId = "player_1",
            updates = emptyList(),
            children = emptyList(),
            animationHints = emptyList()
        )
        val block2 = EffectBlock(
            type = BlockType.ACTION,
            sourceId = "player_1",
            updates = emptyList(),
            children = emptyList(),
            animationHints = emptyList()
        )
        assertEquals(block1, block2)
    }
}
