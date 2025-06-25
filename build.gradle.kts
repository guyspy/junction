plugins {
    // Kotlin plugins for catenin
    kotlin("multiplatform") version "2.1.21" apply false
    kotlin("jvm") version "2.1.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.21" apply false
    
    // Java plugins for future services (quarkus, etc)
    // java plugin will be applied per-project as needed
}

allprojects {
    group = "org.junction"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

// Service-specific configurations
configure(subprojects.filter { it.name == "catenin" }) {
    group = "org.junction.catenin"
}