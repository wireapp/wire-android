package scripts

import BuildPlugins
import scripts.Variants_gradle.Default
import java.util.Properties

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = BuildPlugins.Versions.gradleVersion
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register("runUnitTests") {
    description = "Runs all Unit Tests."
    dependsOn(":app:test${Default.BUILD_VARIANT}UnitTest")
}

tasks.register("runAcceptanceTests") {
    description = "Runs all Acceptance Tests in the connected device."
    dependsOn(":app:connected${Default.BUILD_VARIANT}AndroidTest")
}

tasks.register("assembleApp") {
    description = "assemble the Wire Android Client."
    dependsOn(":app:assemble${Default.BUILD_VARIANT}")
}

tasks.register("compileApp") {
    description = "compiles the Wire Android Client source."
    dependsOn(":app:compile${Default.BUILD_VARIANT}Sources")
}

tasks.register("runApp", Exec::class) {
    val assembleAppTask = "assembleApp"
    val installAppTask = ":app:install${Default.BUILD_VARIANT}"

    description = "assembles and runs the Wire Android Client in the connected device."
    dependsOn(assembleAppTask, installAppTask)
    tasks.findByName(installAppTask)?.mustRunAfter(assembleAppTask)

    val localProperties = File(project.rootDir, "local.properties")
    if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        val sdkDir = properties["sdk.dir"]
        val adb = "${sdkDir}/platform-tools/adb"

        val applicationPackage = "com.wire.android.${Default.BUILD_FLAVOR}.${Default.BUILD_TYPE}"
        val launchActivity = "com.wire.android.feature.launch.ui.LauncherActivity"

        commandLine(adb, "shell", "am", "start", "-n", "${applicationPackage}/${launchActivity}")
    }
}
