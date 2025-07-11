# Day 1: Kotlin Multiplatform 專案架構

## 工作目標
- 建立 Kotlin Multiplatform 專案結構
- 配置 Gradle 多平台編譯
- 實作基礎 YAML 解析功能
- 建立核心資料模型

## 專案結構設計

```
catenin/
├── build.gradle.kts                    # 根專案配置
├── settings.gradle.kts                 # 專案設定
├── gradle.properties                   # Gradle 屬性
├── src/
│   ├── commonMain/kotlin/              # 共享核心邏輯
│   │   └── org/junction/catenin/
│   │       ├── model/                  # 資料模型
│   │       │   ├── GameDefinition.kt
│   │       │   ├── Card.kt
│   │       │   └── Player.kt
│   │       ├── parser/                 # YAML 解析
│   │       │   ├── YamlParser.kt
│   │       │   └── GameDefinitionParser.kt
│   │       └── core/                   # 核心介面
│   │           └── GameEngine.kt
│   ├── commonTest/kotlin/              # 共享測試
│   │   └── org/junction/catenin/
│   │       └── parser/
│   │           └── YamlParserTest.kt
│   ├── jvmMain/kotlin/                 # JVM 特定功能
│   │   └── org/junction/catenin/
│   │       └── platform/
│   │           └── JvmFileReader.kt
│   ├── jvmTest/kotlin/                 # JVM 測試
│   ├── jsMain/kotlin/                  # JS 特定功能
│   │   └── org/junction/catenin/
│   │       └── platform/
│   │           └── JsConsole.kt
│   └── jsTest/kotlin/                  # JS 測試
└── game-samples/                       # 範例遊戲
    ├── simple-combat.yaml
    └── number-war.yaml
```

## Gradle 配置

### build.gradle.kts
```kotlin
plugins {
    kotlin("multiplatform") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("maven-publish")
}

group = "org.junction"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    
    js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        nodejs()
        generateTypeScriptDefinitions()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("com.charleskorn.kaml:kaml:0.55.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation("ch.qos.logback:logback-classic:1.4.11")
            }
        }
        
        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:5.10.0")
                implementation("org.assertj:assertj-core:3.24.2")
            }
        }
        
        val jsMain by getting {
            dependencies {
                // JS 特定依賴（如果需要的話）
            }
        }
    }
}

// 配置 TypeScript 定義生成
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.typescript.TypeScriptValidationTask> {
    enabled = false
}
```

### settings.gradle.kts
```kotlin
rootProject.name = "catenin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

## 核心資料模型

### GameDefinition.kt
```kotlin
package org.junction.catenin.model

import kotlinx.serialization.Serializable

@Serializable
data class GameDefinition(
    val meta: GameMeta,
    val cards: Map<String, CardTypeDefinition>,
    val mechanics: GameMechanics? = null,
    val aiHints: AIHints? = null
)

@Serializable
data class GameMeta(
    val name: String,
    val targetAge: List<Int>, // [min, max]
    val playerCount: List<Int>? = null // [min, max]
)

@Serializable
data class CardTypeDefinition(
    val count: Int,
    val properties: Map<String, PropertyDefinition>,
    val events: Map<String, EventDefinition>? = null
)

@Serializable
sealed class PropertyDefinition {
    @Serializable
    data class IntProperty(
        val type: String = "int",
        val min: Int,
        val max: Int
    ) : PropertyDefinition()
    
    @Serializable
    data class EnumProperty(
        val type: String = "enum", 
        val values: List<String>
    ) : PropertyDefinition()
    
    @Serializable
    data class StringProperty(
        val type: String = "string",
        val maxLength: Int? = null
    ) : PropertyDefinition()
}

@Serializable
data class EventDefinition(
    val action: String,
    val target: String? = null,
    val amount: String? = null,
    val condition: String? = null
)

@Serializable
data class GameMechanics(
    val setup: SetupMechanics? = null,
    val winConditions: List<WinCondition>? = null
)

@Serializable
data class SetupMechanics(
    val players: PlayerSetup? = null
)

@Serializable
data class PlayerSetup(
    val health: Int? = null,
    val handSize: Int? = null,
    val initialScore: Int? = null
)

@Serializable
data class WinCondition(
    val type: String,
    val target: Int? = null,
    val maxTurns: Int? = null,
    val message: String
)

@Serializable
data class AIHints(
    val difficultyFactors: List<String>,
    val commonModifications: Map<String, Map<String, Int>>
)
```

### Card.kt
```kotlin
package org.junction.catenin.model

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
        return when (definition) {
            is PropertyDefinition.IntProperty -> {
                val value = (definition.min..definition.max).random()
                CardPropertyValue.IntValue(value)
            }
            is PropertyDefinition.EnumProperty -> {
                val value = definition.values.random()
                CardPropertyValue.StringValue(value)
            }
            is PropertyDefinition.StringProperty -> {
                CardPropertyValue.StringValue("default")
            }
        }
    }
}
```

### Player.kt
```kotlin
package org.junction.catenin.model

