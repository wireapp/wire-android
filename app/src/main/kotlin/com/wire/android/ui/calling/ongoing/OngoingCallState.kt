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
package com.wire.android.ui.calling.ongoing

import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.kalium.logic.data.call.CallQualityData
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

data class OngoingCallState(
    val flowState: FlowState = FlowState.Default,
    val participants: PersistentList<UICallParticipant> = persistentListOf(),
    val selectedParticipant: SelectedParticipant? = null,
    val callQualityData: CallQualityData = CallQualityData(),
    val shouldShowDoubleTapToast: Boolean = false,
    val othersVideosDisabled: Boolean = false,
) {
    sealed interface FlowState {
        object Default : FlowState
        object CallClosed : FlowState
    }
}
