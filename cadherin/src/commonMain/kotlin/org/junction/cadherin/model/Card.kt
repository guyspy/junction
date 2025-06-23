package org.junction.cadherin.model

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: String,
    val type: String,
    val properties: Map<String, CardPropertyValue>
) {
    fun getIntProperty(name: String): Int? {
        return when (val value = properties[name]) {
            is CardPropertyValue.IntValue -> value.value
            else -> null
        }
    }
    
    fun getStringProperty(name: String): String? {
        return when (val value = properties[name]) {
            is CardPropertyValue.StringValue -> value.value
            else -> null
        }
    }
}

@Serializable
sealed class CardPropertyValue {
    @Serializable
    data class IntValue(val value: Int) : CardPropertyValue()
    
    @Serializable
    data class StringValue(val value: String) : CardPropertyValue()
    
    @Serializable
    data class BooleanValue(val value: Boolean) : CardPropertyValue()
}

data class CardFactory(private val definition: GameDefinition) {
    private var cardIdCounter = 0
    
    fun generateCards(): List<Card> {
        val allCards = mutableListOf<Card>()
        
        definition.cards.forEach { (cardType, cardDef) ->
            repeat(cardDef.count) {
                val card = createCard(cardType, cardDef)
                allCards.add(card)
            }
        }
        
        return allCards
    }
    
    private fun createCard(cardType: String, definition: CardTypeDefinition): Card {
        val cardId = "${cardType}_${cardIdCounter++}"
        val properties = mutableMapOf<String, CardPropertyValue>()
        
        definition.properties.forEach { (propName, propDef) ->
            val value = generatePropertyValue(propDef)
            properties[propName] = value
        }
        
        return Card(cardId, cardType, properties)
    }
    
    private fun generatePropertyValue(definition: PropertyDefinition): CardPropertyValue {
        return when (definition.type) {
            "int" -> {
                val min = definition.min ?: 1
                val max = definition.max ?: 10
                val value = (min..max).random()
                CardPropertyValue.IntValue(value)
            }
            "enum" -> {
                val values = definition.values ?: listOf("default")
                val value = values.random()
                CardPropertyValue.StringValue(value)
            }
            "string" -> {
                CardPropertyValue.StringValue("default")
            }
            else -> CardPropertyValue.StringValue("unknown")
        }
    }
}