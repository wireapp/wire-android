/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home

data class FeatureFlagState(
    val showFileSharingDialog: Boolean = false,
    val isFileSharingEnabledState: Boolean = true,
    val fileSharingRestrictedState: SharingRestrictedState = SharingRestrictedState.NONE,
    val shouldShowGuestRoomLinkDialog: Boolean = false,
    val isGuestRoomLinkEnabled: Boolean = true
) {

    fun isSharingAllowed() = fileSharingRestrictedState == SharingRestrictedState.NONE
    enum class SharingRestrictedState {
        NONE, NO_USER, RESTRICTED_IN_TEAM
    }
}
