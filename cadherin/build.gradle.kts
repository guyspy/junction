plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

group = "org.junction"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    
    jvm {
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    
    js(IR) {
        binaries.library()
        browser()
        nodejs()
        generateTypeScriptDefinitions()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kaml)
            implementation(libs.kotlinx.coroutines.core)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            implementation(libs.assertj.core)
        }
    }
}

// Configure TypeScript definition generation
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.typescript.TypeScriptValidationTask> {
    enabled = false
}

// Ensure TypeScript definitions are generated  
tasks.matching { it.name.endsWith("GenerateTypeScriptDefinitions") }.configureEach {
    dependsOn(tasks.matching { it.name.contains("compileKotlinJs") })
}

// Note: Examples are separate from the library and not included in published artifacts
// To run examples, use them as standalone projects that depend on the published library