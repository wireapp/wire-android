package scripts

import scripts.Variants_gradle.BuildTypes
import scripts.Variants_gradle.ProductFlavors

private object Default {
    const val BUILD_TYPE = BuildTypes.DEBUG
    const val BUILD_FLAVOR = ProductFlavors.DEV
    val VARIANT = "${BUILD_FLAVOR.capitalize()}${BUILD_TYPE.capitalize()}"
    val CLASS_PATH_VARIANT = "$BUILD_FLAVOR${BUILD_TYPE.capitalize()}"
}

subprojects {
    apply(plugin = "jacoco")

    val jacocoReport by tasks.registering(JacocoReport::class) {
        group = "Reporting"
        description = "Generate Jacoco coverage for all module unit tests on debug builds. "
        dependsOn(":app:test${Default.VARIANT}UnitTest")

        reports {
            xml.isEnabled = true
            html.isEnabled = true
            html.destination = file("${buildDir}/jacoco/html")
        }

        classDirectories.setFrom(
            fileTree(project.buildDir) {
                include(
                    "**/classes/**/main/**",
                    "**/intermediates/classes/${Default.CLASS_PATH_VARIANT}/**",
                    "**/intermediates/javac/${Default.CLASS_PATH_VARIANT}/*/classes/**", // Android Gradle Plugin 3.2.x support.
                    "**/tmp/kotlin-classes/${Default.CLASS_PATH_VARIANT}/**"
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

        sourceDirectories.setFrom(
            fileTree(project.projectDir) {
                include(
                    "src/main/java/**",
                    "src/main/kotlin/**",
                    "src/${Default.CLASS_PATH_VARIANT}/java/**",
                    "src/${Default.CLASS_PATH_VARIANT}/kotlin/**"
                )
            }
        )

        executionData.setFrom(
            fileTree(project.buildDir) {
                include(
                    "**/*.exec", "**/*.ec"
                )
            }
        )
    }
}