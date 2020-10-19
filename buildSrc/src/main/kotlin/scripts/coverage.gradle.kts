package scripts

import scripts.Variants_gradle.Default

apply(plugin = "jacoco")

val jacocoReport by tasks.registering(JacocoReport::class) {
    group = "Quality"
    description = "Reports code coverage on tests within the Wire Android codebase"
    dependsOn("test${Default.BUILD_VARIANT}UnitTest")

    val outputDir = "$buildDir/jacoco/html"
    val classPathBuildVariant = "${Default.BUILD_FLAVOR}${Default.BUILD_TYPE.capitalize()}"

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
