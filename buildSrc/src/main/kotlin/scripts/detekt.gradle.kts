package scripts

plugins {
    id("io.gitlab.arturbosch.detekt") apply false
}

val detektAll by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    description = "Runs over whole code base without the starting overhead for each module."
    parallel = true
    buildUponDefaultConfig = true
    setSource(files(rootProject.projectDir))
    config.setFrom(project.rootDir.resolve("config/detekt/detekt.yml"))
    include("**/*.kt")
    exclude("**/*.kts", "*/build/*", "/buildSrc")
    reports {
        xml.enabled = true
        html.enabled = true
        txt.enabled = false
    }
}