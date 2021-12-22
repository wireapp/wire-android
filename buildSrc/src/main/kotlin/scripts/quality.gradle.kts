package scripts

import io.gitlab.arturbosch.detekt.Detekt
import scripts.Variants_gradle.Default

plugins {
    id("com.android.application") apply false
    id("jacoco")
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
val detektAll by tasks.registering(Detekt::class) {
    group = "Quality"
    description = "Runs a detekt code analysis ruleset on the Wire Android codebase "
    parallel = true
    buildUponDefaultConfig = true

    val outputFile = "${project.buildDir}/staticAnalysis/index.html"

    setSource(files(project.projectDir))
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

// Jacoco Configuration
val jacocoReport by tasks.registering(JacocoReport::class) {
    group = "Quality"
    description = "Reports code coverage on tests within the Wire Android codebase"
    dependsOn("test${Default.BUILD_VARIANT}UnitTest")

    val outputDir = "$buildDir/jacoco/html"
    val classPathBuildVariant =  "${Default.BUILD_FLAVOR}${Default.BUILD_TYPE.capitalize()}"

    reports {
        xml.isEnabled = true
        html.isEnabled = true
        html.destination = file(outputDir)
    }

    classDirectories.setFrom(
        fileTree(project.buildDir) {
            include(
                "**/classes/**/main/**",
                "**/intermediates/classes/$classPathBuildVariant/**",
                "**/intermediates/javac/$classPathBuildVariant/*/classes/**",
                "**/tmp/kotlin-classes/$classPathBuildVariant/**"
            )
            exclude(
                "**/R.class",
                "**/R\$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/Manifest$*.class",
                "**/*Test*.*",
                "**/Injector.*",
                "android/**/*.*",
                "**/*\$Lambda$*.*",
                "**/*\$inlined$*.*",
                "**/di/*.*",
                "**/*Database.*",
                "**/*Response.*",
                "**/*Application.*",
                "**/*Entity.*"
            )
        }
    )

    sourceDirectories.setFrom(fileTree(project.projectDir) {
        include("src/main/java/**", "src/main/kotlin/**") })

    executionData.setFrom(fileTree(project.buildDir) {
        include("**/*.exec", "**/*.ec") })

    doLast { println("Report file: $outputDir/index.html") }
}

tasks.register("testCoverage") {
    group = "Quality"
    description = "Reports code coverage on tests within the Wire Android codebase."
    dependsOn(jacocoReport)
}
