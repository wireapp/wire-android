package com.wire.android.ui.home.settings.account

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase.Result.Success
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MyAccountViewModelTest {

    @Test
    fun `when trying to compute if the user requires password, and fails then should not load forgot password url context`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(IsPasswordRequiredUseCase.Result.Failure(StorageFailure.DataNotFound))
            .arrange()

        verify {
            arrangement.selfServerConfigUseCase wasNot Called
        }
    }

    @Test
    fun `when user does not requires password, then should not load forgot password url context`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .arrange()

        verify {
            arrangement.selfServerConfigUseCase wasNot Called
        }
    }

    @Test
    fun `when user requires a password, then should load forgot password url context`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .arrange()

        coVerify { arrangement.selfServerConfigUseCase() }
    }

    private class Arrangement {
        @MockK
        lateinit var navigationManager: NavigationManager

        @MockK
        lateinit var getSelfUserUseCase: GetSelfUserUseCase

        @MockK
        lateinit var getSelfTeamUseCase: GetSelfTeamUseCase

        @MockK
        lateinit var selfServerConfigUseCase: SelfServerConfigUseCase

        @MockK
        lateinit var isPasswordRequiredUseCase: IsPasswordRequiredUseCase

        val viewModel by lazy {
            MyAccountViewModel(
                getSelfUserUseCase,
                getSelfTeamUseCase,
                selfServerConfigUseCase,
                isPasswordRequiredUseCase,
                navigationManager,
                TestDispatcherProvider()
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withUserRequiresPasswordResult(result: IsPasswordRequiredUseCase.Result = Success(true)) = apply {
            coEvery { isPasswordRequiredUseCase() } returns result
        }

        fun arrange() = this to viewModel
    }
}
