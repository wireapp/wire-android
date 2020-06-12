package scripts

import scripts.Variants_gradle.*
import java.util.*

private object Default {
    val BUILD_TYPE = BuildTypes.DEBUG.capitalize()
    val BUILD_FLAVOR = ProductFlavors.DEV.capitalize()
}

tasks.register("clean", Delete::class){
    delete(rootProject.buildDir)
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = BuildPlugins.Versions.gradleVersion
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register("runUnitTests") {
    description = "Runs all Unit Tests."
    dependsOn(":app:test${Default.BUILD_FLAVOR}${Default.BUILD_TYPE}UnitTest")
}

tasks.register("runAcceptanceTests") {
    description = "Runs all Acceptance Tests in the connected device."
    dependsOn(":app:connected${Default.BUILD_FLAVOR}${Default.BUILD_TYPE}AndroidTest")
}

tasks.register("compileApp") {
    description = "Compiles the Wire Android Client."
    dependsOn(":app:assemble${Default.BUILD_FLAVOR}${Default.BUILD_TYPE}")
}

tasks.register("runApp", Exec::class) {
    val compileAppTask = "compileApp"
    val installAppTask = ":app:install${Default.BUILD_FLAVOR}${Default.BUILD_TYPE}"

    description = "Compiles and runs the Wire Android Client in the connected device."
    dependsOn(compileAppTask, installAppTask)
    tasks.findByName(installAppTask)?.mustRunAfter(compileAppTask)

    val localProperties = File(project.rootDir, "local.properties")
    if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        val sdkDir = properties["sdk.dir"]
        val adb = "${sdkDir}/platform-tools/adb"
        
        val applicationPackage = "com.wire.android.${Default.BUILD_FLAVOR.toLowerCase()}.${Default.BUILD_TYPE.toLowerCase()}"
        val launchActivity = "com.wire.android.MainActivity"

        commandLine(adb, "shell", "am", "start", "-n", "${applicationPackage}/${launchActivity}")
    }
}