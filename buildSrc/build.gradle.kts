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

private object Dependencies {
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21"
    const val detektGradlePlugin = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.19.0"
    const val junit = "junit:junit:4.13"
    const val kluent = "org.amshove.kluent:kluent:1.73"
    const val hilt = "com.google.dagger:hilt-android-gradle-plugin:2.47"
    const val spotless = "com.diffplug.spotless:spotless-plugin-gradle:6.1.2"
    const val junit5 = "de.mannodermaus.gradle.plugins:android-junit5:1.9.3.0"
    const val grgit = "org.ajoberstar.grgit:grgit-core:5.2.0"
}

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    id("org.ajoberstar.grgit") version "5.0.0-rc.3"
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("com.android.tools.build:gradle:${klibs.versions.agp.get()}")
    implementation(Dependencies.kotlinGradlePlugin)
    implementation(Dependencies.detektGradlePlugin)
    implementation(Dependencies.spotless)
    implementation(Dependencies.hilt)
    implementation(Dependencies.junit5)

    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.kluent)
    implementation(Dependencies.grgit)
}
