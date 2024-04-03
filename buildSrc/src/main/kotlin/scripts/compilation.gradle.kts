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

import WriteKeyValuesToFileTask
import IncludeGitBuildTask

plugins {
    id("com.android.application") apply false
}

// TODO: Extract to a convention plugin
val gitIdTask = project.tasks.register("includeGitBuildIdentifier", IncludeGitBuildTask::class) {
    println("> Registering Task :includeGitBuildIdentifier")
}

val dependenciesVersionTask = project.tasks.register("dependenciesVersionTask", WriteKeyValuesToFileTask::class) {
    outputJsonFile.set(project.file("src/main/assets/dependencies_version.json"))
    val catalogs = project.extensions.getByType(VersionCatalogsExtension::class.java)
    val catalog = catalogs.named("klibs")
    val pairs = mapOf(
        "avs" to catalog.findVersion("avs").get().requiredVersion,
        "core-crypto" to catalog.findVersion("core-crypto-multiplatform").get().requiredVersion
    )
    keyValues.set(pairs)
}

project.afterEvaluate {
    project.tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
        dependsOn(gitIdTask)
        dependsOn(dependenciesVersionTask)
    }
}