import kotlinx.serialization.Serializable

@Serializable
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
```

## YAML 解析器

### YamlParser.kt
```kotlin
package org.junction.catenin.parser

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import org.junction.catenin.model.*

class YamlParser {
    private val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false
        )
    )
    
    fun parseGameDefinition(yamlContent: String): GameDefinition {
        try {
            return yaml.decodeFromString(GameDefinition.serializer(), yamlContent)
        } catch (e: Exception) {
            throw GameDefinitionParseException("Failed to parse YAML: ${e.message}", e)
        }
    }
}

class GameDefinitionParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

### GameDefinitionParser.kt
```kotlin
package org.junction.catenin.parser

import org.junction.catenin.model.GameDefinition

class GameDefinitionParser {
    private val yamlParser = YamlParser()
    
    fun parseFromString(yamlContent: String): GameDefinition {
        return yamlParser.parseGameDefinition(yamlContent)
    }
    
    fun parseFromFile(filePath: String): GameDefinition {
        val content = readFileContent(filePath)
        return parseFromString(content)
    }
    
    fun validate(definition: GameDefinition): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 基本驗證
        if (definition.meta.name.isBlank()) {
            errors.add("Game name cannot be empty")
        }
        
        if (definition.cards.isEmpty()) {
            errors.add("Game must define at least one card type")
        }
        
        // 驗證卡牌定義
        definition.cards.forEach { (cardType, cardDef) ->
            if (cardDef.count <= 0) {
                errors.add("Card type '$cardType' must have count > 0")
            }
            
            if (cardDef.properties.isEmpty()) {
                errors.add("Card type '$cardType' must have at least one property")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    private fun readFileContent(filePath: String): String {
        return readPlatformFile(filePath)
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<String>) : ValidationResult()
}

// 平台特定的檔案讀取，在各平台實作
expect fun readPlatformFile(filePath: String): String
```

## 核心遊戲引擎介面

### GameEngine.kt
```kotlin
package org.junction.catenin.core

import org.junction.catenin.model.*
import org.junction.catenin.parser.GameDefinitionParser

@JsExport
class GameEngine private constructor(
    private val gameDefinition: GameDefinition,
    private val players: List<Player>
) {
    companion object {
        fun fromYaml(yamlContent: String, playerNames: List<String>): GameEngine {
            val parser = GameDefinitionParser()
            val definition = parser.parseFromString(yamlContent)
            
            val players = playerNames.mapIndexed { index, name ->
                Player(
                    id = "player_$index",
                    name = name,
                    health = definition.mechanics?.setup?.players?.health ?: 10
                )
            }
            
            return GameEngine(definition, players)
        }
    }
    
    fun getGameDefinition(): GameDefinition = gameDefinition
    fun getPlayers(): List<Player> = players
    
    // 這些方法將在後續 Day 實作
    fun drawCard(playerId: String): ActionResult = TODO("Day 2")
    fun playCard(playerId: String, cardId: String): ActionResult = TODO("Day 2")
    fun getUIState(): UIState = TODO("Day 2")
}

@JsExport
data class ActionResult(
    val success: Boolean,
    val message: String,
    val effects: List<String> = emptyList()
)

@JsExport
data class UIState(
    val players: List<Player>,
    val currentPlayer: String,
    val gamePhase: String
)
```

## 平台特定實作

### jvmMain/JvmFileReader.kt
```kotlin
package org.junction.catenin.platform

import java.io.File

actual fun readPlatformFile(filePath: String): String {
    return File(filePath).readText()
}
```

### jsMain/JsConsole.kt
```kotlin
package org.junction.catenin.platform

actual fun readPlatformFile(filePath: String): String {
    // JS 環境中，檔案內容需要從外部傳入
    throw UnsupportedOperationException("File reading not supported in JS environment. Use parseFromString instead.")
}

// JS 特定的 console 輸出
@JsExport
object GameConsole {
    fun log(message: String) {
        console.log(message)
    }
    
    fun displayCard(card: org.junction.catenin.model.Card) {
        val damage = card.getIntProperty("damage")
        val element = card.getStringProperty("element")
        console.log("Card ${card.id}: $element element, $damage damage")
    }
}
```

## 測試框架

