/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.analytics

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.framework.TestUser
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.analytics.SetNewUserTrackingIdentifierUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FinalizeRegistrationAnalyticsMetadataUseCaseTest {

    @Test
    fun `givenAnonymousDataIsDisabledForRegistration, whenFinalizingRegistration, thenDoNothing`() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withIsAnonymousRegistrationEnabledResult(false)
            .arrange()

        useCase()

        coVerify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        coVerify(exactly = 0) { arrangement.globalDataStore.clearAnonymousRegistrationTrackId() }
        coVerify(exactly = 0) { arrangement.globalDataStore.setAnonymousRegistrationEnabled(any()) }
    }

    @Test
    fun `givenAnonymousDataIsEnabledForRegistration, whenFinalizingRegistration, then cleanup metadata`() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withIsAnonymousRegistrationEnabledResult(true)
            .withAnonymousRegistrationTrackId("trackId")
            .arrange()

        useCase()

        coVerify(exactly = 1) { arrangement.coreLogic.getSessionScope(any()) }
        coVerify(exactly = 1) { arrangement.globalDataStore.clearAnonymousRegistrationTrackId() }
        coVerify(exactly = 1) { arrangement.globalDataStore.setAnonymousRegistrationEnabled(eq(false)) }
    }

    private class Arrangement {

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var userSessionScope: UserSessionScope

        @MockK
        lateinit var setNewUserTrackingIdentifierUseCase: SetNewUserTrackingIdentifierUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { coreLogic.getSessionScope(any()) } returns userSessionScope
            every { coreLogic.getSessionScope(any()).setNewUserTrackingIdentifier } returns setNewUserTrackingIdentifierUseCase
        }

        fun withIsAnonymousRegistrationEnabledResult(result: Boolean) = apply {
            every { globalDataStore.isAnonymousRegistrationEnabled() } returns flowOf(result)
        }

        fun withAnonymousRegistrationTrackId(trackId: String) = apply {
            coEvery { globalDataStore.getAnonymousRegistrationTrackId() } returns trackId
        }

        fun arrange() = this to FinalizeRegistrationAnalyticsMetadataUseCase(globalDataStore, TestUser.SELF_USER_ID, coreLogic)
    }
}
