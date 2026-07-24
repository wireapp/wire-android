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

package com.wire.android.ui.debug

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.util.logging.LogFileWriter
import com.wire.kalium.common.logger.CoreLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.debug.ChangeProfilingUseCase
import com.wire.kalium.logic.feature.debug.ObserveDatabaseLoggerStateUseCase
import com.wire.kalium.util.DebugKaliumApi
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class, DebugKaliumApi::class)
@ExtendWith(CoroutineTestExtension::class)
class UserDebugViewModelTest {

    @BeforeEach
    fun setUp() {
        mockkObject(CoreLogger)
        every { CoreLogger.setLoggingLevel(any()) } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CoreLogger)
    }

    @Test
    fun givenLoggingIsDisabled_whenEnablingLogging_thenEnableSharedLoggerLevel() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.setLoggingEnabledState(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.globalDataStore.setLoggingEnabled(true) }
        coVerify(exactly = 1) { arrangement.logFileWriter.start() }
        verify(exactly = 1) { CoreLogger.setLoggingLevel(KaliumLogLevel.VERBOSE) }
    }

    @Test
    fun givenLoggingIsEnabled_whenDisablingLogging_thenDisableSharedLoggerLevel() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.setLoggingEnabledState(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.globalDataStore.setLoggingEnabled(false) }
        coVerify(exactly = 1) { arrangement.logFileWriter.stop() }
        verify(exactly = 1) { CoreLogger.setLoggingLevel(KaliumLogLevel.DISABLED) }
    }

    private class Arrangement {

        @MockK
        lateinit var logFileWriter: LogFileWriter

        @MockK
        lateinit var currentClientIdUseCase: ObserveCurrentClientIdUseCase

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var changeProfilingUseCase: ChangeProfilingUseCase

        @MockK
        lateinit var observeDatabaseLoggerState: ObserveDatabaseLoggerStateUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { logFileWriter.activeLoggingFile } returns File("active.log")
            every { globalDataStore.isLoggingEnabled() } returns emptyFlow()
            coEvery { currentClientIdUseCase() } returns emptyFlow()
            coEvery { observeDatabaseLoggerState() } returns emptyFlow()
        }

        fun arrange() = this to UserDebugViewModel(
            currentAccount = UserId("user", "domain"),
            logFileWriter = logFileWriter,
            currentClientIdUseCase = currentClientIdUseCase,
            globalDataStore = globalDataStore,
            changeProfilingUseCase = changeProfilingUseCase,
            observeDatabaseLoggerState = observeDatabaseLoggerState
        )
    }
}
