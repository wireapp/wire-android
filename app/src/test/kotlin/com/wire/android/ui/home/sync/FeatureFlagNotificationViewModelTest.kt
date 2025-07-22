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
package com.wire.android.ui.home.sync

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.AppLockSource
import com.wire.android.feature.DisableAppLockUseCase
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.FeatureFlagState
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.AppLockTeamConfig
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.configuration.GuestRoomLinkStatus
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.applock.AppLockTeamFeatureConfigObserver
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.E2EIRequiredResult
import com.wire.kalium.logic.feature.user.MarkEnablingE2EIAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.MarkSelfDeletionStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.e2ei.MarkNotifyForRevokedCertificateAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.MarkGuestLinkFeatureFlagAsNotChangedUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class FeatureFlagNotificationViewModelTest {

    @Test
    fun givenNoCurrentUser_thenSharingRestricted() = runTest {
        val (_, viewModel) = Arrangement()
            .withCurrentSessionsFlow(flowOf(CurrentSessionResult.Failure.SessionNotFound))
            .arrange()
        advanceUntilIdle()

        assertEquals(
            FeatureFlagState.FileSharingState.NoUser,
            viewModel.featureFlagState.isFileSharingState
        )
    }

    @Test
    fun givenLoggedInUser_whenFileSharingRestrictedForTeam_thenSharingRestricted() = runTest {
        val (_, viewModel) = Arrangement()
            .withCurrentSessionsFlow(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID))))
            .withFileSharingStatus(flowOf(FileSharingStatus(FileSharingStatus.Value.Disabled, false)))
            .arrange()
        advanceUntilIdle()

        assertEquals(
            FeatureFlagState.FileSharingState.DisabledByTeam,
            viewModel.featureFlagState.isFileSharingState
        )
    }

    @Test
    fun givenGuestDialogIsShown_whenDismissingIt_thenInvokeMarkGuestLinkFeatureFlagAsNotChanged() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionsFlow(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(UserId("value", "domain")))))
            .withGuestRoomLinkFeatureFlag(flowOf(GuestRoomLinkStatus(true, false)))
            .arrange()
        advanceUntilIdle()
        viewModel.dismissGuestRoomLinkDialog()
        advanceUntilIdle()

        verify(exactly = 1) { arrangement.markGuestLinkFeatureFlagAsNotChanged() }
        assertEquals(
            false,
            viewModel.featureFlagState.shouldShowGuestRoomLinkDialog
        )
    }

    @Test
    fun givenLoggedInUser_whenFileSharingAllowed_thenSharingNotRestricted() = runTest {
        val (_, viewModel) = Arrangement()
            .withCurrentSessionsFlow(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID))))
            .withFileSharingStatus(flowOf(FileSharingStatus(FileSharingStatus.Value.EnabledAll, false)))
            .arrange()
        advanceUntilIdle()

        assertEquals(
            FeatureFlagState.FileSharingState.AllowAll,
            viewModel.featureFlagState.isFileSharingState
        )
    }

    @Test
    fun givenSelfDeletionDialogIsShown_whenDismissingIt_thenInvokeMarkSelfDeletionStatusAsNotified() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionsFlow(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(UserId("value", "domain")))))
            .arrange()
        advanceUntilIdle()
        viewModel.dismissSelfDeletingMessagesDialog()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.markSelfDeletingStatusAsNotified() }
        assertEquals(false, viewModel.featureFlagState.shouldShowSelfDeletingMessagesDialog)
    }

    @Test
    fun givenTeamAppLockIsEnforceButNotChanged_whenAppHaveNotAppLockSetup_thenDisplayTheAppLockDialog() = runTest {
        val (_, viewModel) = Arrangement()
            .withCurrentSessionsFlow(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(UserId("value", "domain")))))
            .withIsAppLockSetup(false)
            .withTeamAppLockEnforce(AppLockTeamConfig(true, Duration.ZERO, false))
            .arrange()
        advanceUntilIdle()

        assertTrue(viewModel.featureFlagState.shouldShowTeamAppLockDialog)
    }

    @Test
    fun givenE2EIRequired_thenShowDialog() = runTest {
        val (_, viewModel) = Arrangement()
            .withE2EIRequiredSettings(E2EIRequiredResult.NoGracePeriod.Create)
            .arrange()
        advanceUntilIdle()

        assertEquals(FeatureFlagState.E2EIRequired.NoGracePeriod.Create, viewModel.featureFlagState.e2EIRequired)
    }

    @Test
    fun givenE2EIRequiredDialogShown_whenSnoozeCalled_thenItSnoozedAndDialogShown() = runTest {
        val gracePeriod = 1.days
        val (arrangement, viewModel) = Arrangement()
            .withE2EIRequiredSettings(E2EIRequiredResult.WithGracePeriod.Create(gracePeriod))
            .arrange()
        advanceUntilIdle()

        viewModel.snoozeE2EIdRequiredDialog(FeatureFlagState.E2EIRequired.WithGracePeriod.Create(gracePeriod))
        advanceUntilIdle()

        assertEquals(null, viewModel.featureFlagState.e2EIRequired)
        assertEquals(FeatureFlagState.E2EISnooze(gracePeriod), viewModel.featureFlagState.e2EISnoozeInfo)
        coVerify(exactly = 1) { arrangement.markE2EIRequiredAsNotified(gracePeriod) }
    }

    @Test
    fun givenSnoozeE2EIRequiredDialogShown_whenDismissCalled_thenItSnoozedAndDialogHidden() = runTest {
        val gracePeriod = 1.days
        val (_, viewModel) = Arrangement()
            .withE2EIRequiredSettings(E2EIRequiredResult.WithGracePeriod.Create(gracePeriod))
            .arrange()
        viewModel.snoozeE2EIdRequiredDialog(FeatureFlagState.E2EIRequired.WithGracePeriod.Create(gracePeriod))
        advanceUntilIdle()

        viewModel.dismissSnoozeE2EIdRequiredDialog()

        assertEquals(null, viewModel.featureFlagState.e2EISnoozeInfo)
    }

    @Test
    fun givenE2EIRenewRequired_thenShowDialog() = runTest {
        val (_, viewModel) = Arrangement()
            .withE2EIRequiredSettings(E2EIRequiredResult.NoGracePeriod.Renew)
            .arrange()
        advanceUntilIdle()

        assertEquals(FeatureFlagState.E2EIRequired.NoGracePeriod.Renew, viewModel.featureFlagState.e2EIRequired)
    }

    @Test
    fun givenE2EIRenewDialogShown_whenSnoozeCalled_thenItSnoozedAndDialogShown() = runTest {
        val gracePeriod = 1.days
        val (arrangement, viewModel) = Arrangement()
            .withE2EIRequiredSettings(E2EIRequiredResult.WithGracePeriod.Renew(gracePeriod))
            .arrange()
        advanceUntilIdle()

        viewModel.snoozeE2EIdRequiredDialog(FeatureFlagState.E2EIRequired.WithGracePeriod.Renew(gracePeriod))
        advanceUntilIdle()

        assertEquals(null, viewModel.featureFlagState.e2EIRequired)
        assertEquals(FeatureFlagState.E2EISnooze(gracePeriod), viewModel.featureFlagState.e2EISnoozeInfo)
        coVerify(exactly = 1) { arrangement.markE2EIRequiredAsNotified(gracePeriod) }
    }

    @Test
    fun givenSnoozeE2EIRenewDialogShown_whenDismissCalled_thenItSnoozedAndDialogHidden() = runTest {
        val gracePeriod = 1.days
        val (_, viewModel) = Arrangement()
            .withE2EIRequiredSettings(E2EIRequiredResult.WithGracePeriod.Renew(gracePeriod))
            .arrange()
        viewModel.snoozeE2EIdRequiredDialog(FeatureFlagState.E2EIRequired.WithGracePeriod.Renew(gracePeriod))
        advanceUntilIdle()

        viewModel.dismissSnoozeE2EIdRequiredDialog()

        assertEquals(null, viewModel.featureFlagState.e2EISnoozeInfo)
    }

    @Test
    fun givenOngoingCallEnded_thenShowDialog() = runTest {
        val (_, viewModel) = Arrangement()
            .withEndCallDialog()
            .arrange()
        advanceUntilIdle()

        assertEquals(true, viewModel.featureFlagState.showCallEndedBecauseOfConversationDegraded)
    }

    @Test
    fun givenSourceIsTeamEnforce_whenConfirmingAppLockNotEnforcedDialog_thenRemoveAppLock() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionsFlow(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID))))
            .withAppLockSource(AppLockSource.TeamEnforced)
            .withDisableAppLockUseCase()
            .arrange()
        advanceUntilIdle()

        viewModel.confirmAppLockNotEnforced()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.disableAppLockUseCase() }
    }

    @Test
    fun givenAppLockSourceIsManual_whenConfirmingAppLockNotEnforcedDialog_thenDoNothing() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionsFlow(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID))))
            .withAppLockSource(AppLockSource.Manual)
            .arrange()
        advanceUntilIdle()

        viewModel.confirmAppLockNotEnforced()
        advanceUntilIdle()

        coVerify(exactly = 0) { arrangement.disableAppLockUseCase() }
    }

    @Test
    fun givenE2EIRequired_whenUserLoggedOut_thenHideDialog() = runTest {
        val currentSessionsFlow = MutableSharedFlow<CurrentSessionResult>(1)
        val (_, viewModel) = Arrangement()
            .withE2EIRequiredSettings(E2EIRequiredResult.NoGracePeriod.Create)
            .withCurrentSessionsFlow(currentSessionsFlow)
            .arrange()

        currentSessionsFlow.emit(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))
        advanceUntilIdle()

        assertEquals(FeatureFlagState.E2EIRequired.NoGracePeriod.Create, viewModel.featureFlagState.e2EIRequired)

        // when
        currentSessionsFlow.emit(CurrentSessionResult.Failure.SessionNotFound)
        advanceUntilIdle()

        // then
        assertEquals(null, viewModel.featureFlagState.e2EIRequired)
    }

    @Test
    fun givenADisplayedDialog_whenDismissingIt_thenInvokeMarkFileSharingStatusAsNotifiedUseCaseOnce() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionsFlow(flowOf(CurrentSessionResult.Success(AccountInfo.Valid(UserId("value", "domain")))))
            .arrange()
        coEvery { arrangement.markNotifyForRevokedCertificateAsNotified() } returns Unit

        viewModel.dismissE2EICertificateRevokedDialog()

        assertEquals(false, viewModel.featureFlagState.shouldShowE2eiCertificateRevokedDialog)
        coVerify(exactly = 1) { arrangement.markNotifyForRevokedCertificateAsNotified() }
    }

    private inner class Arrangement {

        @MockK
        lateinit var currentSessionFlow: CurrentSessionFlowUseCase

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var markGuestLinkFeatureFlagAsNotChanged: MarkGuestLinkFeatureFlagAsNotChangedUseCase

        @MockK
        lateinit var markSelfDeletingStatusAsNotified: MarkSelfDeletionStatusAsNotifiedUseCase

        @MockK
        lateinit var markE2EIRequiredAsNotified: MarkEnablingE2EIAsNotifiedUseCase

        @MockK
        lateinit var disableAppLockUseCase: DisableAppLockUseCase

        @MockK
        lateinit var ppLockTeamFeatureConfigObserver: AppLockTeamFeatureConfigObserver

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var markNotifyForRevokedCertificateAsNotified: MarkNotifyForRevokedCertificateAsNotifiedUseCase

        val viewModel: FeatureFlagNotificationViewModel by lazy {
            FeatureFlagNotificationViewModel(
                coreLogic = { coreLogic },
                currentSessionFlow = { currentSessionFlow },
                globalDataStore = { globalDataStore },
                disableAppLockUseCase = { disableAppLockUseCase },
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))
            coEvery { coreLogic.getSessionScope(any()).observeSyncState() } returns flowOf(SyncState.Live)
            coEvery { coreLogic.getSessionScope(any()).observeTeamSettingsSelfDeletionStatus() } returns flowOf()
            every { coreLogic.getSessionScope(any()).markGuestLinkFeatureFlagAsNotChanged } returns markGuestLinkFeatureFlagAsNotChanged
            every { coreLogic.getSessionScope(any()).markSelfDeletingMessagesAsNotified } returns markSelfDeletingStatusAsNotified
            every { coreLogic.getSessionScope(any()).markE2EIRequiredAsNotified } returns markE2EIRequiredAsNotified
            coEvery { coreLogic.getSessionScope(any()).appLockTeamFeatureConfigObserver } returns ppLockTeamFeatureConfigObserver
            coEvery { coreLogic.getSessionScope(any()).observeFileSharingStatus.invoke() } returns flowOf()
            coEvery { coreLogic.getSessionScope(any()).observeGuestRoomLinkFeatureFlag.invoke() } returns flowOf()
            coEvery { coreLogic.getSessionScope(any()).observeE2EIRequired.invoke() } returns flowOf()
            coEvery { coreLogic.getSessionScope(any()).calls.observeEndCallDueToDegradationDialog() } returns flowOf()
            coEvery { coreLogic.getSessionScope(any()).calls.observeAskCallFeedbackUseCase() } returns flowOf()
            coEvery { coreLogic.getSessionScope(any()).observeShouldNotifyForRevokedCertificate() } returns flowOf()
            coEvery { coreLogic.getSessionScope(any()).calls.updateNextTimeCallFeedback(any()) } returns Unit
            every { coreLogic.getSessionScope(any()).markNotifyForRevokedCertificateAsNotified } returns
                    markNotifyForRevokedCertificateAsNotified
            coEvery { ppLockTeamFeatureConfigObserver() } returns flowOf(null)
        }

        fun withCurrentSessionsFlow(result: Flow<CurrentSessionResult>) = apply {
            coEvery { currentSessionFlow() } returns result
        }

        fun withIsAppLockSetup(result: Boolean) = apply {
            coEvery { globalDataStore.isAppLockPasscodeSet() } returns result
        }

        fun withAppLockSource(source: AppLockSource) = apply {
            coEvery { globalDataStore.getAppLockSource() } returns source
        }

        fun withDisableAppLockUseCase() = apply {
            coEvery { disableAppLockUseCase() } returns true
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

        fun withEndCallDialog() = apply {
            coEvery { coreLogic.getSessionScope(any()).calls.observeEndCallDueToDegradationDialog() } returns flowOf(Unit)
        }

        fun withTeamAppLockEnforce(result: AppLockTeamConfig?) = apply {
            coEvery { ppLockTeamFeatureConfigObserver() } returns flowOf(result)
        }

        fun arrange() = this to viewModel
    }
}
