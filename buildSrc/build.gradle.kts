private object Dependencies {
    const val androidBuildTools = "com.android.tools.build:gradle:7.0.3"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10"
    const val detektGradlePlugin = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.19.0"
    const val junit = "junit:junit:4.13"
    const val kluent = "org.amshove.kluent:kluent:1.68"
    const val hilt = "com.google.dagger:hilt-android-gradle-plugin:2.38.1"
}

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    google()
    mavenCentral()
}
dependencies {
    implementation(Dependencies.androidBuildTools)
    implementation(Dependencies.kotlinGradlePlugin)
    implementation(Dependencies.detektGradlePlugin)
    implementation(Dependencies.hilt)

    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.kluent)
}
