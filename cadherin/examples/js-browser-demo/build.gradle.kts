plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
            webpackTask {
                outputFileName = "cadherin-browser-demo.js"
            }
        }
    }
    
    sourceSets {
        jsMain.dependencies {
            implementation(project(":cadherin"))
            // For external usage: implementation(libs.cadherin)
        }
    }
}