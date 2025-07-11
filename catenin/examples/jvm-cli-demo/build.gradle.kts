plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":catenin"))
    // For external usage: implementation(libs.catenin)
}

application {
    mainClass.set("org.junction.catenin.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}