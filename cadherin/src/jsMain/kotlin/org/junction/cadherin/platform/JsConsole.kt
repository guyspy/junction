package org.junction.cadherin.platform

import org.junction.cadherin.model.Card

// JS specific console output
object GameConsole {
    fun log(message: String) {
        console.log(message)
    }
    
    fun displayCard(card: Card) {
        val damage = card.getIntProperty("damage")
        val element = card.getStringProperty("element")
        console.log("Card ${card.id}: $element element, $damage damage")
    }
}