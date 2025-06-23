package org.junction.cadherin.platform

actual fun readPlatformFile(filePath: String): String {
    // JS environment, file content needs to be passed from external source
    throw UnsupportedOperationException("File reading not supported in JS environment. Use parseFromString instead.")
}

// JS specific console output
@JsExport
object GameConsole {
    fun log(message: String) {
        console.log(message)
    }
    
    fun displayCard(card: org.junction.cadherin.model.Card) {
        val damage = card.getIntProperty("damage")
        val element = card.getStringProperty("element")
        console.log("Card ${card.id}: $element element, $damage damage")
    }
}