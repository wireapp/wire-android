package com.wire.android.feature.auth.client.ui

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.shared.session.usecase.SetCurrentSessionToDormantUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DeviceLimitViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var setCurrentSessionToDormantUseCase: SetCurrentSessionToDormantUseCase


    private lateinit var deviceLimitViewModel: DeviceLimitViewModel

    @Before
    fun setUp() {
        deviceLimitViewModel = DeviceLimitViewModel(coroutinesTestRule.dispatcherProvider, setCurrentSessionToDormantUseCase)
    }

    @Test
    fun `given clearSession is called, when the use case runs successfully, then update isCurrentSessionClearedLiveData`() {
        coEvery { setCurrentSessionToDormantUseCase.run(any()) } returns Either.Right(Unit)

        deviceLimitViewModel.clearSession()

        deviceLimitViewModel.isCurrentSessionDormantLiveData.shouldBeUpdated {
            it shouldBeEqualTo true
        }
        coVerify(exactly = 1) { setCurrentSessionToDormantUseCase.run(any()) }
    }

    @Test
    fun `given clearSession is called, when setCurrentSessionToDormantUseCase fails, then update isCurrentSessionClearedLiveData`() {
        val failure = mockk<Failure>()
        coEvery { setCurrentSessionToDormantUseCase.run(any()) } returns Either.Left(failure)

        deviceLimitViewModel.clearSession()

        deviceLimitViewModel.isCurrentSessionDormantLiveData.shouldBeUpdated {
            it shouldBeEqualTo false
        }
        coVerify(exactly = 1) { setCurrentSessionToDormantUseCase.run(any()) }
    }
}
