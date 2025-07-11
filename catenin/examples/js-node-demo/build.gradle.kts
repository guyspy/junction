plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js(IR) {
        nodejs {
            binaries.executable()
        }
    }
    
    sourceSets {
        jsMain.dependencies {
            implementation(project(":catenin"))
            // For external usage: implementation(libs.catenin)
        }
    }
}