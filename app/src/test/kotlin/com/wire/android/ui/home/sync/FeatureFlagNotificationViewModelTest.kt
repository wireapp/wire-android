package com.wire.android.ui.home.sync

import com.wire.android.framework.TestUser
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.FeatureFlagState
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.ObserveFileSharingStatusUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.MarkGuestLinkFeatureFlagAsNotChangedUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureFlagNotificationViewModelTest {

    @OptIn(DelicateCoroutinesApi::class)
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    @Test
    fun givenGuestDialogIsShown_whenDismissingIt_thenInvokeMarkGuestLinkFeatureFlagAsNotChanged() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessions(CurrentSessionResult.Success(AccountInfo.Valid(UserId("value", "domain"))))
            .arrange()
        launch { viewModel.initialSync() }.join()
        viewModel.dismissGuestRoomLinkDialog()

        verify(exactly = 1) { arrangement.markGuestLinkFeatureFlagAsNotChanged() }
        assertEquals(
            expected = false,
            actual = viewModel.featureFlagState.shouldShowGuestRoomLinkDialog
        )
    }

    @Test
    fun givenNoLoggedInUsers_thenSharingRestricted() {
        val (_, viewModel) = Arrangement()
            .withSessions(GetAllSessionsResult.Failure.NoSessionFound)
            .arrange()
        viewModel.updateSharingStateIfNeeded()

        assertEquals(
            expected = FeatureFlagState.SharingRestrictedState.NO_USER,
            actual = viewModel.featureFlagState.fileSharingRestrictedState
        )
    }

//    this test always fails cause viewModel.loadInitialSync() launches a separate Coroutine and updating of
//    FeatureFlagState.isFileSharingEnabledState happens after it's used in viewModel.updateSharingStateIfNeeded()

//    @Test
//    fun givenLoggedInUser_whenFileSharingRestrictedForTeam_thenSharingRestricted() = runTest {
//        val (_, viewModel) = Arrangement()
//            .withSessions(GetAllSessionsResult.Success(listOf(AccountInfo.Valid(TestUser.USER_ID))))
//            .withFileSharingStatus(flowOf(FileSharingStatus(false, false)))
//            .arrange()
//        launch(Dispatchers.Main) {
//            viewModel.loadInitialSync()
//        }
//        viewModel.updateSharingStateIfNeeded()
//
//        assertEquals(
//            expected = FeatureFlagState.SharingRestrictedState.RESTRICTED_IN_TEAM,
//            actual = viewModel.featureFlagState.fileSharingRestrictedState
//        )
//    }

    @Test
    fun givenLoggedInUser_whenFileSharingAllowed_thenSharingRestricted() {
        val (_, viewModel) = Arrangement()
            .withSessions(GetAllSessionsResult.Success(listOf(AccountInfo.Valid(TestUser.USER_ID))))
            .withFileSharingStatus(flowOf(FileSharingStatus(true, false)))
            .arrange()
        viewModel.updateSharingStateIfNeeded()

        assertEquals(
            expected = FeatureFlagState.SharingRestrictedState.NONE,
            actual = viewModel.featureFlagState.fileSharingRestrictedState
        )
    }

    private inner class Arrangement {
        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { navigationManager.navigateBack(any()) } returns Unit
            coEvery { currentSession() } returns CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID))
            coEvery { coreLogic.getSessionScope(any()).observeSyncState() } returns flowOf(SyncState.Live)
        }

        @MockK
        lateinit var getSessions: GetSessionsUseCase

        @MockK
        lateinit var currentSession: CurrentSessionUseCase

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var markGuestLinkFeatureFlagAsNotChanged: MarkGuestLinkFeatureFlagAsNotChangedUseCase

        @MockK
        lateinit var observeFileSharingStatusUseCase: ObserveFileSharingStatusUseCase

        @MockK
        lateinit var observeGuestRoomLinkFeatureFlagUseCase: ObserveGuestRoomLinkFeatureFlagUseCase

        @MockK
        lateinit var navigationManager: NavigationManager

        val viewModel: FeatureFlagNotificationViewModel = FeatureFlagNotificationViewModel(
            coreLogic = coreLogic,
            getSessions = getSessions,
            currentSessionUseCase = currentSession
        )

        init {
            every { coreLogic.getSessionScope(any()).markGuestLinkFeatureFlagAsNotChanged } returns markGuestLinkFeatureFlagAsNotChanged
            every { coreLogic.getSessionScope(any()).observeFileSharingStatus } returns observeFileSharingStatusUseCase
            every { coreLogic.getSessionScope(any()).observeGuestRoomLinkFeatureFlag } returns observeGuestRoomLinkFeatureFlagUseCase
            coEvery { observeFileSharingStatusUseCase.invoke() } returns flowOf()
            coEvery { observeGuestRoomLinkFeatureFlagUseCase.invoke() } returns flowOf()
        }

        fun withSessions(result: GetAllSessionsResult) = apply {
            coEvery { getSessions() } returns result
        }

        fun withCurrentSessions(result: CurrentSessionResult) = apply {
            coEvery { currentSession() } returns result
        }

        fun withSyncState(stateFlow: Flow<SyncState>) = apply {
            coEvery { coreLogic.getSessionScope(any()).observeSyncState() } returns stateFlow
        }

        fun withFileSharingStatus(stateFlow: Flow<FileSharingStatus>) = apply {
            coEvery { coreLogic.getSessionScope(any()).observeFileSharingStatus() } returns stateFlow
        }

        fun arrange() = this to viewModel
    }
}
