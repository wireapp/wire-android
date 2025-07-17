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
package com.wire.android.gradle

import AndroidSdk
import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import versionCatalog
import findLibrary
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    kotlinAndroidExtension: KotlinBaseExtension
): Unit = with(commonExtension) {
    compileSdk = AndroidSdk.compile

    defaultConfig {
        minSdk = AndroidSdk.min
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    configureKotlin()
    configureCompose(this)

    dependencies {
        add("coreLibraryDesugaring", versionCatalog.findLibrary("android.desugarJdkLibs").get())
    }
    configureLint(project)

    with(kotlinAndroidExtension) {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.majorVersion))
        }
    }
}

/**
 * Configure base Kotlin options
 */
private fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            // Set JVM target to 17
            jvmTarget = JavaVersion.VERSION_17.toString()
            // Treat all Kotlin warnings as errors (disabled by default)
            // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
            val warningsAsErrors: String? by project
            allWarningsAsErrors = warningsAsErrors.toBoolean()
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-opt-in=kotlin.RequiresOptIn",
                // Enable experimental coroutines APIs, including Flow
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
        }
        compilerOptions.freeCompilerArgs.addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
        )
    }
}

private fun CommonExtension<*, *, *, *, *, *>.configureLint(project: Project) {
    lint {
        showAll = true
        explainIssues = true
        quiet = false
        abortOnError = true
        ignoreWarnings = true
        disable.add("InvalidPackage") // Some libraries have issues with this.
        disable.add("OldTargetApi") // Lint gives this warning related to SDK Beta.
        disable.add("IconDensities") // For testing purpose. This is safe to remove.
        disable.add("IconMissingDensityFolder") // For testing purpose. This is safe to remove.
        disable.add("ComposePreviewPublic") // Needed for screenshot testing.
        disable.add("MissingTranslation") // We don't want to hardcode translations in English for other languages.
        disable.add("ImpliedQuantity") // In some translations we just have one as words
        baseline = project.file("lint-baseline.xml")
    }
    
    // Configure lint heap size for CI environments
    project.tasks.withType(JavaExec::class.java).configureEach {
        if (name.contains("lint", ignoreCase = true)) {
            jvmArgs("-Xmx6g", "-XX:+UseParallelGC", "-XX:MaxMetaspaceSize=1g")
        }
    }

    with(project) {
        dependencies {
            add("lintChecks", findLibrary("lint-compose"))
        }
    }
}

internal fun CommonExtension<*, *, *, *, *, *>.configureAndroidKotlinTests() {
    defaultConfig {
        testInstrumentationRunner = "com.wire.android.CustomTestRunner"
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "clearPackageData" to "true",
                "force-queryable" to "true"
            )
        )
    }

    // This enables us to share some code between UI and Unit tests!
    fun AndroidSourceSet.includeCommonTestSourceDir() = java {
        srcDir("src/commonTest/kotlin")
    }
    sourceSets["test"].includeCommonTestSourceDir()
    sourceSets["androidTest"].includeCommonTestSourceDir()

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}
