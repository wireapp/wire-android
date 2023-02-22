package com.wire.android.ui.home.sync

import com.wire.kalium.logic.feature.user.ObserveFileSharingStatusUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.MarkGuestLinkFeatureFlagAsNotChangedUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.amshove.kluent.internal.assertEquals
import org.junit.Before
import org.junit.Test

class FeatureFlagNotificationViewModelTest {

    @MockK
    lateinit var observeSyncState: ObserveSyncStateUseCase

    @MockK
    lateinit var observeFileSharingStatus: ObserveFileSharingStatusUseCase

    @MockK
    lateinit var observeGuestRoomLinkFeatureFlag: ObserveGuestRoomLinkFeatureFlagUseCase

    @MockK
    lateinit var markGuestLinkFeatureFlagAsNotChanged: MarkGuestLinkFeatureFlagAsNotChangedUseCase

    lateinit var featureFlagNotificationViewModel: FeatureFlagNotificationViewModel

    @Before
    fun setup() {
        featureFlagNotificationViewModel = FeatureFlagNotificationViewModel(
            observeSyncState = observeSyncState,
            observeFileSharingStatus = observeFileSharingStatus,
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
