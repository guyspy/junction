package org.junction.catenin.model

import kotlinx.serialization.Serializable
import org.junction.catenin.utils.GameRandom
import kotlin.js.JsExport

@Serializable
@JsExport
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
@JsExport
sealed class CardPropertyValue {
    @Serializable
    data class IntValue(val value: Int) : CardPropertyValue()
    
    @Serializable
    data class StringValue(val value: String) : CardPropertyValue()
    
    @Serializable
    data class BooleanValue(val value: Boolean) : CardPropertyValue()
}

@JsExport
class CardFactory private constructor(private val definition: GameDefinition) {
    private var cardIdCounter = 0
    
    companion object {
        fun fromDefinition(definition: GameDefinition): CardFactory {
            return CardFactory(definition)
        }
    }
    
    // JavaScript-friendly method that returns Array by default
    fun generateCards(): Array<Card> {
        val allCards = mutableListOf<Card>()
        
        definition.cards.forEach { (cardType, cardDef) ->
            repeat(cardDef.count) {
                val card = createCard(cardType, cardDef)
                allCards.add(card)
            }
        }
        
        return allCards.toTypedArray()
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
                val value = GameRandom.nextInt(min, max)
                CardPropertyValue.IntValue(value)
            }
            "enum" -> {
                val values = definition.values ?: listOf("default")
                val value = GameRandom.choose(values) ?: "default"
                CardPropertyValue.StringValue(value)
            }
            "string" -> {
                CardPropertyValue.StringValue("default")
            }
            else -> CardPropertyValue.StringValue("unknown")
        }
    }
}

// Top-level factory function for JavaScript compatibility
@JsExport
fun createCardFactory(definition: GameDefinition): CardFactory {
    return CardFactory.fromDefinition(definition)
}