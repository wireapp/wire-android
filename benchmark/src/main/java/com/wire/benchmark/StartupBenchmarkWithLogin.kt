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
package com.wire.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This benchmark will measure the app startup when we have a valid session.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmarkWithLogin {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val args get() = InstrumentationRegistry.getArguments()
    private val targetPackage get() = args.getString("TARGET_PACKAGE", "com.wire")
    private val backendName get() = args.getString("BACKEND_NAME", "STAGING")
    private val conversationName get() = args.getString("CONVERSATION_NAME").orEmpty()

    @Test
    fun startUpWithoutBaselineProfiler() {
        val fixture = createFixture()
        try {
            startup(
            CompilationMode.None()
        ) {
            startActivityAndWait()
            switchBackend(fixture.backend.deeplink)
            login(fixture.email, fixture.password)
        }
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun startUpWithBaselineProfiler() {
        val fixture = createFixture()
        try {
            startup(
            CompilationMode.Partial(BaselineProfileMode.Require)
        ) {
            startActivityAndWait()
            switchBackend(fixture.backend.deeplink)
            login(fixture.email, fixture.password)
        }
        } finally {
            fixture.cleanup()
        }
    }

    private fun startup(
        compilationMode: CompilationMode,
        setupBlock: MacrobenchmarkScope.() -> Unit = {},
    ) = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.COLD,
        compilationMode = compilationMode,
        setupBlock = setupBlock
    ) {
        pressHome()
        startActivityAndWait()
    }

    private fun createFixture() = BenchmarkFixtureFactory.create(
        backendName = backendName,
        context = getInstrumentation().context,
        conversationNameOverride = conversationName,
    )

    companion object {
        private const val ITERATIONS = 5
    }
}
