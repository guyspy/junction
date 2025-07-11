package org.junction.catenin.model

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
@JsExport
data class Player(
    val id: String,
    val name: String,
    val hand: Array<Card> = emptyArray(),
    val health: Int = 10,
    val score: Int = 0
) {
    fun hasCard(cardId: String): Boolean {
        return hand.any { it.id == cardId }
    }
    
    fun removeCard(cardId: String): Pair<Player, Card?> {
        val index = hand.indexOfFirst { it.id == cardId }
        return if (index >= 0) {
            val card = hand[index]
            val newHand = hand.filterIndexed { i, _ -> i != index }.toTypedArray()
            Pair(copy(hand = newHand), card)
        } else {
            Pair(this, null)
        }
    }
    
    fun addCard(card: Card): Player {
        return copy(hand = hand + card)
    }
    
    fun isAlive(): Boolean = health > 0
    
    fun takeDamage(amount: Int): Player {
        return copy(health = maxOf(0, health - amount))
    }
    
    fun heal(amount: Int): Player {
        return copy(health = health + amount)
    }
    
    fun addScore(points: Int): Player {
        return copy(score = score + points)
    }
    
    // Convenience methods for backward compatibility during transition
    @Deprecated("Use removeCard() which returns updated Player", ReplaceWith("removeCard(cardId).second"))
    fun removeCardLegacy(cardId: String): Card? {
        return removeCard(cardId).second
    }
    
    @Deprecated("Use addCard() which returns updated Player", ReplaceWith("addCard(card)"))
    fun addCardLegacy(card: Card) {
        // This method is deprecated and will be removed
        throw UnsupportedOperationException("Use immutable addCard() method instead")
    }
}