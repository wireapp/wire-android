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

package com.wire.android.ui.userprofile.self

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.legalhold.banner.LegalHoldUIState
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel.ErrorCodes
import com.wire.android.ui.userprofile.self.dialog.StatusDialogData
import com.wire.android.ui.userprofile.self.model.OtherAccount
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId

data class SelfUserProfileState(
    val userId: UserId,
    val avatarAsset: UserAvatarAsset? = null,
    val errorMessageCode: ErrorCodes? = null,
    val status: UserAvailabilityStatus = UserAvailabilityStatus.NONE,
    val fullName: String = "",
    val userName: String = "",
    val teamName: String? = "", // maybe teamId is better here
    val teamUrl: String? = null,
    val otherAccounts: List<OtherAccount> = emptyList(),
    val statusDialogData: StatusDialogData? = null, // null means no dialog to display
    val isAvatarLoading: Boolean = false,
    val maxAccountsReached: Boolean = false, // todo. cleanup unused code
    val isReadOnlyAccount: Boolean = true,
    val isAbleToMigrateToTeamAccount: Boolean = false,
    val isLoggingOut: Boolean = false,
    val legalHoldStatus: LegalHoldUIState = LegalHoldUIState.None,
    val accentId: Int = -1,
    val showQrCode: Boolean = false,
)
