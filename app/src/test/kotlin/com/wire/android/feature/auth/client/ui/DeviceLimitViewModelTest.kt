package com.wire.android.feature.auth.client.ui

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.usecase.DevicesLimitReached
import com.wire.android.feature.auth.client.usecase.MalformedPreKeys
import com.wire.android.feature.auth.client.usecase.RegisterClientParams
import com.wire.android.feature.auth.client.usecase.RegisterClientUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.framework.livedata.shouldNotBeUpdated
import com.wire.android.shared.session.usecase.SetDormantSessionToCurrentUseCase
import com.wire.android.shared.session.usecase.SetDormantSessionToCurrentUseCaseParams
import io.mockk.coEvery
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
    private lateinit var setDormantSessionToCurrentUseCase: SetDormantSessionToCurrentUseCase

    @MockK
    private lateinit var registerClientUseCase: RegisterClientUseCase

    private lateinit var deviceLimitViewModel: DeviceLimitViewModel

    @Before
    fun setUp() {
        deviceLimitViewModel = DeviceLimitViewModel(
            coroutinesTestRule.dispatcherProvider,
            setDormantSessionToCurrentUseCase,
            registerClientUseCase
        )
    }

    @Test
    fun `given registerClient is called, when use case runs successfully, then sets success to registerClientLiveData`() {
        val clientResponse = mockk<ClientResponse>()
        val clientParams = RegisterClientParams("")
        coEvery { registerClientUseCase.run(clientParams) } returns Either.Right(clientResponse)

        deviceLimitViewModel.registerClient("")

        deviceLimitViewModel.registerClientLiveData shouldBeUpdated { it shouldSucceed { } }
    }

    @Test
    fun `given registerClient is called, when use case returns DevicesLimitReached failure, then sets failure to loginResultLiveData`() {
        val clientParams = RegisterClientParams("")

        coEvery { registerClientUseCase.run(clientParams) } returns Either.Left(DevicesLimitReached)

        deviceLimitViewModel.registerClient("")

        deviceLimitViewModel.registerClientLiveData shouldBeUpdated { it shouldFail { failure -> failure shouldBe Unit } }
    }

    @Test
    fun `given registerClient is called, when use case returns MalformedPreKeys failure, then do not updates loginResultLiveData`() {
        val clientParams = RegisterClientParams("")

        coEvery { registerClientUseCase.run(clientParams) } returns Either.Left(MalformedPreKeys)

        deviceLimitViewModel.registerClient("")

        deviceLimitViewModel.registerClientLiveData.shouldNotBeUpdated()
    }

    @Test
    fun `given setDormantSessionToCurrent is called, when use case runs successfully, then sets success to sessionLiveData`() {
        val userId = "user-Id"
        val sessionParams = SetDormantSessionToCurrentUseCaseParams(userId)
        coEvery { setDormantSessionToCurrentUseCase.run(sessionParams) } returns Either.Right(Unit)

        deviceLimitViewModel.setDormantSessionToCurrent(userId)

        deviceLimitViewModel.isDormantSessionCurrentLiveData shouldBeUpdated { it shouldSucceed { } }
    }

    @Test
    fun `given setDormantSessionToCurrent is called, when use case returns failure, then sets failure to sessionLiveData`() {
        val failure = mockk<Failure>()
        val userId = "user-Id"
        val sessionParams = SetDormantSessionToCurrentUseCaseParams(userId)
        coEvery { setDormantSessionToCurrentUseCase.run(sessionParams) } returns Either.Left(failure)

        deviceLimitViewModel.setDormantSessionToCurrent(userId)

        deviceLimitViewModel.isDormantSessionCurrentLiveData shouldBeUpdated { it shouldFail { } }
    }

}
