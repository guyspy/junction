# Day 1: YAML 解析器與基本卡牌顯示

## 工作目標
- 實作基本 YAML 解析功能
- 建立遊戲資料模型
- 顯示簡單卡牌資訊
- 完成第一個可運行的概念驗證

## 今日範圍

### YAML 格式定義
```yaml
# game1.yaml - 最簡單的數字卡遊戲
meta:
  name: "我的第一個遊戲"
  target_age: [6, 10]

cards:
  number_card:
    count: 10
    properties:
      value: {type: int, min: 1, max: 5}
      color: {type: enum, values: [red, blue]}
```

### 資料模型設計
```kotlin
// 遊戲定義模型
data class GameDefinition(
    val meta: GameMeta,
    val cards: Map<String, CardTypeDefinition>
)

data class GameMeta(
    val name: String,
    val targetAge: IntRange
)

data class CardTypeDefinition(
    val count: Int,
    val properties: Map<String, PropertyDefinition>
)

sealed class PropertyDefinition {
    data class IntProperty(val min: Int, val max: Int) : PropertyDefinition()
    data class EnumProperty(val values: List<String>) : PropertyDefinition()
}

// 執行時卡牌模型
data class Card(
    val id: String,
    val type: String,
    val properties: Map<String, Any>
)
```

### 核心功能實作

#### 1. YAML 解析器
```kotlin
class GameDefinitionParser {
    private val yaml = Yaml()
    
    fun parseFromFile(filePath: String): GameDefinition {
        val yamlContent = File(filePath).readText()
        return parseFromString(yamlContent)
    }
    
    fun parseFromString(yamlContent: String): GameDefinition {
        val yamlMap = yaml.load<Map<String, Any>>(yamlContent)
        
        val meta = parseMeta(yamlMap["meta"] as Map<String, Any>)
        val cards = parseCards(yamlMap["cards"] as Map<String, Any>)
        
        return GameDefinition(meta, cards)
    }
    
    private fun parseMeta(metaMap: Map<String, Any>): GameMeta {
        val name = metaMap["name"] as String
        val ageList = metaMap["target_age"] as List<Int>
        return GameMeta(name, ageList[0]..ageList[1])
    }
    
    private fun parseCards(cardsMap: Map<String, Any>): Map<String, CardTypeDefinition> {
        return cardsMap.mapValues { (_, cardData) ->
            parseCardType(cardData as Map<String, Any>)
        }
    }
}
```

#### 2. 卡牌生成器
```kotlin
class CardGenerator {
    private val random = Random()
    
    fun generateCards(definition: GameDefinition): List<Card> {
        val allCards = mutableListOf<Card>()
        
        definition.cards.forEach { (cardType, cardDef) ->
            repeat(cardDef.count) { index ->
                val card = createCard(cardType, cardDef, index)
                allCards.add(card)
            }
        }
        
        return allCards
    }
    
    private fun createCard(
        cardType: String, 
        definition: CardTypeDefinition, 
        index: Int
    ): Card {
        val properties = mutableMapOf<String, Any>()
        
        definition.properties.forEach { (propName, propDef) ->
            val value = generatePropertyValue(propDef)
            properties[propName] = value
        }
        
        return Card(
            id = "${cardType}_${index}",
            type = cardType,
            properties = properties
        )
    }
    
    private fun generatePropertyValue(definition: PropertyDefinition): Any {
        return when (definition) {
            is PropertyDefinition.IntProperty -> 
                random.nextInt(definition.min, definition.max + 1)
            is PropertyDefinition.EnumProperty -> 
                definition.values.random()
        }
    }
}
```

#### 3. 簡單顯示系統
```kotlin
class SimpleCardDisplay {
    
    fun displayCards(cards: List<Card>) {
        println("=== 遊戲卡牌列表 ===")
        cards.forEach { card ->
            displayCard(card)
        }
    }
    
    private fun displayCard(card: Card) {
        val value = card.properties["value"]
        val color = card.properties["color"]
        
        println("卡牌 ${card.id}: ${color}色數字${value}")
    }
    
    fun displayGameInfo(definition: GameDefinition) {
        println("遊戲名稱: ${definition.meta.name}")
        println("適合年齡: ${definition.meta.targetAge}")
        println("卡牌類型: ${definition.cards.keys.joinToString(", ")}")
        println()
    }
}
```

## 測試策略

