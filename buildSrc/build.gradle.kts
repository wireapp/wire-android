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

private object Dependencies {
    val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}"
    val detektGradlePlugin = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${klibs.versions.detekt.get()}"
    val koverGradlePlugin = "org.jetbrains.kotlinx:kover-gradle-plugin:${klibs.versions.kover.get()}"
    val junit = "junit:junit:${libs.versions.junit4.get()}"
    val kluent = "org.amshove.kluent:kluent:${libs.versions.kluent.get()}"
    const val spotless = "com.diffplug.spotless:spotless-plugin-gradle:6.1.2"
    const val junit5 = "de.mannodermaus.gradle.plugins:android-junit5:1.9.3.0"
    const val grgit = "org.ajoberstar.grgit:grgit-core:5.2.0"
    const val javapoet = "com.squareup:javapoet:1.13.0"
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
    implementation(libs.kotlin.gradlePlugin)
    implementation(Dependencies.detektGradlePlugin)
    implementation(Dependencies.koverGradlePlugin)
    implementation(Dependencies.spotless)
    implementation(Dependencies.junit5)

    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.kluent)
    implementation(Dependencies.grgit)
    implementation(Dependencies.javapoet) // https://github.com/google/dagger/issues/3068
}
