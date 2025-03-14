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
package com.wire.android.feature

import com.wire.android.services.ServicesManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StartPersistentWebsocketIfNecessaryUseCaseTest {

    @Test
    fun givenShouldStartPersistentWebsocketTrue_whenInvoking_thenStartService() =
        runTest {
            // given
            val (arrangement, sut) = Arrangement()
                .withShouldStartPersistentWebsocketServiceResult(true)
                .arrange()

            // when
            sut.invoke()

            // then
            verify(exactly = 1) { arrangement.servicesManager.startPersistentWebSocketService() }
        }

    @Test
    fun givenShouldStartPersistentWebsocketFalse_whenInvoking_thenDONTStartService() =
        runTest {
            // given
            val (arrangement, sut) = Arrangement()
                .withShouldStartPersistentWebsocketServiceResult(false)
                .arrange()

            // when
            sut.invoke()

            // then
            verify(exactly = 0) { arrangement.servicesManager.startPersistentWebSocketService() }
        }

    inner class Arrangement {

        @MockK
        lateinit var shouldStartPersistentWebSocketServiceUseCase: ShouldStartPersistentWebSocketServiceUseCase

        @MockK
        lateinit var servicesManager: ServicesManager

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { servicesManager.startPersistentWebSocketService() } returns Unit
            every { servicesManager.stopPersistentWebSocketService() } returns Unit
        }

        fun arrange() = this to StartPersistentWebsocketIfNecessaryUseCase(
            servicesManager,
            shouldStartPersistentWebSocketServiceUseCase
        )

        fun withShouldStartPersistentWebsocketServiceResult(shouldStart: Boolean) = apply {
            coEvery { shouldStartPersistentWebSocketServiceUseCase.invoke() } returns
                    ShouldStartPersistentWebSocketServiceUseCase.Result.Success(shouldStart)
        }
    }
}
