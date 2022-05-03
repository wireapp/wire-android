package com.wire.android.ui.userprofile.self

import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.model.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel.ErrorCodes
import com.wire.android.ui.userprofile.self.dialog.StatusDialogData
import com.wire.android.ui.userprofile.self.model.OtherAccount

data class SelfUserProfileState @OptIn(ExperimentalMaterial3Api::class) constructor(
    val avatarAsset: UserAvatarAsset? = null,
    val errorMessageCode: ErrorCodes? = null,
    val status: UserStatus = UserStatus.AVAILABLE,
    val fullName: String = "",
    val userName: String = "",
    val teamName: String? = "", // maybe teamId is better here
    val otherAccounts: List<OtherAccount> = emptyList(),
    val statusDialogData: StatusDialogData? = null, // null means no dialog to display
    val isAvatarLoading: Boolean = false
)
