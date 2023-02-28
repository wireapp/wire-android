package com.wire.android.ui.home.sync

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.MarkFileSharingChangeAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.MarkGuestLinkFeatureFlagAsNotChangedUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.amshove.kluent.internal.assertEquals
import org.junit.Before
import org.junit.Test

class FeatureFlagNotificationViewModelTest {

    @MockK
    lateinit var getSessions: GetSessionsUseCase

    @MockK
    lateinit var currentSession: CurrentSessionUseCase

    @MockK
    lateinit var coreLogic: CoreLogic

    @MockK
    lateinit var observeGuestRoomLinkFeatureFlag: ObserveGuestRoomLinkFeatureFlagUseCase

    @MockK
    lateinit var markGuestLinkFeatureFlagAsNotChanged: MarkGuestLinkFeatureFlagAsNotChangedUseCase

    @MockK
    lateinit var markFileSharingAsNotified: MarkFileSharingChangeAsNotifiedUseCase

    lateinit var featureFlagNotificationViewModel: FeatureFlagNotificationViewModel

    @Before
    fun setup() {
        featureFlagNotificationViewModel = FeatureFlagNotificationViewModel(
            coreLogic = coreLogic,
            getSessions = getSessions,
            currentSessionUseCase = currentSession,
            markFileSharingAsNotified = markFileSharingAsNotified,
            observeGuestRoomLinkFeatureFlag = observeGuestRoomLinkFeatureFlag,
            markGuestLinkFeatureFlagAsNotChanged = markGuestLinkFeatureFlagAsNotChanged
        )
    }

    @Test
    fun givenGuestDialogIsShown_whenDismissingIt_thenInvokeMarkGuestLinkFeatureFlagAsNotChanged() {

        featureFlagNotificationViewModel.dismissGuestRoomLinkDialog()

        verify(exactly = 1) { markGuestLinkFeatureFlagAsNotChanged() }
        assertEquals(
            expected = false,
            actual = featureFlagNotificationViewModel.featureFlagState.shouldShowGuestRoomLinkDialog
        )
    }
}