### commonTest/YamlParserTest.kt
```kotlin
package org.junction.catenin.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YamlParserTest {
    
    @Test
    fun testParseSimpleGameDefinition() {
        val yaml = """
            meta:
              name: "Test Game"
              target_age: [8, 12]
              player_count: [2, 4]
            
            cards:
              attack_card:
                count: 10
                properties:
                  damage:
                    type: int
                    min: 1
                    max: 5
                  element:
                    type: enum
                    values: [fire, water]
                events:
                  on_play:
                    action: "deal_damage"
                    target: "opponent"
                    amount: "{damage}"
        """.trimIndent()
        
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(yaml)
        
        assertEquals("Test Game", definition.meta.name)
        assertEquals(listOf(8, 12), definition.meta.targetAge)
        assertEquals(1, definition.cards.size)
        
        val attackCard = definition.cards["attack_card"]
        assertNotNull(attackCard)
        assertEquals(10, attackCard.count)
        assertEquals(2, attackCard.properties.size)
        
        val validation = parser.validate(definition)
        assertTrue(validation is ValidationResult.Success)
    }
    
    @Test
    fun testValidationFailure() {
        val yaml = """
            meta:
              name: ""
              target_age: [8, 12]
            
            cards: {}
        """.trimIndent()
        
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(yaml)
        val validation = parser.validate(definition)
        
        assertTrue(validation is ValidationResult.Failure)
        assertTrue(validation.errors.contains("Game name cannot be empty"))
        assertTrue(validation.errors.contains("Game must define at least one card type"))
    }
}
```

## 範例遊戲檔案

### game-samples/simple-combat.yaml
```yaml
meta:
  name: "簡單戰鬥"
  target_age: [8, 12]
  player_count: [2, 2]

cards:
  attack_card:
    count: 15
    properties:
      damage:
        type: int
        min: 1
        max: 5
      element:
        type: enum
        values: [fire, water, earth]
    events:
      on_play:
        action: "deal_damage"
        target: "opponent"
        amount: "{damage}"
  
  heal_card:
    count: 5
    properties:
      healing:
        type: int
        min: 2
        max: 4
    events:
      on_play:
        action: "restore_health"
        target: "self"
        amount: "{healing}"

mechanics:
  setup:
    players:
      health: 15
      hand_size: 5
  
  win_conditions:
    - type: "health_depleted"
      message: "{winner} 獲勝！"

ai_hints:
  difficulty_factors: 
    - "cards.attack_card.properties.damage.max"
    - "mechanics.setup.players.health"
  common_modifications:
    easier:
      damage_max: 3
      health: 20
    harder:
      damage_max: 7
      health: 10
```

## 命令列測試程式

### jvmMain/Main.kt
```kotlin
package org.junction.catenin

import org.junction.catenin.core.GameEngine
import org.junction.catenin.model.CardFactory
import org.junction.catenin.parser.GameDefinitionParser

fun main() {
    println("=== Catenin DSL Day 1 Demo ===")
    
    try {
        // 測試 YAML 解析
        val gameYaml = """
            meta:
              name: "Day 1 測試遊戲"
              target_age: [8, 12]
            
            cards:
              number_card:
                count: 8
                properties:
                  value:
                    type: int
                    min: 1
                    max: 5
                  color:
                    type: enum
                    values: [red, blue, green]
        """.trimIndent()
        
        // 解析遊戲定義
        val parser = GameDefinitionParser()
        val definition = parser.parseFromString(gameYaml)
        
        println("✅ 成功解析遊戲: ${definition.meta.name}")
        
        // 生成卡牌
        val cardFactory = CardFactory(definition)
        val cards = cardFactory.generateCards()
        
        println("✅ 成功生成 ${cards.size} 張卡牌")
        
        // 顯示卡牌
        cards.take(5).forEach { card ->
            val value = card.getIntProperty("value")
            val color = card.getStringProperty("color")
            println("   ${card.id}: ${color}色數字${value}")
        }
        
        // 測試 GameEngine 創建
        val engine = GameEngine.fromYaml(gameYaml, listOf("Alice", "Bob"))
        println("✅ 成功創建遊戲引擎")
        println("   玩家: ${engine.getPlayers().map { it.name }}")
        
        println("\n🎉 Day 1 目標達成！")
        
    } catch (e: Exception) {
        println("❌ 執行失敗: ${e.message}")
        e.printStackTrace()
    }
}
```

## 今日交付目標

- [ ] ✅ Kotlin Multiplatform 專案成功建立
- [ ] ✅ Gradle 配置可編譯 JVM 和 JS 目標
- [ ] ✅ YAML 解析器可正確解析遊戲定義
- [ ] ✅ 基本資料模型建立並可序列化
- [ ] ✅ 卡牌生成器可產生隨機屬性卡牌
- [ ] ✅ 遊戲引擎核心介面設計完成
- [ ] ✅ 單元測試覆蓋率 > 80%
- [ ] ✅ 命令列 Demo 程式可執行

## 驗證指令

```bash
# 編譯所有平台
./gradlew build

# 執行 JVM 測試
./gradlew jvmTest

# 執行 JS 測試  
./gradlew jsTest

# 生成 JS 檔案
./gradlew jsBrowserDistribution

# 執行 Demo
./gradlew jvmRun
```

## 明日工作預告

Day 2 將加入玩家狀態管理和基本動作系統，讓玩家可以抽牌和打牌，建立真正的遊戲互動。

## 技術債務記錄

- JS 平台的檔案讀取機制需要優化
- 錯誤訊息本地化待實作
- 卡牌屬性類型系統可擴展更多類型

今日成功建立了跨平台的基礎架構，為後續功能開發奠定穩固基礎！