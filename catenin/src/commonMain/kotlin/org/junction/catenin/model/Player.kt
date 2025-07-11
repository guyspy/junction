package org.junction.catenin.model

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
@JsExport
data class Player(
    val id: String,
    val name: String,
    val hand: MutableList<Card> = mutableListOf(),
    var health: Int = 10,
    var score: Int = 0
) {
    fun hasCard(cardId: String): Boolean {
        return hand.any { it.id == cardId }
    }
    
    fun removeCard(cardId: String): Card? {
        val index = hand.indexOfFirst { it.id == cardId }
        return if (index >= 0) {
            hand.removeAt(index)
        } else {
            null
        }
    }
    
    fun addCard(card: Card) {
        hand.add(card)
    }
    
    fun isAlive(): Boolean = health > 0
    
    fun takeDamage(amount: Int) {
        health = maxOf(0, health - amount)
    }
    
    fun heal(amount: Int) {
        health += amount
    }
}