private object Dependencies {
    const val androidBuildTools = "com.android.tools.build:gradle:7.0.4"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21"
    const val detektGradlePlugin = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.19.0"
    const val junit = "junit:junit:4.13"
    const val kluent = "org.amshove.kluent:kluent:1.68"
    const val hilt = "com.google.dagger:hilt-android-gradle-plugin:2.38.1"
    const val spotless = "com.diffplug.spotless:spotless-plugin-gradle:6.1.2"
    const val junit5 = "de.mannodermaus.gradle.plugins:android-junit5:1.8.2.0"
    const val grgit = "org.ajoberstar.grgit:grgit-core:5.0.0-rc.3"
}

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    id("org.ajoberstar.grgit") version "5.0.0-rc.3"
}

repositories {
    google()
    mavenCentral()
}
dependencies {
    implementation(Dependencies.androidBuildTools)
    implementation(Dependencies.kotlinGradlePlugin)
    implementation(Dependencies.detektGradlePlugin)
    implementation(Dependencies.spotless)
    implementation(Dependencies.hilt)
    implementation(Dependencies.junit5)

    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.kluent)
    implementation(Dependencies.grgit)
}
