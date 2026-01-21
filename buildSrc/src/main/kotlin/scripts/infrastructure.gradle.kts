/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package scripts

import findVersion
import uppercaseFirstChar
import java.util.Properties

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

tasks.register("runUnitTests") {
    description = "Runs all Unit Tests."
    dependsOn(":app:test${Default.BUILD_VARIANT}UnitTest")
    // kalium have only 2 build type debug or release
    val buildType =
        if (Default.resolvedBuildType() == BuildTypes.DEBUG) Default.resolvedBuildType().uppercaseFirstChar()
        else BuildTypes.RELEASE.uppercaseFirstChar()

    // valid submodules path to run unit tests
    val validSubprojects = setOf("core", "features")
    rootProject.subprojects {
        if (validSubprojects.contains(parent?.name)) {
            dependsOn(":${parent?.name}:$name:test${buildType}UnitTest")
        }
    }
}

tasks.register("runAcceptanceTests") {
    description = "Runs all Acceptance Tests in the connected device."
    dependsOn(":app:connectedDevDebugAndroidTest")
}

tasks.register("assembleApp") {
    description = "assemble the Wire Android Client."
    dependsOn(":app:assemble${Default.BUILD_VARIANT}")
}

tasks.register("compileApp") {
    description = "compiles the Wire Android Client source."
    dependsOn(":app:compile${Default.BUILD_VARIANT}Sources")
}

tasks.register("bundleApp") {
    description = "bundles the Wire Android Client to an Android App Bundle."
    dependsOn(":app:bundle${Default.BUILD_VARIANT}")
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

        val applicationPackage = "com.wire.android.${Default.resolvedBuildFlavor()}"
        val launchActivity = "com.wire.android.feature.launch.ui.LauncherActivity"

        commandLine(adb, "shell", "am", "start", "-n", "${applicationPackage}/${launchActivity}")
    }
}
