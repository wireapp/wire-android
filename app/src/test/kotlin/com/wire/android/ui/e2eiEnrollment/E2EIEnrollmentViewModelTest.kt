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

package com.wire.android.ui.e2eiEnrollment

import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.logic.feature.client.FinalizeMLSClientAfterE2EIEnrollment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class E2EIEnrollmentViewModelTest {

    @Test
    fun givenMLSClientFinalizationIsRunning_whenFinalizing_thenOnCompleteWaitsForFinalization() = runTest {
        val finalizationCompleted = CompletableDeferred<Unit>()
        val viewModel = E2EIEnrollmentViewModel(
            finalizeMLSClientAfterE2EIEnrollment = object : FinalizeMLSClientAfterE2EIEnrollment {
                override suspend fun invoke() {
                    finalizationCompleted.await()
                }
            }
        )
        var onCompleteCalled = false

        viewModel.finalizeMLSClient {
            onCompleteCalled = true
        }
        advanceUntilIdle()

        assertFalse(onCompleteCalled)

        finalizationCompleted.complete(Unit)
        advanceUntilIdle()

        assertTrue(onCompleteCalled)
    }
}
