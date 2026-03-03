plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlinx.kover") version "0.9.7"
    id("maven-publish")
}

group = "org.junction.catenin"
version = "1.0.0"

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
        useEsModules()
        generateTypeScriptDefinitions()
    }
    
    sourceSets {
        all {
            languageSettings.optIn("kotlin.js.ExperimentalJsExport")
        }
        
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

// Maven publishing configuration
publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name = "Catenin Game Engine"
                description = "Kotlin Multiplatform game engine for 2D card-based educational games"
                url = "https://github.com/junction/catenin"
                
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
            }
        }
    }
}

// Note: Examples are separate from the library and not included in published artifacts
// To run examples, use them as standalone projects that depend on the published library

// Kover configuration for test coverage (JVM only)
kover {
    reports {
        filters {
            excludes {
                // Exclude examples from coverage
                packages("org.junction.catenin.examples.*")
            }
        }
        
        total {
            html {
                onCheck = true
                htmlDir = layout.buildDirectory.dir("reports/kover/html")
            }
            xml {
                onCheck = true
                xmlFile = layout.buildDirectory.file("reports/kover/result.xml")
            }
        }
    }
}

// Create npm package for local development
tasks.register<Copy>("createNpmPackage") {
    dependsOn("jsBrowserDevelopmentLibraryDistribution")
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    from(layout.buildDirectory.dir("dist/js/developmentLibrary")) {
        exclude("package.json") // Exclude the generated package.json
    }
    into(layout.buildDirectory.dir("npm-package"))
    
    // Copy and rename our custom npm package.json
    from("npm-package.json") {
        rename("npm-package.json", "package.json")
    }
    
    doLast {
        println("✅ NPM package created at: ${layout.buildDirectory.dir("npm-package").get().asFile.absolutePath}")
        println("📦 To install in other projects: npm install ${layout.buildDirectory.dir("npm-package").get().asFile.absolutePath}")
    }
}

// Pack the npm package for distribution
tasks.register<Exec>("packNpmPackage") {
    dependsOn("createNpmPackage")
    
    workingDir = layout.buildDirectory.dir("npm-package").get().asFile
    commandLine("npm", "pack")
    
    doLast {
        val packageFile = layout.buildDirectory.dir("npm-package").get().asFile.resolve("junction-catenin-1.0.0.tgz")
        println("📦 NPM package packed: ${packageFile.absolutePath}")
        println("💡 Install with: npm install ${packageFile.absolutePath}")
    }
}