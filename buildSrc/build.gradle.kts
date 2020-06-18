private object Dependencies {
    const val AndroidBuildTools  = "com.android.tools.build:gradle:4.0.0"
    const val KotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72"
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
    implementation(Dependencies.AndroidBuildTools)
    implementation(Dependencies.KotlinGradlePlugin)
}