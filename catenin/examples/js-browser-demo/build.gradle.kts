plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
            webpackTask {
                outputFileName = "catenin-browser-demo.js"
            }
        }
    }
    
    sourceSets {
        jsMain.dependencies {
            implementation(project(":catenin"))
            // For external usage: implementation(libs.catenin)
        }
    }
}