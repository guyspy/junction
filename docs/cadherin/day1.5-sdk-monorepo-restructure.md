# Day 1.5: SDK Monorepo 重構計劃

## 🎯 目標

將 Cadherin 重構為標準的 SDK + Examples monorepo 架構，提供清晰的核心庫與示例項目分離，支援多平台獨立項目引用。

## 📁 新的 Monorepo 結構

```
junction/
├── cadherin/                        # 核心 SDK (移除 -sdk 後綴)
│   ├── build.gradle.kts            # SDK 專用構建腳本
│   ├── gradle.properties
│   ├── src/
│   │   ├── commonMain/              # 跨平台核心代碼
│   │   ├── commonTest/
│   │   ├── jvmMain/                 # JVM 平台特定代碼
│   │   └── jsMain/                  # JS 平台特定代碼
│   └── README.md                    # SDK 文檔
├── examples/
│   ├── jvm-cli-demo/               # JVM 命令行示例
│   │   ├── build.gradle.kts        # 獨立項目配置
│   │   ├── src/main/kotlin/
│   │   └── README.md
│   ├── js-browser-demo/            # JS 瀏覽器示例
│   │   ├── build.gradle.kts        # 獨立項目配置
│   │   ├── src/main/kotlin/
│   │   ├── src/main/resources/index.html
│   │   └── README.md
│   └── js-node-demo/               # JS Node.js 示例
│       ├── build.gradle.kts
│       ├── src/main/kotlin/
│       └── README.md
├── game-samples/                    # YAML 遊戲範例
│   ├── number-war.yaml
│   └── simple-combat.yaml
├── build.gradle.kts                # 根項目構建腳本
├── settings.gradle.kts             # Monorepo 項目設定
├── gradle.properties              # 全局 Gradle 設定
└── docs/                           # 統一文檔
```

## 🏗️ SDK 設計規範

### 核心庫 (cadherin)
- **群組 ID**: `org.junction.cadherin`
- **Artifact ID**: `cadherin`
- **發布格式**: Kotlin Multiplatform (JVM + JS)
- **版本管理**: 語義化版本號 (如 `1.0.0`)

### Maven 座標
```kotlin
dependencies {
    implementation("org.junction.cadherin:cadherin:1.0.0")
}
```

## 📦 Example 項目設計

### jvm-cli-demo
**目標**: 展示 JVM 平台命令行用法

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation("org.junction.cadherin:cadherin:1.0.0")
}

application {
    mainClass.set("com.example.MainKt")
}
```

**功能展示**:
- 命令行遊戲執行
- YAML 解析與驗證
- 完整遊戲流程演示
- 文件系統 I/O 操作

### js-browser-demo
**目標**: 展示瀏覽器中的網頁遊戲用法

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform")
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
        }
    }
}

dependencies {
    implementation("org.junction.cadherin:cadherin:1.0.0")
}
```

**功能展示**:
- 網頁版卡牌遊戲 UI
- DOM 操作與事件處理
- TypeScript 定義使用
- 瀏覽器本地存儲

### js-node-demo
**目標**: 展示 Node.js 服務器端用法

```kotlin
// build.gradle.kts
kotlin {
    js(IR) {
        nodejs {
            binaries.executable()
        }
    }
}
```

**功能展示**:
- 服務器端遊戲邏輯
- 文件系統操作
- HTTP API 實現
- 多人遊戲房間管理

## 🔧 Monorepo 配置

### 根項目 settings.gradle.kts
```kotlin
rootProject.name = "junction"

include(":cadherin")
include(":examples:jvm-cli-demo")
include(":examples:js-browser-demo") 
include(":examples:js-node-demo")
```

### 版本目錄 (libs.versions.toml)
```toml
[versions]
cadherin = "1.0.0"
kotlin = "2.1.21"
kotlinx-serialization = "1.7.3"
kotlinx-coroutines = "1.9.0"

[libraries]
cadherin = { module = "org.junction.cadherin:cadherin", version.ref = "cadherin" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

### Example 項目依賴策略
```kotlin
// 開發期間: 項目內依賴 (faster builds)
dependencies {
    implementation(project(":cadherin"))
}

// 發布驗證: Maven 依賴 (real-world testing)
dependencies {
    implementation(libs.cadherin)
}
```

## 🚀 發布與分發策略

### SDK 發布流程
1. **本地發布**: `./gradlew :cadherin:publishToMavenLocal`
2. **版本標籤**: Git tag + semantic versioning
3. **Maven Central**: 自動發布到公開倉庫
4. **TypeScript 定義**: 自動生成並發布 `.d.ts` 文件

### Examples 驗證流程
1. **開發測試**: 使用 `project(":cadherin")` 依賴
2. **集成測試**: 切換到 Maven 依賴進行驗證
3. **CI/CD**: 兩種依賴方式都要通過測試

## 📚 文檔架構

### SDK 文檔 (cadherin/README.md)
- 快速開始指南
- API 參考文檔
- 平台特定說明
- 遷移指南

### Example 文檔
- 每個 example 獨立的 README.md
- 運行步驟與先決條件
- 代碼結構解析
- 學習重點說明

### 統一文檔 (docs/)
- 整體架構說明
- 開發者指南
- 發布說明
- 貢獻指南

## 🔄 實施步驟

### Phase 1: 結構重構 (1-2 天)
- [ ] 重新組織目錄結構
- [ ] 設置根項目 Gradle 配置
- [ ] 更新 Cadherin 核心庫的 build.gradle.kts
- [ ] 配置版本目錄 (libs.versions.toml)

### Phase 2: 創建 Examples (2-3 天)
- [ ] 創建 jvm-cli-demo 獨立項目
- [ ] 創建 js-browser-demo 獨立項目
- [ ] 創建 js-node-demo 獨立項目
- [ ] 每個 example 添加完整的 README

### Phase 3: 發布配置 (1 天)
- [ ] 配置 Maven 發布腳本
- [ ] 設置版本管理策略
- [ ] 配置 CI/CD 流水線

### Phase 4: 文檔與測試 (1 天)
- [ ] 更新所有文檔
- [ ] 驗證所有 examples 可獨立運行
- [ ] 測試發布流程

## 🎯 預期效果

### 對開發者的好處
1. **清晰分離**: 核心庫與示例代碼完全分離
2. **易於引用**: 標準 Maven 座標引用方式
3. **多平台支援**: JVM 與 JS 平台完整支援
4. **學習友好**: 豐富的獨立示例項目

### 對維護者的好處
1. **版本管理**: 統一的版本號與發布流程
2. **測試隔離**: Examples 可驗證真實使用場景
3. **文檔同步**: 代碼與文檔緊密結合
4. **CI/CD 簡化**: 清晰的構建與測試流程

## 🚧 潛在挑戰

### 技術挑戰
- **依賴管理**: 開發期間與發布後的依賴切換
- **版本同步**: 確保 examples 與 SDK 版本兼容
- **構建效能**: Monorepo 的構建時間優化

### 解決方案
- 使用 Gradle 的 `composite builds` 功能
- CI/CD 中同時測試兩種依賴方式
- 合理使用 Gradle 的 `configuration cache` 和並行構建

## 📈 成功指標

- [ ] 所有 examples 可以獨立克隆並運行
- [ ] SDK 可以成功發布到 Maven 倉庫
- [ ] 外部項目可以順利引用 SDK
- [ ] 文檔完整且易於理解
- [ ] CI/CD 流程穩定運行

---

**下一步**: 等待確認後開始 Phase 1 的目錄結構重構工作。