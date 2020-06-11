package scripts

import java.util.*

tasks.register("clean", Delete::class){
    delete(rootProject.buildDir)
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = BuildPlugins.Versions.gradleVersion
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register("runUnitTests") {
    description = "Runs all Unit Tests."
    dependsOn(":app:testDebugUnitTest")
}

tasks.register("runAcceptanceTests") {
    description = "Runs all Acceptance Tests in the connected device."
    dependsOn(":app:connectedAndroidTest")
}

tasks.register("compileApp") {
    description = "Compiles the Debug Version of the Wire Android Client."
    dependsOn(":app:assembleDebug")
}

tasks.register("runApp", Exec::class) {
    description = "Compiles and runs the Debug Version of the Wire Android Client in the connected device."
    dependsOn("compileApp", ":app:installDebug")
    tasks.findByName(":app:installDebug")?.mustRunAfter("compileApp")

    val localProperties = File(project.rootDir, "local.properties")
    if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        val sdkDir = properties["sdk.dir"]
        val adb = "${sdkDir}/platform-tools/adb"

        commandLine(adb, "shell", "am", "start", "-n", "com.wire.android/com.wire.android.MainActivity")
    }
}