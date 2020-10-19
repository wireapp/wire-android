package scripts

plugins {
    id("io.gitlab.arturbosch.detekt") apply false
}

val detektAll by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    group = "Quality"
    description = "Runs a detekt code analysis ruleset on the Wire Android codebase "
    parallel = true
    buildUponDefaultConfig = true

    val outputFile = "${project.buildDir}/staticAnalysis/index.html"

    setSource(files(rootProject.projectDir))
    config.setFrom("${project.rootDir}/config/detekt/detekt.yml")

    include("**/*.kt")
    exclude("**/*.kts", "*/build/*", "/buildSrc")

    reports {
        xml.enabled = true
        xml.destination = file(outputFile)
        html.enabled = true
        txt.enabled = false
    }

    val reportFile = "Static Analysis Report: $outputFile \n"
    doFirst { println(reportFile) }
    doLast { println(reportFile) }
}

tasks.register("staticCodeAnalysis") {
    description = "Analyses code within the Wire Android codebase"
    dependsOn(detektAll)
}
