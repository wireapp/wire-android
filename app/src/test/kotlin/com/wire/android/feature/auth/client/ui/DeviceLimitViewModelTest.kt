package com.wire.android.feature.auth.client.ui

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.usecase.DevicesLimitReached
import com.wire.android.feature.auth.client.usecase.RegisterClientParams
import com.wire.android.feature.auth.client.usecase.RegisterClientUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.shared.session.usecase.SetSessionCurrentUseCase
import com.wire.android.shared.session.usecase.SetSessionCurrentUseCaseParams
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DeviceLimitViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var setSessionCurrentUseCase: SetSessionCurrentUseCase

    @MockK
    private lateinit var registerClientUseCase: RegisterClientUseCase

    private lateinit var deviceLimitViewModel: DeviceLimitViewModel

    private val clientParams = RegisterClientParams(USER_ID, PASSWORD)
    private val sessionParams = SetSessionCurrentUseCaseParams(USER_ID)

    @Before
    fun setUp() {
        deviceLimitViewModel = DeviceLimitViewModel(
            coroutinesTestRule.dispatcherProvider,
            setSessionCurrentUseCase,
            registerClientUseCase
        )
    }

    @Test
    fun `given registerClient is called, when use case runs successfully, then sets success to registerClientLiveData`() {
        val clientResponse = mockk<ClientResponse>()
        coEvery { registerClientUseCase.run(clientParams) } returns Either.Right(clientResponse)
        coEvery { setSessionCurrentUseCase.run(sessionParams) } returns Either.Right(Unit)

        deviceLimitViewModel.registerClient(USER_ID, PASSWORD)

        coVerify(exactly = 1) { registerClientUseCase.run(clientParams) }
        coVerify(exactly = 1) { setSessionCurrentUseCase.run(sessionParams) }
        deviceLimitViewModel.registerClientLiveData shouldBeUpdated { it shouldSucceed { } }
    }

    @Test
    fun `given registerClient is called, when use case returns failure, then sets failure to loginResultLiveData`() {
        coEvery { registerClientUseCase.run(clientParams) } returns Either.Left(DevicesLimitReached)

        deviceLimitViewModel.registerClient(USER_ID, PASSWORD)

        coVerify(exactly = 1) { registerClientUseCase.run(clientParams) }
        coVerify(inverse = true) { setSessionCurrentUseCase.run(sessionParams) }
        deviceLimitViewModel.registerClientLiveData shouldBeUpdated { it shouldFail { failure -> failure shouldBe DevicesLimitReached } }
    }

    @Test
    fun `given registerClient is called, when setSessionCurrent use case returns failure, then sets failure to loginResultLiveData`() {
        val clientResponse = mockk<ClientResponse>()
        val failure = mockk<Failure>()

        coEvery { registerClientUseCase.run(clientParams) } returns Either.Right(clientResponse)
        coEvery { setSessionCurrentUseCase.run(sessionParams) } returns Either.Left(failure)

        deviceLimitViewModel.registerClient(USER_ID, PASSWORD)

        coVerify(exactly = 1) { setSessionCurrentUseCase.run(sessionParams) }
        coVerify(exactly = 1) { registerClientUseCase.run(clientParams) }
        deviceLimitViewModel.registerClientLiveData shouldBeUpdated { it shouldFail { failure -> failure shouldBe failure } }
    }

    companion object {
        private const val USER_ID = "user-id"
        private const val PASSWORD = "password-test"
    }

}
