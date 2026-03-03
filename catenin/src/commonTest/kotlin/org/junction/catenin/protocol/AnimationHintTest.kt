package org.junction.catenin.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimationHintTest {

    @Test
    fun testProjectile() {
        val hint = AnimationHint.Projectile(from = "card_1", to = "player_2", effect = "fireball")
        assertEquals("card_1", hint.from)
        assertEquals("player_2", hint.to)
        assertEquals("fireball", hint.effect)
    }

    @Test
    fun testFloatingText() {
        val hint = AnimationHint.FloatingText(target = "player_1", text = "-3 HP", style = "damage")
        assertEquals("player_1", hint.target)
        assertEquals("-3 HP", hint.text)
        assertEquals("damage", hint.style)
    }

    @Test
    fun testShake() {
        val hint = AnimationHint.Shake(target = "player_2", intensity = 0.5f)
        assertEquals("player_2", hint.target)
        assertEquals(0.5f, hint.intensity)
    }

    @Test
    fun testHighlight() {
        val targets = listOf("card_1", "card_2", "card_3")
        val hint = AnimationHint.Highlight(targets = targets, color = "yellow")
        assertEquals(targets, hint.targets)
        assertEquals("yellow", hint.color)
    }

    @Test
    fun testDelay() {
        val hint = AnimationHint.Delay(ms = 500)
        assertEquals(500, hint.ms)
    }

    @Test
    fun testPlaySound() {
        val hint = AnimationHint.PlaySound(soundId = "explosion_01")
        assertEquals("explosion_01", hint.soundId)
    }

    @Test
    fun testAnimationHintIsSealed() {
        val hints: List<AnimationHint> = listOf(
            AnimationHint.Projectile("a", "b", "fire"),
            AnimationHint.FloatingText("a", "text", "style"),
            AnimationHint.Shake("a", 1.0f),
            AnimationHint.Highlight(listOf("a"), "red"),
            AnimationHint.Delay(100),
            AnimationHint.PlaySound("sound")
        )
        assertEquals(6, hints.size)
        assertTrue(hints[0] is AnimationHint.Projectile)
        assertTrue(hints[1] is AnimationHint.FloatingText)
        assertTrue(hints[2] is AnimationHint.Shake)
        assertTrue(hints[3] is AnimationHint.Highlight)
        assertTrue(hints[4] is AnimationHint.Delay)
        assertTrue(hints[5] is AnimationHint.PlaySound)
    }

    @Test
    fun testDataClassEquality() {
        val hint1 = AnimationHint.Projectile("a", "b", "fire")
        val hint2 = AnimationHint.Projectile("a", "b", "fire")
        val hint3 = AnimationHint.Projectile("a", "b", "ice")
        assertEquals(hint1, hint2)
        assertTrue(hint1 != hint3)
    }
}
