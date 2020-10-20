package scripts

plugins {
    id("com.android.application") apply false
    id("scripts.coverage")
    id("io.gitlab.arturbosch.detekt")
}

// Lint Configuration
android {
    lintOptions {
        isQuiet = true
        isAbortOnError = false
        isIgnoreWarnings = true
        disable("InvalidPackage")           //Some libraries have issues with this.
        disable("OldTargetApi")             //Lint gives this warning related to SDK Beta.
        disable("IconDensities")            //For testing purpose. This is safe to remove.
        disable("IconMissingDensityFolder") //For testing purpose. This is safe to remove.
    }
}

// Detekt Configuration
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
        html.enabled = true
        html.destination = file(outputFile)
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