### 單元測試
```kotlin
class GameDefinitionParserTest {
    
    @Test
    fun `should parse simple game definition`() {
        val yaml = """
            meta:
              name: "測試遊戲"
              target_age: [6, 10]
            
            cards:
              number_card:
                count: 5
                properties:
                  value: {type: int, min: 1, max: 3}
                  color: {type: enum, values: [red, blue]}
        """.trimIndent()
        
        val parser = GameDefinitionParser()
        val game = parser.parseFromString(yaml)
        
        assertThat(game.meta.name).isEqualTo("測試遊戲")
        assertThat(game.meta.targetAge).isEqualTo(6..10)
        assertThat(game.cards).hasSize(1)
        assertThat(game.cards["number_card"]?.count).isEqualTo(5)
    }
}

class CardGeneratorTest {
    
    @Test
    fun `should generate correct number of cards`() {
        val definition = GameDefinition(
            meta = GameMeta("測試", 6..10),
            cards = mapOf(
                "number_card" to CardTypeDefinition(
                    count = 3,
                    properties = mapOf(
                        "value" to PropertyDefinition.IntProperty(1, 5),
                        "color" to PropertyDefinition.EnumProperty(listOf("red", "blue"))
                    )
                )
            )
        )
        
        val generator = CardGenerator()
        val cards = generator.generateCards(definition)
        
        assertThat(cards).hasSize(3)
        cards.forEach { card ->
            assertThat(card.type).isEqualTo("number_card")
            assertThat(card.properties["value"] as Int).isBetween(1, 5)
            assertThat(card.properties["color"]).isIn("red", "blue")
        }
    }
}
```

### 整合測試
```kotlin
class Day1IntegrationTest {
    
    @Test
    fun `complete day 1 workflow`() {
        // 1. 建立測試 YAML 檔案
        val testYaml = createTestGameFile()
        
        // 2. 解析遊戲定義
        val parser = GameDefinitionParser()
        val gameDefinition = parser.parseFromFile(testYaml)
        
        // 3. 生成卡牌
        val generator = CardGenerator()
        val cards = generator.generateCards(gameDefinition)
        
        // 4. 顯示結果
        val display = SimpleCardDisplay()
        display.displayGameInfo(gameDefinition)
        display.displayCards(cards)
        
        // 5. 驗證結果
        assertThat(cards).isNotEmpty()
        println("Day 1 功能驗證成功！")
    }
    
    private fun createTestGameFile(): String {
        val content = """
            meta:
              name: "Day 1 測試遊戲"
              target_age: [7, 12]
            
            cards:
              number_card:
                count: 8
                properties:
                  value: {type: int, min: 1, max: 4}
                  color: {type: enum, values: [red, blue, green]}
        """.trimIndent()
        
        val file = File.createTempFile("test_game_day1", ".yaml")
        file.writeText(content)
        return file.absolutePath
    }
}
```

## 執行範例

### 建立測試遊戲檔案
```yaml
# test_game.yaml
meta:
  name: "數字顏色卡"
  target_age: [5, 8]

cards:
  number_card:
    count: 12
    properties:
      value: {type: int, min: 1, max: 6}
      color: {type: enum, values: [red, blue, green, yellow]}
```

### 主程式
```kotlin
fun main() {
    println("=== Cadherin DSL Day 1 Demo ===")
    
    try {
        // 解析遊戲定義
        val parser = GameDefinitionParser()
        val game = parser.parseFromFile("test_game.yaml")
        
        // 生成卡牌
        val generator = CardGenerator()
        val cards = generator.generateCards(game)
        
        // 顯示結果
        val display = SimpleCardDisplay()
        display.displayGameInfo(game)
        display.displayCards(cards)
        
        println("\n✅ Day 1 目標達成！")
        println("- ✅ YAML 解析成功")
        println("- ✅ 卡牌生成成功")
        println("- ✅ 基本顯示功能完成")
        
    } catch (e: Exception) {
        println("❌ 執行失敗: ${e.message}")
        e.printStackTrace()
    }
}
```

## 今日交付成果
- [ ] ✅ YAML 解析器實作完成
- [ ] ✅ 基本資料模型建立
- [ ] ✅ 卡牌生成器功能
- [ ] ✅ 簡單顯示系統
- [ ] ✅ 單元測試覆蓋 > 80%
- [ ] ✅ 整合測試驗證流程
- [ ] ✅ 可執行的 Demo 程式

## 明日工作預告
Day 2 將加入玩家概念和基本動作（抽牌、打牌），讓卡牌可以在玩家間移動。

## 技術債務記錄
- 錯誤處理尚未完善（明日加強）
- 卡牌顯示格式較簡單（可後續美化）
- 缺少 YAML 格式驗證（Day 9 加入）

今日專注於建立基礎架構和驗證核心概念，為後續開發奠定穩固基礎。