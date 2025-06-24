plugins {
    kotlin("multiplatform") version "2.1.21" apply false
    kotlin("jvm") version "2.1.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.21" apply false
}

allprojects {
    group = "org.junction.cadherin"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}