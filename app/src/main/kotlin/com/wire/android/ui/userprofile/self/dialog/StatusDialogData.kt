package com.wire.android.ui.userprofile.self.dialog

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

sealed class StatusDialogData(
    @StringRes val title: Int,
    @StringRes val text: Int,
    val status: UserAvailabilityStatus,
) {

    abstract val isCheckBoxChecked: Boolean
    abstract fun changeCheckBoxState(isChecked: Boolean): StatusDialogData

    data class StateAvailable(override val isCheckBoxChecked: Boolean = false) : StatusDialogData(
        title = R.string.user_profile_change_status_dialog_available_title,
        text = R.string.user_profile_change_status_dialog_available_text,
        status = UserAvailabilityStatus.AVAILABLE
    ) {
        override fun changeCheckBoxState(isChecked: Boolean): StatusDialogData = copy(isCheckBoxChecked = isChecked)
    }

    data class StateBusy(override val isCheckBoxChecked: Boolean = false) : StatusDialogData(
        title = R.string.user_profile_change_status_dialog_busy_title,
        text = R.string.user_profile_change_status_dialog_busy_text,
        status = UserAvailabilityStatus.BUSY
    ) {
        override fun changeCheckBoxState(isChecked: Boolean): StatusDialogData = copy(isCheckBoxChecked = isChecked)
    }

    data class StateAway(override val isCheckBoxChecked: Boolean = false) : StatusDialogData(
        title = R.string.user_profile_change_status_dialog_away_title,
        text = R.string.user_profile_change_status_dialog_away_text,
        status = UserAvailabilityStatus.AWAY
    ) {
        override fun changeCheckBoxState(isChecked: Boolean): StatusDialogData = copy(isCheckBoxChecked = isChecked)
    }

    data class StateNone(override val isCheckBoxChecked: Boolean = false) : StatusDialogData(
        title = R.string.user_profile_change_status_dialog_none_title,
        text = R.string.user_profile_change_status_dialog_none_text,
        status = UserAvailabilityStatus.NONE
    ) {
        override fun changeCheckBoxState(isChecked: Boolean): StatusDialogData = copy(isCheckBoxChecked = isChecked)
    }
}
