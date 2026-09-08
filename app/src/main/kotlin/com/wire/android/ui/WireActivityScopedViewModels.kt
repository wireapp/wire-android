/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import com.wire.android.di.metro.wireMetroViewModel
import com.wire.android.ui.common.commonTopAppBarViewModel
import com.wire.android.ui.common.topappbar.CommonTopAppBarParams
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateViewModel
import com.wire.android.ui.joinConversation.JoinConversationViaCodeViewModel
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedViewModel
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModel

/**
 * Session-owned activity chrome dependencies.
 *
 * The current Metro graph and Navigation 3 owner are supplied by composition. Keeping the
 * ownership policy here removes graph-derived ViewModel keys and concrete factory calls from
 * [WireActivity], while the explicit owner gives all chrome dependencies one session lifetime.
 */
internal data class WireActivityScopedViewModels(
    val callFeedbackViewModel: CallFeedbackViewModel,
    val featureFlagNotificationViewModel: FeatureFlagNotificationViewModel,
    val commonTopAppBarViewModel: CommonTopAppBarViewModel,
    val legalHoldRequestedViewModel: LegalHoldRequestedViewModel,
    val legalHoldDeactivatedViewModel: LegalHoldDeactivatedViewModel,
    val getE2EICertificateViewModel: GetE2EICertificateViewModel,
    val joinConversationViaCodeViewModel: JoinConversationViaCodeViewModel,
)

@Composable
internal fun wireActivityScopedViewModels(
    owner: ViewModelStoreOwner,
): WireActivityScopedViewModels =
    WireActivityScopedViewModels(
        callFeedbackViewModel = wireMetroViewModel(owner = owner),
        featureFlagNotificationViewModel = wireMetroViewModel(owner = owner),
        commonTopAppBarViewModel = commonTopAppBarViewModel(
            CommonTopAppBarParams(
                showNoNetwork = true,
                showSync = true,
                showActiveCalls = true,
            ),
            owner = owner,
        ),
        legalHoldRequestedViewModel = wireMetroViewModel(owner = owner),
        legalHoldDeactivatedViewModel = wireMetroViewModel(owner = owner),
        getE2EICertificateViewModel = wireMetroViewModel(owner = owner),
        joinConversationViaCodeViewModel = wireMetroViewModel(owner = owner),
    )
