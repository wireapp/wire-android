/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualStartupBenchmarkWithLogin {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val args get() = InstrumentationRegistry.getArguments()
    private val targetPackage get() = args.getString("TARGET_PACKAGE", "com.wire")
    private val email get() = args.getString("EMAIL").orEmpty()
    private val password get() = args.getString("PASSWORD").orEmpty()

    @Test
    fun startUpWithoutBaselineProfiler() {
        startup(CompilationMode.None()) {
            startActivityAndWait()
            if (email.isNotEmpty() && password.isNotEmpty()) login(email, password)
        }
    }

    @Test
    fun startUpWithBaselineProfiler() {
        startup(CompilationMode.Partial(BaselineProfileMode.Require)) {
            startActivityAndWait()
            if (email.isNotEmpty() && password.isNotEmpty()) login(email, password)
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

    companion object {
        private const val ITERATIONS = 5
    }
}
