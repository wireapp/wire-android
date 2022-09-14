package com.wire.android.feature

import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mock

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSwitchUseCaseTest {

    @Test
    fun `given current session is valid, when SwitchToAccount is called , then update current session and the user is navigated to the home screen`() = runTest {
        val expectedNavigationCommand = NavigationCommand(
            NavigationItem.Home.getRouteWithArgs(),
            BackStackMode.CLEAR_WHOLE
        )

        val (arrangement, switchAccount) =
            Arrangement()
                .withGetCurrentSession(CurrentSessionResult.Success(ACCOUNT_VALID_1))
                .withUpdateCurrentSession(UpdateCurrentSessionUseCase.Result.Success)
                .withNavigation(expectedNavigationCommand)
                .arrange()

        switchAccount(SwitchAccountParam.SwitchToAccount(ACCOUNT_VALID_2.userId))

        coVerify(exactly = 1) {
            arrangement.currentSession()
            arrangement.updateCurrentSession(any())
            arrangement.navigationManager.navigate(expectedNavigationCommand)
            arrangement.navigationManager.navigate(any())
        }
    }

    @Test
    fun `given current session is valid and there are no other sessions, when SwitchToNextAccountOrWelcome , then update current session and the user is navigated to the welcome screen`() = runTest {
        val expectedNavigationCommand = NavigationCommand(
            NavigationItem.Welcome.getRouteWithArgs(),
            BackStackMode.CLEAR_WHOLE
        )

        val (arrangement, switchAccount) =
            Arrangement()
                .withGetCurrentSession(CurrentSessionResult.Success(ACCOUNT_VALID_1))
                .withUpdateCurrentSession(UpdateCurrentSessionUseCase.Result.Success)
                .withGetAllSessions(GetAllSessionsResult.Success(emptyList()))
                .withNavigation(expectedNavigationCommand)
                .arrange()

        switchAccount(SwitchAccountParam.SwitchToNextAccountOrWelcome)

        coVerify(exactly = 1) {
            arrangement.currentSession()
            arrangement.updateCurrentSession(any())
            arrangement.navigationManager.navigate(expectedNavigationCommand)
            arrangement.navigationManager.navigate(any())
        }
    }


    @Test
    fun `given current session is invalid , when switching to account , then update current session and delete the old one`() = runTest {
        val currentAccount = ACCOUNT_INVALID_3
        val switchTO = ACCOUNT_VALID_2

        val expectedNavigationCommand = NavigationCommand(
            NavigationItem.Home.getRouteWithArgs(),
            BackStackMode.CLEAR_WHOLE
        )

        val (arrangement, switchAccount) =
            Arrangement()
                .withGetCurrentSession(CurrentSessionResult.Success(currentAccount))
                .withUpdateCurrentSession(UpdateCurrentSessionUseCase.Result.Success)
                .withGetAllSessions(GetAllSessionsResult.Success(emptyList()))
                .withNavigation(expectedNavigationCommand)
                .withDeleteSession(currentAccount.userId, DeleteSessionUseCase.Result.Success)
                .arrange()

        switchAccount(SwitchAccountParam.SwitchToAccount(switchTO.userId))

        coVerify(exactly = 1) {
            arrangement.currentSession()
            arrangement.updateCurrentSession(null)
            arrangement.updateCurrentSession(any())
            arrangement.navigationManager.navigate(expectedNavigationCommand)
            arrangement.navigationManager.navigate(any())
            arrangement.deleteSession(currentAccount.userId)
            arrangement.deleteSession(any())
        }
    }

    private companion object {
        val ACCOUNT_VALID_1 = AccountInfo.Valid(UserId("userId_valid_1", "domain_valid_1"))
        val ACCOUNT_VALID_2 = AccountInfo.Valid(UserId("userId_valid_2", "domain_valid_2"))
        val ACCOUNT_INVALID_3 =
            AccountInfo.Invalid(UserId("userId_invalid_3", "domain_invalid_3"), LogoutReason.SELF_SOFT_LOGOUT)
        val ACCOUNT_INVALID_4 =
            AccountInfo.Invalid(UserId("userId_invalid_4", "domain_invalid_4"), LogoutReason.SELF_SOFT_LOGOUT)

    }


    private class Arrangement {

        @Mock
        lateinit var updateCurrentSession: UpdateCurrentSessionUseCase

        @Mock
        lateinit var navigationManager: NavigationManager

        @Mock
        lateinit var getSessions: GetSessionsUseCase

        @Mock
        lateinit var currentSession: CurrentSessionUseCase

        @Mock
        lateinit var deleteSession: DeleteSessionUseCase


        @OptIn(ExperimentalCoroutinesApi::class)
        var accountSwitchUseCase: AccountSwitchUseCase = AccountSwitchUseCase(
            updateCurrentSession,
            navigationManager,
            getSessions,
            currentSession,
            deleteSession,
            TestScope()
        )

        fun withUpdateCurrentSession(userId: UserId, result: UpdateCurrentSessionUseCase.Result) = apply {
            coEvery { updateCurrentSession(userId) } returns result
        }

        fun withGetAllSessions(result: GetAllSessionsResult) = apply {
            coEvery { getSessions() } returns result
        }

        fun withGetCurrentSession(result: CurrentSessionResult) = apply {
            coEvery { currentSession() } returns result
        }

        fun withUpdateCurrentSession(result: UpdateCurrentSessionUseCase.Result) = apply {
            coEvery { updateCurrentSession(any()) } returns result
        }

        fun withNavigation(command: NavigationCommand) = apply {
            coEvery { navigationManager.navigate(command) } returns Unit
        }

        fun withDeleteSession(userId: UserId, result: DeleteSessionUseCase.Result) = apply {
            coEvery { deleteSession(userId) } returns result
        }

        fun arrange() = this to accountSwitchUseCase

    }
}
