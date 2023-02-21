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
    }
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.40")
        var fdroidBuild = gradle.startParameter.taskRequests.toString().toLowerCase().contains("fdroid")
	    if (fdroidBuild) {
            println("Not including gms")
        } else {
            println("Including gms")
            classpath("com.google.gms:google-services:4.3.14")
        }
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

        // TODO we should remove this and "localrepo" dir after cryptobox-android debugging is completed
        val avsLocal = maven(url = uri("$rootDir/kalium/localrepo/"))
        exclusiveContent {
            forRepositories(avsLocal)
            filter {
                includeModule("com.wire", "cryptobox-android")
            }
        }
    }
}

plugins {
    id(ScriptPlugins.infrastructure)
}
