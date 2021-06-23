private object Dependencies {
    const val androidBuildTools = "com.android.tools.build:gradle:4.1.0"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10"
    const val detektGradlePlugin = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.14.1"
}

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    jcenter()
    google()
}
dependencies {
    implementation(Dependencies.androidBuildTools)
    implementation(Dependencies.kotlinGradlePlugin)
    implementation(Dependencies.detektGradlePlugin)

    testImplementation("junit:junit:4.13")
    testImplementation("org.amshove.kluent:kluent:1.60")
}
