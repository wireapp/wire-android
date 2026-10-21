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
import com.wire.android.session.FakeSessionAdmission
import com.wire.android.ui.home.FeatureFlagState
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.AppLockTeamConfig
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.configuration.GuestRoomLinkStatus
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
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
import io.mockk.mockk
import io.mockk.verify
import io.mockk.impl.annotations.MockK
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
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

        coVerify(exactly = 1) { arrangement.markGuestLinkFeatureFlagAsNotChanged() }
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

    @Test
    fun `one admission owns all feature collectors through pending replacement invalidation and teardown`() = runTest {
        val started = mutableListOf<String>()
        val drained = mutableListOf<String>()
        val current = MutableStateFlow<CurrentSessionResult>(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))
        val arrangement = Arrangement().withCurrentSessionsFlow(current).withTrackedCollectors(started, drained)
        arrangement.admission.publish(TestUser.USER_ID, null)
        val (_, viewModel) = arrangement.arrange()
        runCurrent()
        assertEquals(emptyList<String>(), started)
        arrangement.admission.publish(TestUser.USER_ID, arrangement.session)
        runCurrent()
        assertEquals(7, started.size)
        arrangement.admission.publish(TestUser.USER_ID, null)
        runCurrent()
        assertEquals(7, drained.size)
        arrangement.admission.publish(TestUser.USER_ID, arrangement.session)
        runCurrent()
        assertEquals(14, started.size) // Equal raw metadata still rebinds the provider revision.
        current.value = CurrentSessionResult.Success(AccountInfo.Invalid(TestUser.USER_ID, LogoutReason.SELF_SOFT_LOGOUT))
        runCurrent()
        assertEquals(14, drained.size)
        assertEquals(FeatureFlagState.FileSharingState.NoUser, viewModel.featureFlagState.isFileSharingState)
        viewModel.dismissSelfDeletingMessagesDialog()
        viewModel.dismissE2EICertificateRevokedDialog()
        arrangement.admission.publish(TestUser.USER_ID, arrangement.session)
        runCurrent()
        assertEquals(14, started.size)
        coVerify(exactly = 0) { arrangement.markSelfDeletingStatusAsNotified() }
        coVerify(exactly = 0) { arrangement.markNotifyForRevokedCertificateAsNotified() }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `pending valid admission retains enforced app lock feature state`() = runTest {
        val (arrangement, viewModel) = Arrangement().withIsAppLockSetup(false)
            .withTeamAppLockEnforce(AppLockTeamConfig(true, Duration.ZERO, false)).arrange()
        runCurrent()
        assertTrue(viewModel.featureFlagState.isTeamAppLockEnabled)
        assertTrue(viewModel.featureFlagState.shouldShowTeamAppLockDialog)
        arrangement.admission.publish(TestUser.USER_ID, null)
        runCurrent()
        assertTrue(viewModel.featureFlagState.isTeamAppLockEnabled)
        assertTrue(viewModel.featureFlagState.shouldShowTeamAppLockDialog)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `acknowledgement waiting for publication is abandoned when raw current becomes invalid`() = runTest {
        val current = MutableStateFlow<CurrentSessionResult>(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))
        val arrangement = Arrangement().withCurrentSessionsFlow(current)
        arrangement.admission.publish(TestUser.USER_ID, null)
        val (_, viewModel) = arrangement.arrange()
        viewModel.dismissGuestRoomLinkDialog()
        runCurrent()
        coVerify(exactly = 0) { arrangement.markGuestLinkFeatureFlagAsNotChanged() }
        current.value = CurrentSessionResult.Success(AccountInfo.Invalid(TestUser.USER_ID, LogoutReason.SELF_SOFT_LOGOUT))
        runCurrent()
        arrangement.admission.publish(TestUser.USER_ID, arrangement.session)
        runCurrent()
        coVerify(exactly = 0) { arrangement.markGuestLinkFeatureFlagAsNotChanged() }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `revoked acknowledgement drains and is not replayed by an equal account revision`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val drained = CompletableDeferred<Unit>()
        val (arrangement, viewModel) = Arrangement().arrange()
        coEvery { arrangement.markSelfDeletingStatusAsNotified() } coAnswers {
            entered.complete(Unit)
            try { awaitCancellation() } finally { drained.complete(Unit) }
        }
        viewModel.dismissSelfDeletingMessagesDialog()
        entered.await()
        arrangement.admission.publish(TestUser.USER_ID, null)
        runCurrent()
        assertTrue(drained.isCompleted)
        arrangement.admission.publish(TestUser.USER_ID, arrangement.session)
        runCurrent()
        coVerify(exactly = 1) { arrangement.markSelfDeletingStatusAsNotified() }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `cancelling the VM drains its initial and post live collectors`() = runTest {
        val started = mutableListOf<String>()
        val drained = mutableListOf<String>()
        val (arrangement, viewModel) = Arrangement().withTrackedCollectors(started, drained).arrange()
        runCurrent()
        assertEquals(7, started.size)
        viewModel.viewModelScope.cancel()
        runCurrent()
        assertEquals(started.toSet(), drained.toSet())
        arrangement.admission.publish(TestUser.USER_ID, arrangement.session)
        runCurrent()
        assertEquals(7, started.size)
    }

    @Test
    fun `genuine file observer failure escapes its admitted parent`() {
        val failure = IllegalStateException("file sharing failed")
        val observed = assertThrows(IllegalStateException::class.java) {
            runTest {
                val (_, viewModel) = Arrangement().withFileSharingStatus(flow { throw failure }).arrange()
                runCurrent()
                viewModel.viewModelScope.cancel()
            }
        }
        assertEquals(failure.message, observed.message)
    }

    private fun tracked(started: MutableList<String>, drained: MutableList<String>, name: String): Flow<Nothing> = flow {
        started += name
        try { awaitCancellation() } finally { drained += name }
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

        val session = mockk<UserSessionScope>()
        val admission by lazy { FakeSessionAdmission(coreLogic) { session } }
        var rawSessions: Flow<CurrentSessionResult> = flowOf(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))

        val viewModel: FeatureFlagNotificationViewModel by lazy {
            FeatureFlagNotificationViewModel(
                coreLogic = lazyOf(coreLogic),
                currentSessionFlow = lazyOf(currentSessionFlow),
                globalDataStore = lazyOf(globalDataStore),
                disableAppLockUseCase = lazyOf(disableAppLockUseCase),
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            admission
            every { currentSessionFlow() } answers { rawSessions }
            coEvery { coreLogic.getGlobalScope().observeAllValidSessionsFlow() } answers {
                rawSessions.map { result ->
                    GetAllSessionsResult.Success(
                        listOfNotNull((result as? CurrentSessionResult.Success)?.accountInfo?.takeIf { it.isValid() })
                    )
                }
            }
            coEvery { session.observeSyncState() } returns flowOf(SyncState.Live)
            coEvery { session.observeTeamSettingsSelfDeletionStatus() } returns flowOf()
            every {
                session.markGuestLinkFeatureFlagAsNotChanged
            } returns markGuestLinkFeatureFlagAsNotChanged
            every { session.markSelfDeletingMessagesAsNotified } returns markSelfDeletingStatusAsNotified
            every { session.markE2EIRequiredAsNotified } returns markE2EIRequiredAsNotified
            coEvery { session.appLockTeamFeatureConfigObserver } returns ppLockTeamFeatureConfigObserver
            coEvery { session.observeFileSharingStatus.invoke() } returns flowOf()
            coEvery { session.observeGuestRoomLinkFeatureFlag.invoke() } returns flowOf()
            coEvery { session.observeE2EIRequired.invoke() } returns flowOf()
            coEvery { session.calls.observeEndCallDueToDegradationDialog() } returns flowOf()
            coEvery { session.calls.observeAskCallFeedbackUseCase() } returns flowOf()
            coEvery { session.observeShouldNotifyForRevokedCertificate() } returns flowOf()
            coEvery { session.calls.updateNextTimeCallFeedback(any()) } returns Unit
            every { session.markNotifyForRevokedCertificateAsNotified } returns
                    markNotifyForRevokedCertificateAsNotified
            coEvery { ppLockTeamFeatureConfigObserver() } returns flowOf(null)
            withIsAppLockSetup(true)
        }

        fun withCurrentSessionsFlow(result: Flow<CurrentSessionResult>) = apply {
            rawSessions = result
        }

        fun withIsAppLockSetup(result: Boolean) = apply {
            coEvery { globalDataStore.isAppLockPasscodeSetFlow() } returns flowOf(result)
        }

        fun withAppLockSource(source: AppLockSource) = apply {
            coEvery { globalDataStore.getAppLockSource() } returns source
        }

        fun withDisableAppLockUseCase() = apply {
            coEvery { disableAppLockUseCase() } returns true
        }

        fun withFileSharingStatus(stateFlow: Flow<FileSharingStatus>) = apply {
            coEvery { session.observeFileSharingStatus() } returns stateFlow
        }

        fun withGuestRoomLinkFeatureFlag(stateFlow: Flow<GuestRoomLinkStatus>) = apply {
            coEvery { session.observeGuestRoomLinkFeatureFlag() } returns stateFlow
        }

        fun withE2EIRequiredSettings(result: E2EIRequiredResult) = apply {
            coEvery { session.observeE2EIRequired() } returns flowOf(result)
        }

        fun withEndCallDialog() = apply {
            coEvery { session.calls.observeEndCallDueToDegradationDialog() } returns flowOf(Unit)
        }

        fun withTeamAppLockEnforce(result: AppLockTeamConfig?) = apply {
            coEvery { ppLockTeamFeatureConfigObserver() } returns flowOf(result)
        }

        fun withTrackedCollectors(started: MutableList<String>, drained: MutableList<String>) = apply {
            coEvery { session.observeFileSharingStatus() } returns tracked(started, drained, "file")
            coEvery { session.observeTeamSettingsSelfDeletionStatus() } returns tracked(started, drained, "deletion")
            coEvery { session.observeGuestRoomLinkFeatureFlag() } returns tracked(started, drained, "guest")
            coEvery { session.observeE2EIRequired() } returns tracked(started, drained, "e2ei")
            coEvery { ppLockTeamFeatureConfigObserver() } returns tracked(started, drained, "lock")
            coEvery { session.calls.observeEndCallDueToDegradationDialog() } returns tracked(started, drained, "call")
            coEvery { session.observeShouldNotifyForRevokedCertificate() } returns tracked(started, drained, "revoked")
        }

        fun arrange() = this to viewModel
    }
}
