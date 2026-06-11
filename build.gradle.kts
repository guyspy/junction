plugins {
    // Kotlin plugins for catenin
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false

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

// Fix webpack version mismatch in npm workspaces for Kotlin/JS browser tests
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    extensions.configure<org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension> {
        override("webpack", "5.101.3")
    }
}