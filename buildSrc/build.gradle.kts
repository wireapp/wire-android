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

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    id("org.ajoberstar.grgit") version "5.2.2"
}

repositories {
    google()
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("com.android.tools.build:gradle:${klibs.versions.agp.get()}")
    implementation(libs.kotlin.gradlePlugin)
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${klibs.versions.detekt.get()}")
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:${libs.versions.kover.get()}")
    implementation(libs.android.junit5)

    testImplementation("junit:junit:${libs.versions.junit4.get()}")
    testImplementation(kotlin("test"))
    implementation(libs.grgit.core)
    implementation(libs.squareup.javapoet) // https://github.com/google/dagger/issues/3068
}
