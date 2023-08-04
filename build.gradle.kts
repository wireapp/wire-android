/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven(url ="https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(libs.hilt.gradlePlugin)
        classpath(libs.googleGms.gradlePlugin)
        classpath(libs.aboutLibraries.gradlePlugin)
    }
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/wireapp/core-crypto")
            credentials {
                username = getLocalProperty("github.package_registry.user", System.getenv("GITHUB_USER"))
                password = getLocalProperty("github.package_registry.token", System.getenv("GITHUB_TOKEN"))
            }
        }
        maven { url = java.net.URI("https://jitpack.io") }
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
}

plugins {
    id(ScriptPlugins.infrastructure)
    alias(libs.plugins.ksp) apply false // https://github.com/google/dagger/issues/3965
}
