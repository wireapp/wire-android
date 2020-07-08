package scripts

import BuildPlugins
import scripts.Variants_gradle.BuildTypes
import scripts.Variants_gradle.ProductFlavors
import java.util.*

private object Default {
    const val BUILD_TYPE = BuildTypes.DEBUG
    const val BUILD_FLAVOR = ProductFlavors.DEV

    val VARIANT = "${BUILD_FLAVOR.capitalize()}${BUILD_TYPE.capitalize()}"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = BuildPlugins.Versions.gradleVersion
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register("staticCodeAnalysis") {
    description = "Analyses code within the Wire Android codebase"
    dependsOn("detektAll")
}

tasks.register("runUnitTests") {
    description = "Runs all Unit Tests."
    dependsOn(":app:test${Default.VARIANT}UnitTest")
}

tasks.register("runAcceptanceTests") {
    description = "Runs all Acceptance Tests in the connected device."
    dependsOn(":app:connected${Default.VARIANT}AndroidTest")
}

tasks.register("compileApp") {
    description = "Compiles the Wire Android Client."
    dependsOn(":app:assemble${Default.VARIANT}")
}

tasks.register("runApp", Exec::class) {
    val compileAppTask = "compileApp"
    val installAppTask = ":app:install${Default.VARIANT}"

    description = "Compiles and runs the Wire Android Client in the connected device."
    dependsOn(compileAppTask, installAppTask)
    tasks.findByName(installAppTask)?.mustRunAfter(compileAppTask)

    val localProperties = File(project.rootDir, "local.properties")
    if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        val sdkDir = properties["sdk.dir"]
        val adb = "${sdkDir}/platform-tools/adb"

        val applicationPackage = "com.wire.android.${Default.BUILD_FLAVOR}.${Default.BUILD_TYPE}"
        val launchActivity = "com.wire.android.feature.welcome.WelcomeActivity"

        commandLine(adb, "shell", "am", "start", "-n", "${applicationPackage}/${launchActivity}")
    }
}