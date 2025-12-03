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
rootProject.name = "wire-android"
pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenLocal()
        mavenCentral()
        google()
         maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

// Include all the existent modules in the project
val basePathModules = setOf("features", "core", "tests")
val ignorableModules = setOf("buildSrc", "kalium")
rootDir
    .walk()
    .maxDepth(1)
    .filter { it != rootDir }
    .filter { project ->
        basePathModules.contains(project.name) || ignorableModules.none { project.name == it }
                && project.isDirectory && file("${project.absolutePath}/build.gradle.kts").exists()
    }.map { rootDirFile ->
        if (basePathModules.contains(rootDirFile.name)) {
            rootDirFile.walk()
                .maxDepth(1)
                .filter { it.name != "template" && file("${it.absolutePath}/build.gradle.kts").exists() }
                .map { "${rootDirFile.name}:${it.name}" }.toList()
        } else {
            listOf(rootDirFile.name)
        }
    }.forEach {
        include(it)
    }

dependencyResolutionManagement {
 repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
    versionCatalogs {
        create("klibs") {
            from(files("kalium/gradle/libs.versions.toml"))
        }
    }
}

// A work-around where we define the included builds in a different file
// so Reloaded's Dependabot doesn't try to look into Kalium's build.gradle.kts, which is inaccessible as it is a git submodule.
// See: https://github.com/dependabot/dependabot-core/issues/7201#issuecomment-1571319655
apply(from = "include_builds.gradle.kts")
