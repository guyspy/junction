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
        jvmToolchain(21)
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    
    js(IR) {
        binaries.library()
        browser()
        nodejs {
            testTask {
                useMocha {
                    timeout = "5s"
                }
            }
        }
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
                // JS specific dependencies if needed
            }
        }
    }
}

// Configure TypeScript definition generation
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.typescript.TypeScriptValidationTask> {
    enabled = false
}

// Add jvmRun task for CLI demo
tasks.register<JavaExec>("jvmRun") {
    group = "application"
    description = "Run the JVM command-line demo"
    dependsOn(tasks.getByName("jvmMainClasses"))
    val jvmTarget = kotlin.targets.getByName("jvm")
    val mainCompilation = jvmTarget.compilations.getByName("main")
    classpath = mainCompilation.output.allOutputs + mainCompilation.runtimeDependencyFiles!!
    mainClass.set("org.junction.cadherin.MainKt")
}