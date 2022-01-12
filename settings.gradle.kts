//Include all the existent modules in the project
rootDir
    .walk()
    .maxDepth(1)
    .filter {
        it.name != "buildSrc" && it.name != "kalium" && it.isDirectory &&
                file("${it.absolutePath}/build.gradle.kts").exists()
    }
    .forEach {
        include(":${it.name}")
    }

includeBuild("kalium") {
    // This dependency substitution should not be done on release mode once the Kalium library has been published to Maven repo
    dependencySubstitution {
        substitute(module("com.wire.kalium:kalium-logic")).using(project(":logic"))
    }
}
