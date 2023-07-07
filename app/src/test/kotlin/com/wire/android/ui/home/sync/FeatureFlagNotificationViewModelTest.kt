package com.wire.android.ui.home.sync

import com.wire.android.framework.TestUser
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.FeatureFlagState
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.configuration.GuestRoomLinkStatus
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.user.E2EIRequiredResult
import com.wire.kalium.logic.feature.user.MarkEnablingE2EIAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.MarkSelfDeletionStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.MarkGuestLinkFeatureFlagAsNotChangedUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureFlagNotificationViewModelTest {

    private val mainThreadSurrogate = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenNoCurrentUser_thenSharingRestricted() = runTest(mainThreadSurrogate) {
        val (_, viewModel) = Arrangement()
            .withCurrentSessions(CurrentSessionResult.Failure.SessionNotFound)
            .arrange()
        viewModel.initialSync()
        advanceUntilIdle()

        assertEquals(
            expected = FeatureFlagState.SharingRestrictedState.NO_USER,
            actual = viewModel.featureFlagState.fileSharingRestrictedState
        )
    }

    @Test
    fun givenLoggedInUser_whenFileSharingRestrictedForTeam_thenSharingRestricted() = runTest(mainThreadSurrogate) {
        val (_, viewModel) = Arrangement()
            .withCurrentSessions(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))
            .withFileSharingStatus(flowOf(FileSharingStatus(FileSharingStatus.Value.Disabled, false)))
            .arrange()
        viewModel.initialSync()
        advanceUntilIdle()

        assertEquals(
            expected = FeatureFlagState.SharingRestrictedState.RESTRICTED_IN_TEAM,
            actual = viewModel.featureFlagState.fileSharingRestrictedState
        )
    }

    @Test
    fun givenGuestDialogIsShown_whenDismissingIt_thenInvokeMarkGuestLinkFeatureFlagAsNotChanged() = runTest(mainThreadSurrogate) {
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessions(CurrentSessionResult.Success(AccountInfo.Valid(UserId("value", "domain"))))
            .withGuestRoomLinkFeatureFlag(flowOf(GuestRoomLinkStatus(true, false)))
            .arrange()
        viewModel.initialSync()
        advanceUntilIdle()
        viewModel.dismissGuestRoomLinkDialog()
        advanceUntilIdle()

        verify(exactly = 1) { arrangement.markGuestLinkFeatureFlagAsNotChanged() }
        assertEquals(
            expected = false,
            actual = viewModel.featureFlagState.shouldShowGuestRoomLinkDialog
        )
    }

    @Test
    fun givenLoggedInUser_whenFileSharingAllowed_thenSharingNotRestricted() = runTest(mainThreadSurrogate) {
        val (_, viewModel) = Arrangement()
            .withCurrentSessions(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))
            .withFileSharingStatus(flowOf(FileSharingStatus(FileSharingStatus.Value.EnabledAll, false)))
            .arrange()
        viewModel.initialSync()
        advanceUntilIdle()

        assertEquals(
            expected = FeatureFlagState.SharingRestrictedState.NONE,
            actual = viewModel.featureFlagState.fileSharingRestrictedState
        )
    }

    @Test
    fun givenSelfDeletionDialogIsShown_whenDismissingIt_thenInvokeMarkSelfDeletionStatusAsNotified() = runTest(mainThreadSurrogate) {
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessions(CurrentSessionResult.Success(AccountInfo.Valid(UserId("value", "domain"))))
            .arrange()
        viewModel.initialSync()
        advanceUntilIdle()
        viewModel.dismissSelfDeletingMessagesDialog()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.markSelfDeletingStatusAsNotified() }
        assertEquals(false, viewModel.featureFlagState.shouldShowSelfDeletingMessagesDialog)
    }

    @Test
    fun givenE2EIRequired_thenShowDialog() = runTest(mainThreadSurrogate) {
        val (arrangement, viewModel) = Arrangement()
            .withE2EIRequiredSettings(E2EIRequiredResult.NoGracePeriod)
            .arrange()
        viewModel.initialSync()
        advanceUntilIdle()

        assertEquals(FeatureFlagState.E2EIRequired.NoGracePeriod, viewModel.featureFlagState.e2EIRequired)
    }

    @Test
    fun givenE2EIRequiredDialogShown_whenSnoozeCalled_thenItSnoozedAndDialogShown() = runTest(mainThreadSurrogate) {
        val gracePeriod = 1.days
        val (arrangement, viewModel) = Arrangement()
            .withE2EIRequiredSettings(E2EIRequiredResult.WithGracePeriod(gracePeriod))
            .arrange()
        viewModel.initialSync()
        advanceUntilIdle()

        viewModel.snoozeE2EIdRequiredDialog(FeatureFlagState.E2EIRequired.WithGracePeriod(gracePeriod))
        advanceUntilIdle()

        assertEquals(null, viewModel.featureFlagState.e2EIRequired)
        assertEquals(FeatureFlagState.E2EISnooze(gracePeriod), viewModel.featureFlagState.e2EISnoozeInfo)
        coVerify(exactly = 1) { arrangement.markE2EIRequiredAsNotified() }
    }

    @Test
    fun givenSnoozeE2EIRequiredDialogShown_whenDismissCalled_thenItSnoozedAndDialogHidden() = runTest(mainThreadSurrogate) {
        val gracePeriod = 1.days
        val (arrangement, viewModel) = Arrangement()
            .withE2EIRequiredSettings(E2EIRequiredResult.WithGracePeriod(gracePeriod))
            .arrange()
        viewModel.snoozeE2EIdRequiredDialog(FeatureFlagState.E2EIRequired.WithGracePeriod(gracePeriod))
        advanceUntilIdle()

        viewModel.dismissSnoozeE2EIdRequiredDialog()

        assertEquals(null, viewModel.featureFlagState.e2EISnoozeInfo)
    }

    private inner class Arrangement {
        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { navigationManager.navigateBack(any()) } returns Unit
            coEvery { currentSession() } returns CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID))
            coEvery { coreLogic.getSessionScope(any()).observeSyncState() } returns flowOf(SyncState.Live)
            coEvery { coreLogic.getSessionScope(any()).observeTeamSettingsSelfDeletionStatus() } returns flowOf()
        }

        @MockK
        lateinit var currentSession: CurrentSessionUseCase

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var markGuestLinkFeatureFlagAsNotChanged: MarkGuestLinkFeatureFlagAsNotChangedUseCase

        @MockK
        lateinit var markSelfDeletingStatusAsNotified: MarkSelfDeletionStatusAsNotifiedUseCase

        @MockK
        lateinit var markE2EIRequiredAsNotified: MarkEnablingE2EIAsNotifiedUseCase

        @MockK
        lateinit var navigationManager: NavigationManager

        val viewModel: FeatureFlagNotificationViewModel = FeatureFlagNotificationViewModel(
            coreLogic = coreLogic,
            currentSessionUseCase = currentSession
        )

        init {
            every { coreLogic.getSessionScope(any()).markGuestLinkFeatureFlagAsNotChanged } returns markGuestLinkFeatureFlagAsNotChanged
            every { coreLogic.getSessionScope(any()).markSelfDeletingMessagesAsNotified } returns markSelfDeletingStatusAsNotified
            every { coreLogic.getSessionScope(any()).markE2EIRequiredAsNotified } returns markE2EIRequiredAsNotified
            coEvery { coreLogic.getSessionScope(any()).observeFileSharingStatus.invoke() } returns flowOf()
            coEvery { coreLogic.getSessionScope(any()).observeGuestRoomLinkFeatureFlag.invoke() } returns flowOf()
            coEvery { coreLogic.getSessionScope(any()).observeE2EIRequired.invoke() } returns flowOf()
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

        fun withGuestRoomLinkFeatureFlag(stateFlow: Flow<GuestRoomLinkStatus>) = apply {
            coEvery { coreLogic.getSessionScope(any()).observeGuestRoomLinkFeatureFlag() } returns stateFlow
        }

        fun withE2EIRequiredSettings(result: E2EIRequiredResult) = apply {
            coEvery { coreLogic.getSessionScope(any()).observeE2EIRequired() } returns flowOf(result)
        }

        fun arrange() = this to viewModel
    }
}
