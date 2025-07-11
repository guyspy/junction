// Pure HTML demo - no build needed
// This demo shows how to use the Catenin library as a JavaScript import

// Task to prepare the HTML demo
tasks.register("serve") {
    dependsOn(":catenin:jsBrowserDevelopmentLibraryDistribution")
    doLast {
        val libDir = project(":catenin").layout.buildDirectory.dir("dist/js/developmentLibrary").get().asFile
        val demoFile = file("index.html")
        println("HTML Demo ready!")
        println("Library files available at: ${libDir.absolutePath}")
        println("Open in browser: ${demoFile.absolutePath}")
        println("Note: Update the import path in index.html to point to the library directory")
    }
}