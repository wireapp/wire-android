package com.wire.android.ui.userprofile

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel.ErrorCodes

data class SelfUserProfileState(
    val avatarAssetByteArray: ByteArray? = null,
    val errorMessageCode: ErrorCodes? = null,
    val status: UserStatus = UserStatus.AVAILABLE,
    val fullName: String = "",
    val userName: String = "",
    val teamName: String? = "", // maybe teamId is better here
    val otherAccounts: List<OtherAccount> = emptyList(),
    val statusDialogData: StatusDialogData? = null, // null means no dialog to display
    val isAvatarLoading: Boolean = false
)

data class OtherAccount(val id: String, val avatarUrl: String, val fullName: String, val teamName: String? = null)

sealed class StatusDialogData(
    @StringRes val title: Int,
    @StringRes val text: Int,
    val status: UserStatus,
) {

    abstract val isCheckBoxChecked: Boolean
    abstract fun changeCheckBoxState(isChecked: Boolean): StatusDialogData

    data class StateAvailable(override val isCheckBoxChecked: Boolean = false) : StatusDialogData(
        title = R.string.user_profile_change_status_dialog_available_title,
        text = R.string.user_profile_change_status_dialog_available_text,
        status = UserStatus.AVAILABLE
    ) {
        override fun changeCheckBoxState(isChecked: Boolean): StatusDialogData = copy(isCheckBoxChecked = isChecked)
    }

    data class StateBusy(override val isCheckBoxChecked: Boolean = false) : StatusDialogData(
        title = R.string.user_profile_change_status_dialog_busy_title,
        text = R.string.user_profile_change_status_dialog_busy_text,
        status = UserStatus.BUSY
    ) {
        override fun changeCheckBoxState(isChecked: Boolean): StatusDialogData = copy(isCheckBoxChecked = isChecked)
    }

    data class StateAway(override val isCheckBoxChecked: Boolean = false) : StatusDialogData(
        title = R.string.user_profile_change_status_dialog_away_title,
        text = R.string.user_profile_change_status_dialog_away_text,
        status = UserStatus.AWAY
    ) {
        override fun changeCheckBoxState(isChecked: Boolean): StatusDialogData = copy(isCheckBoxChecked = isChecked)
    }

    data class StateNone(override val isCheckBoxChecked: Boolean = false) : StatusDialogData(
        title = R.string.user_profile_change_status_dialog_none_title,
        text = R.string.user_profile_change_status_dialog_none_text,
        status = UserStatus.NONE
    ) {
        override fun changeCheckBoxState(isChecked: Boolean): StatusDialogData = copy(isCheckBoxChecked = isChecked)
    }
}
