package scripts

plugins {
    id("io.gitlab.arturbosch.detekt") apply false
}

val detektAll by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    group = "Quality"
    description = "Runs a detekt code analysis ruleset on the Wire Android codebase "
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

tasks.register("staticCodeAnalysis") {
    description = "Analyses code within the Wire Android codebase"
    dependsOn(detektAll)
}
